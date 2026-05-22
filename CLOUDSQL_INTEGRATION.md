# Cloud SQL Integration - Deployment Guide

## Problem Resolved

The original deployment had 3 services showing Cloud SQL proxy errors because:

1. **Missing Cloud SQL Instance**: No actual Cloud SQL PostgreSQL instance was created
2. **Incomplete Workload Identity Setup**: Service accounts were referenced but not properly configured
3. **Outdated Cloud SQL Proxy**: Using old proxy image and configuration
4. **Missing Database Credentials**: No proper secret management for database credentials
5. **Incomplete Environment Variables**: Missing database connection parameters in Cloud Build

## Changes Made

### 1. Created Cloud SQL Setup Script (`setup-cloudsql.sh`)

This script automatically:
- Creates Cloud SQL PostgreSQL instance
- Creates database and user with secure credentials
- Sets up Workload Identity for secure authentication
- Creates Kubernetes service accounts and secrets
- Configures proper IAM bindings

### 2. Updated Kubernetes Deployment Template (`k8s/k8s-deploy-template.yaml`)

**Fraud Service & Payment Service:**
- Updated to latest Cloud SQL Proxy image (2.8.0)
- Added proper resource limits and security context
- Changed database credentials to use Kubernetes secrets
- Improved proxy configuration with structured logging

**Order Service:**
- Added resource limits and health checks
- No database connection needed (gateway service only)

### 3. Enhanced Cloud Build Pipeline (`cloudbuild.yaml`)

- Added Cloud SQL setup step in the pipeline
- Added missing database-related substitution variables
- Proper environment variable handling for connection names
- Integrated Workload Identity setup

### 4. Updated Documentation (`README.md`)

- Comprehensive Cloud SQL integration guide
- Troubleshooting section for common proxy errors
- Security best practices
- Step-by-step deployment instructions

## Deployment Instructions

### Option 1: Automated Deployment (Recommended)

```bash
# 1. Run initial setup
./setup-cloudsql.sh [PROJECT_ID]

# 2. Deploy everything (includes Cloud SQL setup)
gcloud builds submit --config cloudbuild.yaml .
```

### Option 2: Manual Cloud SQL Setup

```bash
# 1. Run initial setup
./setup-cloudsql.sh [PROJECT_ID]

# 2. Get GKE credentials
gcloud container clusters get-credentials cb-cluster --zone us-central1-a

# 3. Setup Cloud SQL manually
chmod +x setup-cloudsql.sh
./setup-cloudsql.sh [PROJECT_ID]

# 4. Deploy with proper substitutions
gcloud builds submit --config cloudbuild.yaml \
  --substitutions=_DB_CONNECTION_NAME=PROJECT_ID:us-central1:microservices-db,_DB_NAME=microservices,_DB_USER=app-user,_DB_PASS=your-secure-password .
```

## Verification

After deployment, verify the Cloud SQL integration:

```bash
# Check pods are running
kubectl get pods

# Check Cloud SQL proxy logs
kubectl logs -l app=fraud-service -c cloud-sql-proxy
kubectl logs -l app=payment-service -c cloud-sql-proxy

# Verify database connectivity
kubectl exec -it deployment/fraud-service -c fraud-service -- env | grep SPRING_DATASOURCE

# Check Workload Identity
kubectl describe serviceaccount cloudsql-sa
```

## Key Security Improvements

1. **Workload Identity**: No service account keys stored in containers
2. **Kubernetes Secrets**: Database credentials managed securely
3. **Latest Proxy**: Updated to Cloud SQL Proxy 2.8.0 with security improvements
4. **Resource Limits**: Proper CPU and memory constraints
5. **Non-root Containers**: Enhanced security context

## Troubleshooting

If you still see Cloud SQL proxy errors:

1. **Check Workload Identity binding**:
   ```bash
   gcloud iam service-accounts get-iam-policy cloudsql-proxy-sa@PROJECT_ID.iam.gserviceaccount.com
   ```

2. **Verify Cloud SQL instance exists**:
   ```bash
   gcloud sql instances list
   ```

3. **Check service account permissions**:
   ```bash
   gcloud projects get-iam-policy PROJECT_ID --flatten="bindings[].members" --filter="bindings.role:roles/cloudsql.client"
   ```

The deployment now provides a complete, secure, and production-ready Cloud SQL integration for the microservices architecture.