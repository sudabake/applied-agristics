---
navigation:
  parent: index.md
  title: 阵列升级
  position: 50
  icon: appliedagristics:planter_speed_module
categories:
  - agr-array
item_ids:
  - appliedagristics:planter_speed_module
  - appliedagristics:planter_parallel_module
  - appliedagristics:planter_conversion_module
---

# 阵列升级

<Row gap="5">
  <BlockImage id="appliedagristics:planter_speed_module" scale="4" />
  <BlockImage id="appliedagristics:planter_parallel_module" scale="4" />
  <BlockImage id="appliedagristics:planter_conversion_module" scale="4" />
</Row>

| 部件 | 作用 |
|---|---|
| <ItemLink id="appliedagristics:planter_speed_module" /> | 缩短两次收获之间的时间，最多装四块。 |
| <ItemLink id="appliedagristics:planter_parallel_module" /> | 让同一种作物同时使用更多种子，最多装四块。 |
| <ItemLink id="appliedagristics:planter_conversion_module" /> | 先补满当前种植需求，再把当批多余的神秘农业种子或生物种子转为对应精华。 |

## 默认升级数值

| 各类模块数量 | 速度倍率 | 每种作物可用种子 |
|---:|---:|---:|
| 0 | 1× | 1 |
| 1 | 2× | 4 |
| 2 | 7.10× | 16 |
| 3 | 36.66× | 64 |
| 4 | 256× | 256 |

产量按实际使用的种子算，耗电和产量也受作物及输出空间影响。没有作物仓时，每批最多处理 64 轮。

默认配置下，四块速度模块、四块并行模块、单种作物用满 256 颗种子时，该作物的耗电目标约为 5,000 AE/t，实际值受批量和输出限制。

## 满配示例

<GameScene zoom="2" interactive={true} background="transparent">
  <ImportStructure src="structure/array.snbt" />
  <IsometricCamera yaw="195" pitch="35" />
</GameScene>

升级模块可放在任意合法外壳格。初次建阵列可以先按[最低配置](array.md)搭建。
