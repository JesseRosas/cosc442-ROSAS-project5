# JoshHack Guidance

- Environment: Java 17 with the Maven Wrapper (`./mvnw`).
- Before considering work complete, run:
  - `./mvnw test`
  - `./mvnw test-compile org.pitest:pitest-maven:mutationCoverage`
  - `./mvnw spotbugs:spotbugs`
- Preserve existing game behavior unless a behavior change is explicitly requested.
- Do not weaken, remove, skip, or disable tests to make the build pass.
- Clearly identify all production-code changes for human review; keep test-only changes separate when practical.
- Do not push or merge changes automatically. Leave version-control actions to a human.
