# ECS Integration tests

In-progress, proof-of-concept integration tests for the ECS provider.

## What they do

This dir contains a test class (`EcsSpec`) which starts `clouddriver` on a random port and makes basic requests to various endpoints we expect responses from.

The `integrationTest` task can be run manually with gradle, or automatically on pushes to a test branch of this fork.
See [.github/workflows/ecs_integ.yml](../../../.github/workflows/ecs_integ.yml) in the project root.

To run manually run the integ test task:
```bash
$> ./gradlew :clouddriver-ecs:integrationTest
```

### Test cases
See [EcsSpec.java](java/com/netflix/spinnaker/clouddriver/ecs/EcsSpec.java) for annotated test cases.

## What they do NOT do (yet)

1. **Mock AWS service responses**
`clouddriver` still expects AWS service creds to be available via the default credential chain. In the `ecs_integ` Github workflow, these are assumed to be available as Github secrets and are configured with the [aws-actions/configure-aws-credentials](https://github.com/aws-actions/configure-aws-credentials) action.

2. **Seed test SQL instance with data**
Temp data in the cache will let us to validate `get` request responses for ECS resources.

3. **Meaningfully test atomic operations**
Setup, and cred retrieval are exercised, but more work needs to be done to meaningfully validate _CreateServerGroupAtomicOperations_ and other operations (right now we can just form a request and hit `/ops`)
