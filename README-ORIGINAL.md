# Enterprise Microservices Platform

High-performance, scalable microservices platform built with Spring Boot, Virtual Threads, and reactive architecture, designed to handle **1 million concurrent users**.

## Table of Contents

- [Overview](#overview)
- [Technology Stack](#technology-stack)
- [Architecture](#architecture)
- [Quick Start](#quick-start)
- [Project Structure](#project-structure)
- [Documentation](#documentation)
- [Development](#development)
- [Deployment](#deployment)
- [Contributing](#contributing)

## Overview

This enterprise-grade microservices platform demonstrates modern architectural patterns and cutting-edge technologies:

- **Reactive API Gateway** với Spring Cloud Gateway (WebFlux)
- **Virtual Threads** (Java 21 Project Loom) cho blocking services
- **Multi-level Caching** (L1 Caffeine + L2 Redis Cluster)
- **Event-Driven Architecture** với Kafka
- **OAuth2 PKCE Flow** với Keycloak
- **Distributed Tracing** với Zipkin
- **Kubernetes-native** deployment với HPA & auto-scaling
- **Angular 21** modern frontend

## Technology Stack

### Backend
- **Java 21** - Virtual Threads support
- **Spring Boot 3.4.1** - Latest stable release
- **Spring Cloud Gateway** - Reactive WebFlux gateway
- **PostgreSQL 16** - Primary database
- **Redis 7** - Distributed cache
- **Kafka 3.9** - Event streaming
- **Keycloak 26** - Identity & Access Management

### Infrastructure
- **Docker & Docker Compose** - Local development
- **Kubernetes** - Container orchestration
- **AWS EKS** - Managed Kubernetes
- **Prometheus & Grafana** - Monitoring & visualization
- **Zipkin** - Distributed tracing

### Frontend
- **Angular 21** - Modern web framework
- **NgRx** - State management
- **OAuth2 OIDC** - Authentication

## Architecture
---
config:
  theme: redux-dark
  look: classic
---
graph LR
    %% --- STYLES ---
    classDef external fill:#fff3cd,stroke:#d39e00,stroke-width:2px,color:#1f1f1f
    classDef gateway fill:#d6ecff,stroke:#1c7ed6,stroke-width:2px,color:#0b1f33
    classDef internal fill:#e9fce9,stroke:#2f9e44,stroke-width:2px,color:#102a12
    classDef infra fill:#f3e8ff,stroke:#7b2cbf,stroke-width:2px,color:#1a1025
    classDef data fill:#f1f3f5,stroke:#495057,stroke-width:2px,color:#111
    classDef obs fill:#212529,stroke:#fa5252,stroke-width:2px,color:#fff,stroke-dasharray: 5 5

    %% --- EXTERNAL ---
    subgraph EXT [EXTERNAL ZONE]
        Client["Client / External Actor"]
        ThirdParty["Third-Party Systems"]
    end

    %% --- GATEWAY LAYER ---
    subgraph GW_LAYER [GATEWAY & IDP LAYER]
        direction TB
        Keycloak["Keycloak IdP
        PKCE Flow"]
        Gateway["Spring Cloud Gateway
        (Start Trace ID)
        + L1 Caffeine Cache"]
    end

    %% --- CORE SYSTEM ---
    subgraph CORE_SYSTEM [INTERNAL SYSTEM ARCHITECTURE]
        direction TB
        
        %% OBSERVABILITY
        subgraph OBS_LAYER [OBSERVABILITY STACK]
            direction LR
            Zipkin[("Zipkin
            (Distributed Tracing)")]
            MonitorBackend[("Prometheus / Grafana
            (Metrics & Logs)")]
        end

        %% INFRA
        subgraph INFRA_BUS [INFRASTRUCTURE SHARED]
            direction LR
            Redis[("Redis Cluster
            (L2 Cache)")]
            Kafka[("Kafka Cluster
            (Event Bus)")]
        end

        %% SERVICES
        subgraph SVC_STACK [MICROSERVICES LAYER]
            direction LR
            Business["business-service"]
            IAM["iam-service"]
            Process["process-management"]
            Integration["integration-service"]
        end
    end

    %% --- DATA LAYER ---
    subgraph DATA_LAYER [DATA PERSISTENCE]
        direction TB
        PgBiz[("DB Business")]
        PgIAM[("DB IAM")]
        PgBPM[("DB Process")]
    end

    %% ================= CONNECTIONS =================

    %% 1. Auth Flow
    Client -->|1. Auth PKCE| Keycloak
    Client ==>|2. HTTPS + Token| Gateway

    %% 2. Authorization & Caching
    Gateway --"3. Check Authz/Cache"--> Redis
    Redis -.->|Miss| IAM
    IAM -.->|Update Cache| Redis
    IAM --"Authz OK"--> Gateway

    %% 3. Routing & Trace Propagation
    Gateway ==>|4. Route + Trace Header| Business
    Gateway ==>|4. Route + Trace Header| IAM
    Gateway ==>|4. Route + Trace Header| Process

    %% 4. Internal Logic & Kafka
    Business <==>|Async Event| Kafka
    Kafka <==>|Consume/Produce| Process
    Process <==>|Internal Call| Integration
    Integration <-->|External API| ThirdParty

    %% 5. Data Persistence
    Business --> PgBiz
    IAM --> PgIAM
    Process --> PgBPM

    %% 6. OBSERVABILITY FLOW
    Gateway & Business & IAM & Process & Integration -.->|Async Push Trace| Zipkin
    Gateway & Business & IAM & Process & Integration -.->|Async Push Metrics| MonitorBackend

    %% --- CLASS ASSIGNMENT ---
    class Client,ThirdParty external
    class Gateway,Keycloak gateway
    class Business,IAM,Process,Integration internal
    class Redis,Kafka infra
    class PgIAM,PgBPM,PgBiz data
    class Zipkin,MonitorBackend obs

- Đối với cấu trúc project xây dựng hướng modular structure, cho phép build multiple module 
- Với frontend lựa chọn Angular 21 latest version