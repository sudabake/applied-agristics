# 平台与依赖版本记录

本页是 **2026-10-13 的依赖选型记录**，表内“最新稳定版”仅指当日核查结果，不代表持续更新的版本排行。当前锁定值见根目录 `gradle.properties`，各次实测组合见 `VERIFICATION.md`。以下保留当时核查记录；移植平台尚未测试。

## 平台路线

| 平台 | 定位 | JDK |
| --- | --- | --- |
| Minecraft 1.21.1 / NeoForge | 优先开发主线 | 64 位 JDK 21 |
| Minecraft 1.20.1 / Forge | 主线稳定后的移植目标 | 64 位 JDK 17 |

依据：[NeoForge 1.21.1 开发文档](https://docs.neoforged.net/docs/1.21.1/gettingstarted/)、[Forge 1.20.1 开发文档](https://docs.minecraftforge.net/en/1.20.1/gettingstarted/)。移植目标不等于已经支持，两个平台需要各自的构建与运行验证。

## 稳定版选择规则

1. 按 Minecraft 版本与加载器筛选，按发布时间排序，仅将 Release 计为稳定版。
2. 用最新稳定版开发和测试，上一稳定版作为最低支持目标。
3. 首次运行时验证最新组合、最低组合及混合组合，避免代码无意中调用仅在最新版本存在的 API。
4. 必需库还须满足上游模组明确声明的最低要求；不允许为了取“上一稳定版”而安装被上游排除的版本。
5. 每个发布版本保存具体依赖和验证记录；上游新版本出现时重新评估，不自动提高已经发布版本的最低要求。
6. 对未测试的新大版本不作无限兼容承诺；模组元数据的版本范围在实际验证后确定。

## 1.21.1 NeoForge 主线

| 依赖 | 最新稳定版（开发基线） | 上一稳定版 | 最低支持目标 |
| --- | --- | --- | --- |
| AE2 | 19.2.18 | 19.2.17 | 19.2.17，开发游戏测试通过 |
| Mystical Agriculture | 8.0.28 | 8.0.27 | 8.0.27，开发游戏测试通过 |
| GuideME（AE2 的必需库） | 21.1.19 | 21.1.18 | 21.1.18，开发游戏测试通过 |
| Cucumber Library（神秘农业的必需库） | 8.0.16 | 8.0.15 | 至少 8.0.16，上游明确要求 |

神秘农业 8.0.27 与 8.0.28 的 JAR 元数据均要求 Cucumber `[8.0.16,)`，因此 8.0.15 无法作为该组合的最低目标。这里保留上游兼容性约束，不覆盖它。

AE2 19.2.18 的 JAR 声明 NeoForge `[21.1.169,)`、Minecraft `[1.21.1]`、GuideME `[21.1.1,)`。这些是上游要求，不等于本项目已验证的支持下限。

### 待核实的元数据差异

Modrinth / CurseForge 将神秘农业 8.0.27、8.0.28 标注为支持 1.21.1，但下载 JAR 中的 `META-INF/neoforge.mods.toml` 写有 Minecraft 范围 `[1.21,1.21.1)`。下载文件的 SHA512 与 Modrinth 声明一致。

该范围文字与发布平台标注存在差异。本轮在 NeoForge 21.1.259 下，8.0.27 与 8.0.28 均实际加载并通过开发游戏测试；8.0.28 还通过客户端验证。未修改上游 JAR，也未设置 dependencyOverrides。结论只覆盖实测组合。

来源：[AE2 19.2.18](https://modrinth.com/mod/ae2/version/KDnFUmMm)、[AE2 19.2.17](https://modrinth.com/mod/ae2/version/kfyIqgJ6)、[神秘农业 8.0.28](https://modrinth.com/mod/mystical-agriculture/version/PLakgT6X)、[神秘农业 8.0.27](https://modrinth.com/mod/mystical-agriculture/version/izIaJr8V)、[GuideME 版本](https://modrinth.com/mod/guideme/versions)、[Cucumber 版本](https://modrinth.com/mod/cucumber/versions)。

## 1.20.1 Forge 后续移植

| 依赖 | 最新稳定版（核查日） | 上一稳定版／最低候选 |
| --- | --- | --- |
| AE2 | 15.4.11 | 15.4.10 |
| Mystical Agriculture | 7.0.24 | 7.0.23 |
| GuideME | 20.1.15 | 20.1.14 |
| Cucumber Library | 7.0.16 | 7.0.15 |

AE2 15.4.11 的 JAR 声明 Forge `[47.1.3,)`、Minecraft `[1.20.1,1.20.2)`、GuideME `[20.1.7,20.2.0)`。神秘农业 7.0.24 的 JAR 声明 Forge `[47.1.0,)`、Minecraft `[1.20.1]`、Cucumber `[7.0.5,)`。

移植开始时重新核查稳定序列并验证候选最低组合；当前不创建第二套开发工程或声称 Forge 支持已完成。

来源：[AE2 15.4.11](https://modrinth.com/mod/ae2/version/jYgSnxye)、[AE2 15.4.10](https://modrinth.com/mod/ae2/version/7KVs6HMQ)、[神秘农业 7.0.24](https://modrinth.com/mod/mystical-agriculture/version/EEilTecF)、[神秘农业 7.0.23](https://modrinth.com/mod/mystical-agriculture/version/L4VoS46I)。

## 当前验证状态

- 1.21.1 工程已建立：ModDevGradle 2.0.148、Gradle 9.2.1、Java 21。按用户选择，开发基线和声明的 NeoForge 最低版本已改为实测的 21.1.248；AE2 自身的 21.1.169 要求不代表本项目已验证更旧的加载器。
- NeoForge 21.1.248 已通过构建、6 项单元测试、最新与最低前置组合各 5 项 GameTest，以及最新组合的四种客户端页面和操作消息测试。此前 21.1.259 的混合组合记录保留，不将其当作 21.1.248 的混合组合实测结果。
- 最新、最低及 AE2／农业混合组合的开发 GameTest 均通过，最新组合客户端页面和操作消息通过。
- Cucumber 最低仍为 8.0.16，GuideME 最低声明为 21.1.18。没有穷举其他库版本排列。
- 1.20.1 Forge 移植、正式独立服务器玩家连接、长期整合包兼容性尚未验证。
- 具体测试与证据见 [VERIFICATION.md](VERIFICATION.md)。
