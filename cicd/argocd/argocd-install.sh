#!/bin/bash
# ArgoCD Installation Script for Kubernetes

set -e

echo "===================================="
echo "Installing ArgoCD on Kubernetes"
echo "===================================="

# Create ArgoCD namespace
echo "Creating argocd namespace..."
kubectl create namespace argocd || true

# Install ArgoCD
echo "Installing ArgoCD..."
kubectl apply -n argocd -f https://raw.githubusercontent.com/argoproj/argo-cd/stable/manifests/install.yaml

# Wait for ArgoCD to be ready
echo "Waiting for ArgoCD to be ready..."
kubectl wait --for=condition=available --timeout=600s deployment/argocd-server -n argocd

# Get initial admin password
echo "Getting initial admin password..."
ARGOCD_PASSWORD=$(kubectl -n argocd get secret argocd-initial-admin-secret -o jsonpath="{.data.password}" | base64 -d)

echo ""
echo "===================================="
echo "ArgoCD Installation Complete!"
echo "===================================="
echo ""
echo "Access ArgoCD:"
echo "1. Port-forward the ArgoCD server:"
echo "   kubectl port-forward svc/argocd-server -n argocd 8080:443"
echo ""
echo "2. Login credentials:"
echo "   Username: admin"
echo "   Password: ${ARGOCD_PASSWORD}"
echo ""
echo "3. Access UI at: https://localhost:8080"
echo ""
echo "4. Login via CLI:"
echo "   argocd login localhost:8080 --username admin --password ${ARGOCD_PASSWORD} --insecure"
echo ""
echo "===================================="
echo "Next Steps:"
echo "1. Change the admin password:"
echo "   argocd account update-password"
echo ""
echo "2. Add your Git repository:"
echo "   argocd repo add https://github.com/your-org/standard-microservices.git --username <user> --password <token>"
echo ""
echo "3. Deploy applications:"
echo "   kubectl apply -f cicd/argocd/application-development.yaml"
echo "   kubectl apply -f cicd/argocd/application-production.yaml"
echo ""
echo "===================================="
