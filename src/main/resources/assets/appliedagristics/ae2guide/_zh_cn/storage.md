---
navigation:
  parent: index.md
  title: 种子仓
  position: 30
  icon: appliedagristics:seed_storage
categories:
  - agr-array
item_ids:
  - appliedagristics:seed_storage
  - appliedagristics:planter_input_port
---

# 种子仓

<Row gap="5">
  <BlockImage id="appliedagristics:planter_input_port" scale="4" />
  <BlockImage id="appliedagristics:seed_storage" scale="4" />
</Row>

| 方块 | 容量 |
|---|---|
| <ItemLink id="appliedagristics:planter_input_port" /> | 16 格，每格最多 64 个 |
| <ItemLink id="appliedagristics:seed_storage" /> | 种类不限，默认每种最多 256 个 |

两种仓都装时，控制器使用 ME 种子仓。不同物种的生物种子分别计数。

## 普通种子仓

手动放种子或用管道输入。手动取出正在使用的种子时，对应作物会暂停，占用的种子随之释放。

## ME 种子仓

- **收容种子**从网络取种。侧栏里可以开启自动收容，并筛选收容类别。
- **推出种子**需要二次确认，并会关闭自动收容。网络装不下的种子留在仓内。
- 阵列自己产出的新种子优先回到本阵列的 ME 种子仓。胡萝卜、土豆等兼作产物的种植材料只补到当前种植所需的数量。

在服务端配置文件里用 `storage.meSeedsPerType` 调整单种上限。调低后会保留旧库存。

[查看收获物的去向](crop-output.md)。
