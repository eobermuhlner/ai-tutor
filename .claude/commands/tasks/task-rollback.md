---
description: Generate rollback instructions for a task
argument-hint: task_file=tasks/task-XXXX-feature-name.md
---

Generate rollback plan for task: $1

## Rollback Scenarios

### Scenario 1: Pre-Production (before deployment)
**When**: Discovered issue during testing
**Action**: Git revert
```bash
git revert <commit-hash>
git push
```

### Scenario 2: Post-Deployment (feature flag enabled)
**When**: Issue discovered in production, feature flag exists
**Action**: Disable feature flag
```yaml
# application.yml
feature-name:
  enabled: false  # Change from true
```
**Recovery time**: Immediate (no redeploy needed)

### Scenario 3: Post-Deployment (no feature flag)
**When**: Issue discovered in production, no feature flag
**Action**: Emergency rollback

1. **Immediate**: Redeploy previous version
   ```bash
   git checkout <previous-release-tag>
   ./gradlew build
   # Deploy...
   ```

2. **Database rollback** (if migration ran):
   ```sql
   -- Rollback script (AUTO-GENERATED from task spec)
   [SQL statements to reverse migration]
   ```

3. **Verification**:
   ```bash
   # Check health endpoint
   curl http://localhost:8080/actuator/health

   # Check feature works
   [Specific test commands]
   ```

## Rollback Checklist

- [ ] Identify which changes need reverting
- [ ] Database rollback script prepared
- [ ] Config rollback documented
- [ ] Verification tests identified
- [ ] Communication plan (who to notify)
- [ ] Monitoring alerts configured

## Prevention

To avoid needing rollback:
1. Feature flags for all new features
2. Gradual rollout (5% → 25% → 50% → 100%)
3. Automated integration tests in CI/CD
4. Staging environment testing
5. Monitoring alerts for error rates

Generate specific rollback SQL and verification commands based on the task specification.