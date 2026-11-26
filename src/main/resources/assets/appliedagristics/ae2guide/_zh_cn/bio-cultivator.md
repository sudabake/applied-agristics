---
navigation:
  parent: index.md
  title: ME 生物培养器
  position: 60
  icon: appliedagristics:me_bio_cultivator
categories:
  - agr-machines
item_ids:
  - appliedagristics:me_bio_cultivator
  - appliedagristics:bio_sampler
  - appliedagristics:bio_seed
  - appliedagristics:culture_nutrient
  - appliedagristics:empty_bio_seed
---

# ME 生物培养器

<BlockImage id="appliedagristics:me_bio_cultivator" scale="5" />

ME 生物培养器把生物样本做成可以种植的生物种子。

## 输入和产物

| 各放一份 | 产出 |
|---|---|
| 已取样的 <ItemLink id="appliedagristics:bio_sampler" />、<ItemLink id="appliedagristics:culture_nutrient" /> 和 <ItemLink id="appliedagristics:empty_bio_seed" /> | 对应物种的 <ItemLink id="appliedagristics:bio_seed" /> |

手持生物棉签右键生物即可采样。合成营养试剂时会拿回空桶。

## 自动制作

在 JEI 选中物种，将配方填入 AE 加工样板，再交给样板供应器。ME 订单完成后，种子直接返还网络。

手动供料时，可在侧栏设置自动输出到相邻容器。基础加工时间是 10 秒，可装四张 <ItemLink id="ae2:speed_card" />。
