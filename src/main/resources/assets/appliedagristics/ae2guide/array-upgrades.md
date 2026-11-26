---
navigation:
  parent: index.md
  title: Array Upgrades
  position: 50
  icon: appliedagristics:planter_speed_module
categories:
  - agr-array
item_ids:
  - appliedagristics:planter_speed_module
  - appliedagristics:planter_parallel_module
  - appliedagristics:planter_conversion_module
---

# Array Upgrades

<Row gap="5">
  <BlockImage id="appliedagristics:planter_speed_module" scale="4" />
  <BlockImage id="appliedagristics:planter_parallel_module" scale="4" />
  <BlockImage id="appliedagristics:planter_conversion_module" scale="4" />
</Row>

| Part | Effect |
|---|---|
| <ItemLink id="appliedagristics:planter_speed_module" /> | Shortens the time between harvests. Up to four modules. |
| <ItemLink id="appliedagristics:planter_parallel_module" /> | Uses more seeds of the same crop at once. Up to four modules. |
| <ItemLink id="appliedagristics:planter_conversion_module" /> | Converts surplus new seeds from Mystical Agriculture and biological crops to their matching essence after refilling active planting. |

## Default upgrade values

| Number of modules | Speed | Seeds per crop |
|---:|---:|---:|
| 0 | 1× | 1 |
| 1 | 2× | 4 |
| 2 | 7.10× | 16 |
| 3 | 36.66× | 64 |
| 4 | 256× | 256 |

The array uses only the seeds it actually has. Power and yield also depend on the crop and output space. Without Crop Storage, each batch is limited to 64 harvest rounds.

With the default settings, one crop with four speed modules, four parallel modules, and 256 seeds has a power target of about 5,000 AE/t before batch and output limits.

## Upgraded example

<GameScene zoom="2" interactive={true} background="transparent">
  <ImportStructure src="structure/array.snbt" />
  <IsometricCamera yaw="195" pitch="35" />
</GameScene>

These upgrades can be placed in any legal shell position. See [building the minimum array](array.md) for the parts needed to get started.
