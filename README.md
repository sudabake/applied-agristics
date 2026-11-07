# Applied Agristics 应用农业

Minecraft **1.21.1 / NeoForge / Java 21**，AE2 × Mystical Agriculture 农业附属。

当前版本为 **1.3.1**。

下载见 [GitHub Releases](https://github.com/sudabake/applied-agristics/releases)、[CurseForge](https://www.curseforge.com/minecraft/mc-mods/applied-agristics) 和 [Modrinth](https://modrinth.com/mod/applied-agristics)。

## 主要内容

- 基础种植机使用种子和合适的土壤生产，水提供加速。特殊作物按要求在机器下方放置基座。
- 内置原版树苗、杜鹃和下界巨型菌规则，无需安装 Botany Pots 即可种植。树木默认 2400 tick，每轮 1～4 根原木／菌柄，5% 额外种植材料；橡树和深色橡树另有 5% 苹果，周期与掉落可通过数据包调整。
- 生物棉签右键采样，不伤害生物。ME 生物培养器消耗已采样棉签、营养试剂和空白生物种子，直接生成对应种子。基础 10 秒、4 AE/t，无耗水，支持四张独立加速卡及 AE 加工样板。
- ME 农业阵列为 3×3×4，多方块成型后通过唯一的 ME 接入模块联网，自身不占频道。普通种子仓和输出口支持前期使用，ME 种子仓与作物仓提供后期仓储。
- 速度、并行模块各最多四块，默认满配分别为 256 倍速度与 256 并行容量。实际产量取决于占用种子数、作物周期和输出能力。
- 新生种子优先进入同阵列 ME 种子仓，其余收获物优先作物仓、随后输出口。普通种子仓不自动回填。作物仓接受本阵列全部收获，AE 仅可提取。
- ME 种子仓支持手动收容、自动收容、分类过滤和二次确认推出。控制器可设置新发现作物默认启用，默认关闭。
- 转换模块只作用于神秘农业与本 Mod 生物精华作物，先补并行，再将新生余量按 1∶1 转为精华。普通作物的种子收存保持正常。
- ME 注魔祭坛替换原注魔中心，使用八个真实神秘农业基座，支持 AE 下单、四张独立加速卡和按面自动输出。
- 多方块成型、拆除和重新成型保留部件朝向。接入模块、普通种子仓／输入口和输出口支持六向旋转，ME 种子仓与作物仓保持水平旋转，成型控制器锁定朝向。
- 扳手可旋转和 Shift＋右键拆卸。仅 ME 种子仓与作物仓随方块保留库存，其他机器内部物品单独掉落。
- JEI 提供配方、种植产物和可拖动结构页，GuideME 提供说明。Jade、JDTE 与 Botany Pots 适配均为可选。

阵列使用独立完整面，五面共用机壳、功能正面单独绘制，三种模块六面复用各自贴图。培养舱透明，控制器保留屏幕状态动画。

## 1.3.1 更新

新增本体树木与特殊植物种植规则，修正成型部件朝向、外部配方优先级、实际耗电预估和 JEI 产物预览。详细中英文更新日志见 [1.3.1 更新说明](docs/RELEASE_NOTES.md)。

客户端与服务器需同步更新。旧版已被自动覆盖的部件朝向需要手动调整一次；现用方块身份、库存和种植占用数据继续保留。

## 构建

三个平台的一键发布与首次令牌配置见 [发布说明](docs/PUBLISHING.md)。

安装 JDK 21，在项目根目录运行 `gradlew.bat build`（Windows）或 `bash gradlew build`。成品位于 `build/libs/`。运行 `gradlew.bat runGameTestServer` 执行游戏测试。

开发依赖为 NeoForge 21.1.248、AE2 19.2.18、Mystical Agriculture 8.0.28、GuideME 21.1.19 和 Cucumber 8.0.16。此前验证过最低核心组合 NeoForge 21.1.241、AE2 19.2.17、Mystical Agriculture 8.0.27、GuideME 21.1.17 和 Cucumber 8.0.16。

## 项目资料

- [机器与多方块规则](docs/ME_MULTIBLOCK.md)
- [作物数据与公式](docs/CROP_DATA.md)
- [当前版本更新日志](docs/RELEASE_NOTES.md)
- [1.3.0 历史更新日志](docs/CHANGELOG_NEXT.md)

许可证为 LGPL-3.0-only。第三方声明见 [THIRD_PARTY_NOTICES.md](THIRD_PARTY_NOTICES.md)。
