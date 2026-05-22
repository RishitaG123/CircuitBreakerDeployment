#!/bin/bash

# Setup script for microservices deployment pipeline
# This script enables required APIs and sets up IAM permissions

set -e

PROJECT_ID=${1:-$(gcloud config get-value project)}
if [ -z "$PROJECT_ID" ]; then
    echo "Error: Please provide PROJECT_ID as argument or set default project"
    echo "Usage: $0 [PROJECT_ID]"
    exit 1
fi

echo "Setting up project: $PROJECT_ID"

# Enable required APIs
echo "Enabling required APIs..."
gcloud services enable cloudbuild.googleapis.com \
    artifactregistry.googleapis.com \
    container.googleapis.com \
    sqladmin.googleapis.com \
    compute.googleapis.com \
    --project=$PROJECT_ID

# Get Cloud Build service account
CB_SA=$(gcloud projects describe $PROJECT_ID --format="value(projectNumber)")@cloudbuild.gserviceaccount.com

echo "Cloud Build service account: $CB_SA"

# Grant necessary IAM roles
echo "Granting IAM roles to Cloud Build service account..."

# For GKE cluster management
gcloud projects add-iam-policy-binding $PROJECT_ID \
    --member="serviceAccount:$CB_SA" \
    --role="roles/container.developer"

# For Cloud SQL management
gcloud projects add-iam-policy-binding $PROJECT_ID \
    --member="serviceAccount:$CB_SA" \
    --role="roles/cloudsql.admin"

# For Artifact Registry
gcloud projects add-iam-policy-binding $PROJECT_ID \
    --member="serviceAccount:$CB_SA" \
    --role="roles/artifactregistry.admin"

# For Compute Engine (needed for GKE)
gcloud projects add-iam-policy-binding $PROJECT_ID \
    --member="serviceAccount:$CB_SA" \
    --role="roles/compute.admin"

# For Service Account management (needed for GKE)
gcloud projects add-iam-policy-binding $PROJECT_ID \
    --member="serviceAccount:$CB_SA" \
    --role="roles/iam.serviceAccountUser"

echo "Setup completed successfully!"
echo ""
echo "You can now run the deployment with:"
echo "gcloud builds submit --config cloudbuild.yaml ."
echo ""
echo "Or create a trigger with:"
echo "gcloud builds triggers create github \\"
echo "  --repo-name=your-repo \\"
echo "  --repo-owner=your-username \\"
echo "  --branch-pattern='^main$' \\"
echo "  --build-config=cloudbuild.yaml"