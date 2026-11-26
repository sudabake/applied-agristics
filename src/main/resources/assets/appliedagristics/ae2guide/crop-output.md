---
navigation:
  parent: index.md
  title: Crop Output
  position: 40
  icon: appliedagristics:essence_vault
categories:
  - agr-array
item_ids:
  - appliedagristics:essence_vault
  - appliedagristics:planter_output_port
---

# Crop Output

<Row gap="5">
  <BlockImage id="appliedagristics:planter_output_port" scale="4" />
  <BlockImage id="appliedagristics:essence_vault" scale="4" />
</Row>

| Block | How to take the harvest |
|---|---|
| <ItemLink id="appliedagristics:planter_output_port" /> | Take it by hand, extract with pipes, or enable auto-output to the face with the port symbol. |
| <ItemLink id="appliedagristics:essence_vault" /> | Extract it from the connected ME network. |

## Routing and capacity

With both blocks present, the Crop Storage receives harvests first. Its default capacity is unlimited. Pack makers can set a total item limit with `storage.cropVaultCapacity` in the server configuration. At a finite limit, overflow goes to ME first and then the Output Port. If neither has room, the array keeps the pending harvest.

## Control production

Set the controller's **Stock Target** to the desired quantity in ME. At `-1`, production continues whenever there is output space. The **Enable new crops** switch applies the first time a crop enters the array.

The formed array provides its own planting environment. Find the possible outputs of crops, saplings, and compatible planting recipes in JEI.
