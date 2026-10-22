# Alpha.17 validation

The review used an isolated Alpha.16 checkout to reproduce four defects before fixing production behavior. DeepReviewTests covers named-seed replant accounting, short infusion recipes, physical-slot container returns, external extraction followed by manual crafting, and transferred output components.

Local validation passed: 17 JUnit tests; 41 GameTests with default dependencies and 41 with minimum dependencies; eight client machine screens plus JEI/GuideME smoke checks; two isolated rebuilds with all 564 runtime resources and collision shapes unchanged. The allowlisted public snapshot independently passed clean build, 17 JUnit tests, 41 GameTests and resource rebuild checks. Initial reproductions and one old-world shutdown stall were retained locally; the final fresh-world runs exited normally. These checks do not establish crash-safe cross-file transactions, long-term multiplayer reliability, arbitrary mod compatibility or large-server performance.

No development logs, game saves or launcher files are included in this repository. Developer fixtures are excluded from the built JAR.
