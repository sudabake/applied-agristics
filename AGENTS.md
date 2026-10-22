# Development conventions

- Minecraft 1.21.1, NeoForge 21.1.248, Java 21. Keep dependency versions explicit.
- Preserve item counts, seed component identity and server authority. Only seed storage and essence vault blocks retain inventory data when dismantled.
- Array dimensions are 3x3x4, with fixed upper layers and at most four speed/four parallel modules in the lower layers.
- Approved art export is owned by tools/install_alpha14_art.py. Run tools/check_resource_rebuild.py after changing generation; do not overwrite machine crafting recipes with placeholders.
- Add meaningful regressions for inventory, storage, recipe and networking changes. Development tests are excluded from the built JAR.
- Check AE2 and relevant addon APIs before duplicating their mechanisms. Preserve upstream license notices.
- Never commit credentials, private paths, logs, saves, launcher settings or local backups. No release or publication without maintainer authorization.
