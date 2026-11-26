---
navigation:
  parent: index.md
  title: 搭建 ME 农业阵列
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

# 搭建 ME 农业阵列

阵列是 **3 × 3 × 4**（宽 × 深 × 高）。控制器面朝外，放在**从下数第二层的正面中央**。手动搭建时可以直接[看逐层摆法](array-layers.md)。

右键控制器，点**一键搭建**。等结构成型后，把接入模块连到有电的 ME 网络。

<GameScene zoom="2" interactive={true} background="transparent">
  <ImportStructure src="structure/array-minimum.snbt" />
  <IsometricCamera yaw="195" pitch="35" />
  <BoxAnnotation color="#5fdbc4" min="1 1 0" max="2 2 1">控制器位置</BoxAnnotation>
</GameScene>

拖动上图可旋转最低配置的阵列。

## 最低用料

| 部件 | 数量 |
|---|---:|
| <ItemLink id="appliedagristics:network_planter" /> | 1 |
| <ItemLink id="appliedagristics:cultivation_chamber" /> | 2 |
| <ItemLink id="appliedagristics:me_network_access" /> | 1 |
| <ItemLink id="appliedagristics:planter_input_port" /> | 1 |
| <ItemLink id="appliedagristics:planter_output_port" /> | 1 |
| <ItemLink id="appliedagristics:planter_frame" /> | 30 |

普通种子仓可以换成 ME 种子仓，输出口可以换成作物仓。ME 网络接入模块恰好需要一个。

## 搭好后开始生产

1. 右键控制器，点**一键搭建**。生存模式从背包取材，创造模式搭[满配阵列](array-upgrades.md)。也可以[手动摆放](array-layers.md)。
2. 等结构成型，把 ME 网络接入模块接到有电的 ME 网络。阵列不占频道。
3. 放入一颗种子，在控制器里启用对应作物。到[输出部件](crop-output.md)查看第一批收获。

## 搭好了却没动静

| 提示或现象 | 检查方法 |
|---|---|
| 聊天栏提示缺料 | 补齐提示里的部件，再点一键搭建。 |
| 闪烁红框 | 腾出被标记的格子。 |
| 外壳不完整或缺培养舱 | 对照[逐层图](array-layers.md)检查两枚藏在内部的培养舱和外壳。 |
| 已成型，网络离线 | 把接入模块连到有电的 ME 网络。 |
| 已在线，没产物 | 放入种子、启用作物，再检查有没有输出空间。 |

对着阵列部件按 **U** 可以打开 JEI 结构页。
