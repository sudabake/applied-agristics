---
navigation:
  parent: array.md
  title: Build Layer by Layer
  position: 10
  icon: appliedagristics:cultivation_chamber
---

# Build Layer by Layer

Each view includes all layers placed so far. The controller's side is the front. Drag a preview to rotate it.

## 1. Bottom layer

Fill the entire 3 × 3 floor. In this example, the Output Port is at the front centre.

<GameScene zoom="2" interactive={true} background="transparent">
  <ImportStructure src="structure/array-layer-1.snbt" />
  <IsometricCamera yaw="195" pitch="35" />
</GameScene>

## 2. Controller and first chamber

Place the controller at the front centre of this layer. Put the first Cultivation Chamber directly behind it, in the centre of the footprint. Install the seed storage and network access in the shell.

<GameScene zoom="2" interactive={true} background="transparent">
  <ImportStructure src="structure/array-layer-2.snbt" />
  <IsometricCamera yaw="195" pitch="35" />
  <BoxAnnotation color="#5fdbc4" min="1 1 1" max="2 2 2">First Cultivation Chamber</BoxAnnotation>
</GameScene>

## 3. Second chamber

Stack the second Cultivation Chamber immediately above the first. Fill the eight shell positions around it.

<GameScene zoom="2" interactive={true} background="transparent">
  <ImportStructure src="structure/array-layer-3.snbt" />
  <IsometricCamera yaw="195" pitch="35" />
  <BoxAnnotation color="#5fdbc4" min="1 2 1" max="2 3 2">Second Cultivation Chamber</BoxAnnotation>
</GameScene>

## 4. Roof

Fill all nine positions. The blocks in the shell can be swapped for valid array parts. The controller and the two chambers have fixed positions.

<GameScene zoom="2" interactive={true} background="transparent">
  <ImportStructure src="structure/array-minimum.snbt" />
  <IsometricCamera yaw="195" pitch="35" />
</GameScene>

[Connect the array and start a crop](array.md).

## Adjust port directions

Use a wrench to turn the network access, seed input, or output port in any of six directions. ME Seed Storage and Crop Storage rotate horizontally. Once the array forms, its controller stays facing the same way.
