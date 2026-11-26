---
navigation:
  parent: array.md
  title: 逐层搭建
  position: 10
  icon: appliedagristics:cultivation_chamber
---

# 逐层搭建

每张图都包括前面已摆好的层。控制器这一侧是正面，拖动任意预览即可换角度。

## 第 1 层 · 底层

将底部 3 × 3 的九个格子填满。图中的输出口在正面中央。

<GameScene zoom="2" interactive={true} background="transparent">
  <ImportStructure src="structure/array-layer-1.snbt" />
  <IsometricCamera yaw="195" pitch="35" />
</GameScene>

## 第 2 层 · 控制器与第一个培养舱

控制器摆在这一层的正面中央。**第一个农业培养舱放在控制器正后方**，也就是这一层的中心。普通种子仓和网络接入模块装在外壳。

<GameScene zoom="2" interactive={true} background="transparent">
  <ImportStructure src="structure/array-layer-2.snbt" />
  <IsometricCamera yaw="195" pitch="35" />
  <BoxAnnotation color="#5fdbc4" min="1 1 1" max="2 2 2">第一个农业培养舱</BoxAnnotation>
</GameScene>

## 第 3 层 · 第二个培养舱

**第二个农业培养舱叠在第一个正上方。** 四周八格填外壳。

<GameScene zoom="2" interactive={true} background="transparent">
  <ImportStructure src="structure/array-layer-3.snbt" />
  <IsometricCamera yaw="195" pitch="35" />
  <BoxAnnotation color="#5fdbc4" min="1 2 1" max="2 3 2">第二个农业培养舱</BoxAnnotation>
</GameScene>

## 第 4 层 · 封顶

顶层九格全部填满。外壳可以换成合法部件，控制器和两枚内置培养舱的位置固定。

<GameScene zoom="2" interactive={true} background="transparent">
  <ImportStructure src="structure/array-minimum.snbt" />
  <IsometricCamera yaw="195" pitch="35" />
</GameScene>

[联网并开始种植](array.md)。

## 调整端口朝向

用扳手可以把接入模块、普通种子仓和输出口转向六个方向。ME 种子仓和作物仓可以水平旋转。阵列成型后，控制器的朝向锁定。
