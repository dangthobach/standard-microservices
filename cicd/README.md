# CI/CD Pipeline Setup Guide

This guide covers the complete CI/CD setup for the Enterprise Microservices platform using Jenkins and ArgoCD.

## Architecture Overview

```
┌─────────────┐      ┌──────────────┐      ┌─────────────┐      ┌──────────────┐
│   GitHub    │─────▶│   Jenkins    │─────▶│   Docker    │─────▶│   ArgoCD     │
│             │      │              │      │   Registry  │      │              │
│  Git Push   │      │  Build & Test│      │   Push Image│      │  Deploy K8s  │
└─────────────┘      └──────────────┘      └─────────────┘      └──────────────┘
```

## Prerequisites

1. **Kubernetes Cluster** (v1.25+)
2. **kubectl** configured to access cluster
3. **Docker Registry** (Docker Hub, AWS ECR, GCR, etc.)
4. **Git Repository** with appropriate access

## Part 1: Jenkins Setup

### Step 1: Install Jenkins on Kubernetes

```bash
# Create Jenkins namespace
kubectl create namespace jenkins

# Deploy Jenkins
kubectl apply -f cicd/jenkins/jenkins-kubernetes.yaml

# Wait for Jenkins to be ready
kubectl wait --for=condition=available --timeout=600s deployment/jenkins -n jenkins

# Get initial admin password
kubectl exec -it -n jenkins deployment/jenkins -- cat /var/jenkins_home/secrets/initialAdminPassword
```

### Step 2: Access Jenkins UI

```bash
# Port-forward Jenkins service
kubectl port-forward -n jenkins svc/jenkins 8080:8080

# Access Jenkins at: http://localhost:8080/jenkins
```

### Step 3: Configure Jenkins

1. **Install Required Plugins:**
   - Docker Pipeline
   - Kubernetes
   - Git
   - Maven Integration
   - NodeJS
   - SonarQube Scanner
   - Pipeline

2. **Configure Credentials:**
   - Docker Registry credentials
   - Git repository credentials
   - Kubernetes service account token

3. **Configure Tools:**
   - Maven: Auto-install Maven 3.9.x
   - NodeJS: Auto-install Node 20.x
   - Docker: Use Docker from agent

4. **Create Multibranch Pipeline:**
   - Source: Git
   - Repository URL: Your GitHub repo
   - Script Path: `Jenkinsfile`
   - Scan triggers: Every 5 minutes

## Part 2: ArgoCD Setup

### Step 1: Install ArgoCD

```bash
# Run installation script
chmod +x cicd/argocd/argocd-install.sh
./cicd/argocd/argocd-install.sh
```

### Step 2: Access ArgoCD UI

```bash
# Port-forward ArgoCD server
kubectl port-forward svc/argocd-server -n argocd 8080:443

# Access ArgoCD at: https://localhost:8080

# Login credentials:
# Username: admin
# Password: (from installation script output)
```

### Step 3: Configure ArgoCD

```bash
# Login to ArgoCD CLI
argocd login localhost:8080 --username admin --insecure

# Change admin password
argocd account update-password

# Add Git repository
argocd repo add https://github.com/your-org/standard-microservices.git \
  --username <github-username> \
  --password <github-token>
```

### Step 4: Deploy Applications

```bash
# Deploy Development environment
kubectl apply -f cicd/argocd/application-development.yaml

# Deploy Production environment
kubectl apply -f cicd/argocd/application-production.yaml

# Check application status
argocd app list
argocd app get enterprise-production
```

## Part 3: Complete CI/CD Workflow

### Development Workflow

1. **Developer pushes code to `develop` branch**
2. **Jenkins** automatically:
   - Detects change
   - Runs tests
   - Builds Docker images
   - Pushes to registry with `dev-<commit>` tag
   - Updates K8s manifests
3. **ArgoCD** automatically:
   - Detects manifest change
   - Syncs to development cluster
   - Verifies deployment health

### Production Workflow

1. **Merge PR to `main` branch**
2. **Jenkins** automatically:
   - Runs full test suite
   - Runs SonarQube analysis
   - Builds production images
   - Security scan with Trivy
   - Pushes to registry with version tag
   - Updates production manifests
3. **ArgoCD** automatically:
   - Detects manifest change
   - Syncs to production cluster
   - Performs rolling update
   - Verifies health checks

## Monitoring CI/CD Pipeline

### Jenkins Metrics

```bash
# View Jenkins logs
kubectl logs -n jenkins deployment/jenkins -f

# Check build status
# Access Jenkins UI → Dashboard
```

### ArgoCD Metrics

```bash
# View ArgoCD application status
argocd app get enterprise-production

# Check sync status
argocd app list

# View application logs
kubectl logs -n argocd deployment/argocd-server
```

## Rollback Procedures

### ArgoCD Rollback

```bash
# View application history
argocd app history enterprise-production

# Rollback to specific revision
argocd app rollback enterprise-production <revision-number>

# Sync to Git state
argocd app sync enterprise-production --prune
```

### Manual Rollback

```bash
# Rollback Kubernetes deployment
kubectl rollout undo deployment/gateway-service -n enterprise-prod

# Check rollout status
kubectl rollout status deployment/gateway-service -n enterprise-prod
```

## Troubleshooting

### Jenkins Issues

```bash
# Check Jenkins pod status
kubectl get pods -n jenkins

# View Jenkins logs
kubectl logs -n jenkins deployment/jenkins --tail=100

# Restart Jenkins
kubectl rollout restart deployment/jenkins -n jenkins
```

### ArgoCD Issues

```bash
# Check ArgoCD status
kubectl get pods -n argocd

# Refresh application
argocd app get enterprise-production --refresh

# Hard refresh (ignore cache)
argocd app get enterprise-production --hard-refresh

# View application events
kubectl describe application enterprise-production -n argocd
```

## Security Best Practices

1. **Use Secret Management:**
   - Store credentials in Kubernetes Secrets
   - Use External Secrets Operator for production
   - Rotate secrets regularly

2. **RBAC Configuration:**
   - Limit Jenkins service account permissions
   - Configure ArgoCD RBAC policies
   - Use namespace isolation

3. **Image Security:**
   - Scan images with Trivy
   - Use image signing (Cosign)
   - Implement image admission policies

4. **Network Security:**
   - Use NetworkPolicies
   - Enable mTLS between services
   - Restrict egress traffic

## Performance Optimization

### For 1M CCU:

1. **Jenkins Agent Pool:**
   - Configure Kubernetes-based dynamic agents
   - Scale agents based on build queue

2. **ArgoCD Scaling:**
   - Increase ArgoCD server replicas
   - Configure application controller replicas
   - Use application sets for multi-cluster

3. **Build Optimization:**
   - Use Docker layer caching
   - Implement build parallelization
   - Use incremental builds

## Additional Resources

- [Jenkins Documentation](https://www.jenkins.io/doc/)
- [ArgoCD Documentation](https://argo-cd.readthedocs.io/)
- [Kubernetes Best Practices](https://kubernetes.io/docs/concepts/configuration/overview/)
