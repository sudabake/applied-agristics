# Beta1.1 validation

18 JUnit tests and 6 Python resource-export tests passed. Both default and minimum
dependency combinations passed 78 GameTests and saved/exited normally. The first
minimum-dependency run passed its assertions but stalled in vanilla chunk unload
during shutdown; its records were retained and a fresh-directory retry exited normally.
No production shutdown workaround was added.

Two isolated resource rebuilds preserved 585 resources and collision geometry.
An isolated client-world copy passed eight machine menus, controller filter alignment,
scrolling, packets, JEI biological variants/recipes, the shared AE2 guide and existing
rendering/animation checks. The client saved and exited normally. Outdated fixture
assumptions about the removed ordinary-storage IO page and fixed toolbar coordinates
were corrected before the successful client run.

The JAR was checked against resource and compiled-class outputs. No development tests,
placeholder files, retired controller IO screen, PNG text/EXIF metadata or configured
local-path patterns were found. These checks do not establish arbitrary mod compatibility
or long-term large-server performance. Saves, launcher files and local logs are excluded.
