# Microservices Deployment Pipeline with Cloud SQL Integration

This repository contains a complete CI/CD pipeline for deploying microservices to Google Kubernetes Engine (GKE) with Cloud SQL PostgreSQL integration using Google Cloud Build and Workload Identity.

## Architecture

The system consists of three Spring Boot microservices:

1. **Order Service** (Port 8080) - Main orchestration service with LoadBalancer
2. **Fraud Service** (Port 8081) - Fraud detection service with PostgreSQL via Cloud SQL Proxy
3. **Payment Service** (Port 8082) - Payment processing service with PostgreSQL via Cloud SQL Proxy

## Features

- **Docker Images**: Multi-stage builds for all microservices
- **Artifact Registry**: Images pushed to Google Artifact Registry
- **GKE Deployment**: Automated cluster creation and deployment
- **Cloud SQL Integration**: PostgreSQL database with Cloud SQL Proxy and Workload Identity
- **Service Mesh**: Internal service communication via Kubernetes services
- **Load Balancing**: External access via LoadBalancer for order-service
- **Security**: Workload Identity for secure database access without service account keys

## Prerequisites

1. Google Cloud Project with billing enabled
2. Required APIs enabled (automatically enabled by setup script):
   - Cloud Build API
   - Artifact Registry API
   - Kubernetes Engine API
   - Cloud SQL Admin API
   - Compute Engine API
3. Appropriate IAM permissions for Cloud Build service account

## Quick Start

### 1. Initial Setup

Run the setup script to enable APIs and configure IAM permissions:

```bash
./setup-cloudsql.sh [PROJECT_ID]
```

### 2. Deploy with Cloud SQL Integration

The deployment will automatically create Cloud SQL instance, databases, and configure Workload Identity:

```bash
gcloud builds submit --config cloudbuild.yaml .
```

### 3. Custom Configuration

You can override default values using substitutions:

```bash
gcloud builds submit --config cloudbuild.yaml \
  --substitutions=_DB_NAME=myapp,_DB_USER=myuser,_DB_PASS=securepassword .
```

## Configuration

The pipeline is configured via substitutions in `cloudbuild.yaml`:

```yaml
substitutions:
  _REGION: "us-central1"                           # Artifact Registry and GKE region
  _REPOSITORY: "microservices-repo"                # Artifact Registry repository name
  _CLUSTER_NAME: "cb-cluster"                      # GKE cluster name
  _CLUSTER_ZONE: "us-central1-a"                   # GKE cluster zone
  _DB_CONNECTION_NAME: "PROJECT_ID:us-central1:microservices-db"  # Cloud SQL connection name
  _DB_NAME: "microservices"                        # Database name
  _DB_USER: "app-user"                             # Database user
  _DB_PASS: "changeme"                             # Database password (change in production)
```

## Cloud SQL Integration Details

### Architecture

- **Cloud SQL Proxy**: Runs as a sidecar container in fraud-service and payment-service pods
- **Workload Identity**: Secure authentication without service account keys
- **Connection**: Local connection via proxy (127.0.0.1:5432)
- **Credentials**: Stored in Kubernetes secrets

### Automatic Setup

The `setup-cloudsql.sh` script automatically:

1. Creates Cloud SQL PostgreSQL instance
2. Creates database and user
3. Sets up Workload Identity
4. Creates Kubernetes service accounts and secrets
5. Configures IAM bindings

### Manual Cloud SQL Setup

If you prefer to set up Cloud SQL manually:

```bash
# Make the script executable
chmod +x setup-cloudsql.sh

# Run with custom parameters
./setup-cloudsql.sh PROJECT_ID REGION DB_INSTANCE DB_NAME DB_USER DB_PASS CLUSTER_NAME CLUSTER_ZONE
```

## Pipeline Steps

1. **Create Artifact Registry Repository** - Creates Docker repository (idempotent)
2. **Build Maven Project** - Builds all microservices
3. **Build Docker Images** - Creates container images for all services
4. **Push to Artifact Registry** - Pushes images to Google Artifact Registry
5. **Create GKE Cluster** - Creates Kubernetes cluster with Workload Identity (idempotent)
6. **Setup Cloud SQL** - Creates database instance and configures Workload Identity
7. **Deploy to Kubernetes** - Applies manifests with Cloud SQL proxy configuration

