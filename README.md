# Applied Agristics 应用农业

Minecraft **1.21.1 / NeoForge / Java 21**，AE2 × Mystical Agriculture 农业附属。

当前版本为 **1.3.0**。

## 主要内容

- 基础种植机使用种子和合适的土壤生产，水提供加速。特殊作物按要求在机器下方放置基座。
- 生物棉签右键采样，不伤害生物。ME 生物培养器消耗已采样棉签、营养试剂和空白生物种子，直接生成对应种子。基础 10 秒、4 AE/t，无耗水，支持四张独立加速卡及 AE 加工样板。
- ME 农业阵列为 3×3×4，多方块成型后通过唯一的 ME 接入模块联网，自身不占频道。普通种子仓和输出口支持前期使用，ME 种子仓与作物仓提供后期仓储。
- 速度、并行模块各最多四块，默认满配分别为 256 倍速度与 256 并行容量。实际产量取决于占用种子数、作物周期和输出能力。
- 新生种子优先进入同阵列 ME 种子仓，其余收获物优先作物仓、随后输出口。普通种子仓不自动回填。作物仓接受本阵列全部收获，AE 仅可提取。
- ME 种子仓支持手动收容、自动收容、分类过滤和二次确认推出。控制器可设置新发现作物默认启用，默认关闭。
- 转换模块只作用于神秘农业与本 Mod 生物精华作物，先补并行，再将新生余量按 1∶1 转为精华。普通作物的种子收存保持正常。
- ME 注魔祭坛替换原注魔中心，使用八个真实神秘农业基座，支持 AE 下单、四张独立加速卡和按面自动输出。
- 扳手可旋转和 Shift＋右键拆卸。仅 ME 种子仓与作物仓随方块保留库存，其他机器内部物品单独掉落。
- JEI 提供配方、种植产物和可拖动结构页，GuideME 提供说明。Jade、JDTE 与 Botany Pots 适配均为可选。

阵列使用独立完整面，五面共用机壳、功能正面单独绘制，三种模块六面复用各自贴图。培养舱透明，控制器保留屏幕状态动画。

## 构建

安装 JDK 21，在项目根目录运行 `gradlew.bat build`（Windows）或 `bash gradlew build`。成品位于 `build/libs/`。运行 `gradlew.bat runGameTestServer` 执行游戏测试。

开发依赖为 NeoForge 21.1.248、AE2 19.2.18、Mystical Agriculture 8.0.28、GuideME 21.1.19 和 Cucumber 8.0.16。此前验证过最低核心组合 NeoForge 21.1.241、AE2 19.2.17、Mystical Agriculture 8.0.27、GuideME 21.1.17 和 Cucumber 8.0.16，完整整合包兼容范围见验证记录。

## 项目资料

- [机器与多方块规则](docs/ME_MULTIBLOCK.md)
- [作物数据与公式](docs/CROP_DATA.md)
- [下一版更新日志](docs/CHANGELOG_NEXT.md)

许可证为 LGPL-3.0-only。第三方声明见 [THIRD_PARTY_NOTICES.md](THIRD_PARTY_NOTICES.md)。
