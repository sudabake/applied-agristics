---
navigation:
  parent: index.md
  title: Build the ME Agricultural Array
  position: 20
  icon: appliedagristics:network_planter
categories:
  - agr-build
item_ids:
  - appliedagristics:network_planter
  - appliedagristics:me_network_access
  - appliedagristics:planter_frame
  - appliedagristics:cultivation_chamber
---

# Build the ME Agricultural Array

The array is **3 × 3 × 4** (width × depth × height). Place the controller facing out at the front centre, **one block above the bottom layer**. [See each layer](array-layers.md) if you are building by hand.

Right-click the controller and select **Build Array**. After it forms, connect the ME Network Access to a powered network.

<GameScene zoom="2" interactive={true} background="transparent">
  <ImportStructure src="structure/array-minimum.snbt" />
  <IsometricCamera yaw="195" pitch="35" />
  <BoxAnnotation color="#5fdbc4" min="1 1 0" max="2 2 1">Controller position</BoxAnnotation>
</GameScene>

Drag to rotate the minimum build.

## Materials for the minimum build

| Part | Count |
|---|---:|
| <ItemLink id="appliedagristics:network_planter" /> | 1 |
| <ItemLink id="appliedagristics:cultivation_chamber" /> | 2 |
| <ItemLink id="appliedagristics:me_network_access" /> | 1 |
| <ItemLink id="appliedagristics:planter_input_port" /> | 1 |
| <ItemLink id="appliedagristics:planter_output_port" /> | 1 |
| <ItemLink id="appliedagristics:planter_frame" /> | 30 |

ME Seed Storage can replace Seed Storage. Crop Storage can replace the Output Port. You need exactly one ME Network Access.

## Build and start

1. Right-click the controller and select **Build Array**. In survival, this uses the materials in your inventory. In creative, it builds the [upgraded array](array-upgrades.md). You can also [place the blocks by hand](array-layers.md).
2. After the structure forms, connect the ME Network Access to a powered ME network. The array uses no channel.
3. Add one seed and enable its crop in the controller. Check your [output block](crop-output.md) for the first harvest.

## If the array will not start

| What you see | What to check |
|---|---|
| Missing parts in chat | Bring the listed items and press **Build Array** again. |
| Flashing red outline | Clear the marked position. |
| Incomplete shell or missing chamber | Check the two hidden chambers and the outer shell in the [layer views](array-layers.md). |
| Structure formed, network offline | Connect a powered ME network to the ME Network Access. |
| Online, no harvest | Add seeds, enable the crop, and make room for its output. |

Press **U** over an array part to open the JEI structure view.
