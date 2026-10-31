# Contributor guidance

- Minecraft 1.21.1, NeoForge 21.1.248, Java 21. Preserve server authority, inventories and saved-data migration.
- The array is 3x3x4. All shell layers are modular; only the controller and two internal chambers have fixed positions. Speed and parallel limits are four each.
- Current original 16px art is selected by tools/install_native_block_art.py. Connected shells use array_connected_16_v02. No array-wide moving light band.
- Preserve license notices. Check public AE2/addon APIs before duplicating mechanisms.
- Run meaningful regressions and the isolated resource rebuild after affected changes. Developer fixtures must remain excluded from release JARs.
- Never commit credentials, private paths, logs, saves, launcher settings or local backups. Publication requires maintainer authorization.

- Beta1.1 storage: all array harvests can enter Crop Storage (legacy essence_vault ID), extraction-only through AE. New seeds prefer the same array ME seed storage; ordinary seed storage does not auto-refill.
- AE2 guide integration uses the shared AE2 guide; ordinary seed storage has no IO page. Preserve packet numbers and old inventory migration.

- Beta1.1 storage: all array harvests can enter Crop Storage (legacy essence_vault ID), extraction-only through AE. New seeds prefer the same array ME seed storage; ordinary seed storage does not auto-refill.
- AE2 guide integration uses the shared AE2 guide; ordinary seed storage has no IO page. Preserve packet numbers and old inventory migration.
