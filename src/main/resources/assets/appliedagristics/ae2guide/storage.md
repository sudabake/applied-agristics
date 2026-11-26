---
navigation:
  parent: index.md
  title: Seed Storage
  position: 30
  icon: appliedagristics:seed_storage
categories:
  - agr-array
item_ids:
  - appliedagristics:seed_storage
  - appliedagristics:planter_input_port
---

# Seed Storage

<Row gap="5">
  <BlockImage id="appliedagristics:planter_input_port" scale="4" />
  <BlockImage id="appliedagristics:seed_storage" scale="4" />
</Row>

| Block | Capacity |
|---|---|
| <ItemLink id="appliedagristics:planter_input_port" /> | 16 slots, up to 64 per slot |
| <ItemLink id="appliedagristics:seed_storage" /> | Unlimited seed types, up to 256 of each type by default |

If both are installed, the controller plants from the ME Seed Storage. Biological seeds with different species each get their own limit.

## Ordinary storage

Insert seeds by hand or with automation. Taking seeds currently in use by hand pauses their crop and releases them.

## ME Seed Storage

- **Collect Seeds** takes eligible seeds from the network. Use the side settings for automatic collection and category filters.
- **Eject Seeds** asks for confirmation and turns off automatic collection. Seeds that do not fit in the network stay in the storage block.
- New seeds from the array refill its ME Seed Storage first. Carrots, potatoes, and similar crops only refill what the array needs to plant.

The server setting `storage.meSeedsPerType` controls the per-type limit. Reducing it preserves existing stock.

[See where harvests go](crop-output.md).
