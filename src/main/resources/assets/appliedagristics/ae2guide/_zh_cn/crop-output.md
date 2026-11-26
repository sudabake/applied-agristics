---
navigation:
  parent: index.md
  title: 作物输出
  position: 40
  icon: appliedagristics:essence_vault
categories:
  - agr-array
item_ids:
  - appliedagristics:essence_vault
  - appliedagristics:planter_output_port
---

# 作物输出

<Row gap="5">
  <BlockImage id="appliedagristics:planter_output_port" scale="4" />
  <BlockImage id="appliedagristics:essence_vault" scale="4" />
</Row>

| 方块 | 怎样取产物 |
|---|---|
| <ItemLink id="appliedagristics:planter_output_port" /> | 手动取、用管道抽取，或开启自动输出，从带图案的一面推给相邻容器。 |
| <ItemLink id="appliedagristics:essence_vault" /> | 从已连接的 ME 网络取出。 |

## 产物去向与容量

两种方块都装时，收获物先进作物仓。作物仓默认容量无限，可以通过配置文件的 `storage.cropVaultCapacity` 修改总量上限。设为有限容量后，溢出先进 ME 网络，再尝试输出口。两处都满时，阵列会保留待输出的收获。

## 控制生产

在控制器里把**库存目标**设为希望 ME 网络保有的数量。设为 `-1` 时，有输出空间就会持续生产。**自动启用新作物**只影响第一次进入阵列的作物。

阵列成型后自带培养环境。普通作物、树苗和兼容配方的可能产物都能在 JEI 查看。
