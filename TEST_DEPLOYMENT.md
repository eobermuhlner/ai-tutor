# Test Deployment Workflows

The CI/CD pipeline includes automated deployment workflows that have been implemented as described in the requirements:

## Files Created

1. `.github/workflows/deploy-test.yml` - Deploys to test environment on merge to master
2. `.github/workflows/deploy-prod.yml` - Deploys to production on release tags
3. `DEPLOYMENT_CONFIG.md` - Documentation for deployment configuration
4. `deployment/deploy.sh` - Manual deployment script
5. `deployment/README.md` - Documentation for deployment directory
6. Updated `CI.md` - Documentation for deployment workflows

## Testing Process

To verify these workflows work properly, you would need to:

1. **Set up GitHub repository** with the proper secrets and environments
2. **Merge changes to master branch** to trigger test deployment
3. **Create a GitHub release** to trigger production deployment

## Verification Steps

1. Check that the GitHub Actions workflows are properly formatted
2. Verify that secrets are properly referenced in the workflows
3. Confirm that SSH deployment steps are correctly implemented
4. Ensure environment-specific configurations are correctly set

These deployment workflows automatically deploy the application to:
- Test environment (ai-tutor-test.obermuhlner.ch) when code is merged to master
- Production environment (ai-tutor.obermuhlner.ch) when a release is created

The workflows handle:
- Building and pushing Docker images to GitHub Container Registry
- Pulling and running the containers on the target server
- Health checking to ensure successful deployment
- Proper environment-specific configurations