## Service Communication

- **External**: order-service exposed via LoadBalancer on port 80
- **Internal**: Services communicate via Kubernetes DNS
  - `http://fraud-service:8081`
  - `http://payment-service:8082`
- **Database**: fraud-service and payment-service connect via Cloud SQL Proxy

## Monitoring and Troubleshooting

### Check Deployment Status

```bash
kubectl get pods
kubectl get services
kubectl get deployments
kubectl describe pod -l app=fraud-service
```

### View Logs

```bash
# Application logs
kubectl logs -l app=order-service
kubectl logs -l app=fraud-service -c fraud-service
kubectl logs -l app=payment-service -c payment-service

# Cloud SQL Proxy logs
kubectl logs -l app=fraud-service -c cloud-sql-proxy
kubectl logs -l app=payment-service -c cloud-sql-proxy
```

### Database Connectivity

```bash
# Check database secrets
kubectl get secret cloudsql-db-credentials -o yaml

# Test database environment variables
kubectl exec -it deployment/fraud-service -c fraud-service -- env | grep SPRING_DATASOURCE

# Check Workload Identity configuration
kubectl describe serviceaccount cloudsql-sa
```

### Access Application

```bash
# Get external IP
kubectl get service order-service

# Test endpoint
curl http://EXTERNAL_IP/api/orders
```

## Security Features

1. **Workload Identity**: No service account keys stored in containers
2. **Kubernetes Secrets**: Database credentials stored securely
3. **Cloud SQL Proxy**: Encrypted connection to database
4. **Resource Limits**: CPU and memory limits on all containers
5. **Security Context**: Non-root containers with read-only filesystems

## Troubleshooting Common Issues

### Cloud SQL Proxy Errors

1. **Connection refused**: Check if Workload Identity is properly configured
   ```bash
   kubectl describe serviceaccount cloudsql-sa
   gcloud iam service-accounts get-iam-policy cloudsql-proxy-sa@PROJECT_ID.iam.gserviceaccount.com
   ```

2. **Authentication errors**: Verify service account has Cloud SQL Client role
   ```bash
   gcloud projects get-iam-policy PROJECT_ID --flatten="bindings[].members" --filter="bindings.role:roles/cloudsql.client"
   ```

3. **Instance not found**: Check Cloud SQL instance exists and connection name is correct
   ```bash
   gcloud sql instances list
   ```

### Application Startup Issues

1. **Database connection timeout**: Check Cloud SQL proxy logs
2. **Image pull errors**: Verify Artifact Registry permissions
3. **Resource constraints**: Check pod resource limits and node capacity

## Development

### Local Development

For local development, you can run services with a local PostgreSQL instance:

```bash
# Start local PostgreSQL
docker run --name postgres -e POSTGRES_PASSWORD=123 -p 5432:5432 -d postgres:15

# Run service locally
cd fraud-service
mvn spring-boot:run
```

### Testing

```bash
# Build and test
mvn clean test

# Integration testing with testcontainers
mvn test -Dspring.profiles.active=integration
```

## Cleanup

```bash
# Delete GKE cluster
gcloud container clusters delete cb-cluster --zone us-central1-a

# Delete Cloud SQL instance
gcloud sql instances delete microservices-db

# Delete Artifact Registry repository
gcloud artifacts repositories delete microservices-repo --location us-central1

# Delete service accounts
gcloud iam service-accounts delete cloudsql-proxy-sa@PROJECT_ID.iam.gserviceaccount.com
```

## Production Considerations

1. **Database Password**: Use strong passwords and consider Google Secret Manager
2. **Network Security**: Use private GKE clusters and Cloud SQL private IP
3. **High Availability**: Enable Cloud SQL high availability and increase replicas
4. **Monitoring**: Set up Cloud Monitoring and Logging
5. **Backup**: Configure automated Cloud SQL backups
6. **Resource Limits**: Adjust CPU and memory limits based on load testing