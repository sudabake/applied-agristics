# Beta 1.0 validation scope

Local pre-release validation passed 18 JUnit tests, 65 required GameTests on both the development and minimum dependency combinations, six portable resource-export tests and two isolated resource rebuilds. Inventory and protocol regressions cover same-key seed/output handling and long component-bearing control IDs. Upper-layer ports and modules are covered by a real-array GameTest.

The release JAR is checked against current compiled classes and committed runtime resources. Retired textures, duplicate entries, missing model/texture references, orphan animation metadata, developer fixtures, local machine paths and PNG text/EXIF metadata are screened.

These checks do not establish arbitrary modpack compatibility, long-term multiplayer reliability or large-server performance. No personal test worlds, logs, screenshots or launcher files are included. Hosted CI results are reported separately by GitHub Actions.
