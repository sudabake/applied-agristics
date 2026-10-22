---
navigation:
  title: ME 农业阵列
  icon: appliedagristics:network_planter
item_ids:
  - appliedagristics:network_planter
  - appliedagristics:seed_storage
  - appliedagristics:me_network_access
  - appliedagristics:planter_input_port
  - appliedagristics:planter_output_port
  - appliedagristics:essence_vault
  - appliedagristics:planter_frame
  - appliedagristics:cultivation_chamber
  - appliedagristics:planter_speed_module
  - appliedagristics:planter_parallel_module
  - appliedagristics:planter_conversion_module
---

# ME 农业阵列

阵列宽 3、深 3、高 4。可拖动下面的结构查看。

<GameScene zoom="2" background="transparent">
  <ImportStructure src="structure/array.snbt" />
</GameScene>

## 合成进阶

基础种植站和细胞培养器可在前期制作。采样器每次制作 4 个，空培养胶囊每次 4 个，营养剂每次 8 份；萃取会返还空胶囊。

农业阵列框架每次制作 4 个，培养舱使用二级精华。控制器由基础种植站、AE 材料与三级精华升级；搭配接入模块、输入口和输出口即可运行，不需要先制作种子仓或精华存储器。

速度、并行模块使用三级精华和对应 AE 卡制作，配方固定，逐块添加。种子转换模块使用四级精华。种子仓使用五级精华与 64K 存储组件，精华存储器使用五级精华与 256K 存储组件，作为后期仓储升级。

ME 注魔祭坛由神秘农业普通注魔祭坛升级。全部设备配方都可在 JEI 中查看。

## 固定的上半部

最上层九格全部放农业阵列框架。自上往下第二层中心放培养舱，其余八格放框架。成型后，上层显示通风结构，第二层显示培养观察窗。

## 可替换的下半部

自下往上第二层中心仍放一个培养舱，正面中央放 ME 种植控制器。其余位置可以放框架、输入输出口、种子仓、精华存储器或升级模块。

整座阵列只允许一个控制器、一个 ME 网络接入模块。至少需要一种种子来源（输入口或种子仓），以及一种产物去向（输出口或精华存储器）。示例展示满升级及两种可选仓储，并非强制的面板摆法。

速度、并行模块各最多四块。第五块会阻止成型。四块满配分别达到服务器配置的速度和并行上限；不是将原有单块效果简单缩减成四次翻倍。目标耗电仍为每种满配、种子占满且正在运行的作物约 5000 AE/t，多作物相加。

## 种子、产物与联网

只有成型后的 ME 网络接入模块可以连接 AE，整座结构使用一个通道。上半部不能放接入模块和升级模块。

每种作物可以逐渐占满并行槽。正在使用的种子不计入 AE 可用库存，关闭作物后返还。达到精华库存目标后暂停，库存降低后自动恢复；目标 -1 为持续生产。

种子转换模块只转换收获中新产生的多余种子。未占满并行时先保留新增种子，不消耗已有库存种子。

精华存储器只接收本结构产生的精华，AE 只能提取；普通作物产物需要输出口。仅种子仓与精华存储器拆卸保留数据，其他机器会单独掉出内部物品。

拆除控制器积压的大批产物在原位置分批掉落。未发出的数量随世界保存；附近掉落过多或区块未加载时等待，已发出的物品仍会按原版规则消失。

## 查看结构和说明

背包或 JEI 中悬停任意阵列部件，使用 JEI 的用途键（通常 U）进入农业阵列结构页。结构页可以旋转、按层查看，并列出示例材料；点击“打开说明”进入本页。

物品悬停时按住 GuideME 的说明键（通常 G）查看本页。世界中准星对着任意阵列部件，按“阵列结构用途”或“阵列说明”键也能打开对应页面。世界快捷键可在游戏按键设置中修改；没有 JEI 时结构键会打开本说明。
