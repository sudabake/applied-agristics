# 平台与依赖版本记录

核查日期：2026-10-13。信息来自作者在 Modrinth 发布的版本记录及下载 JAR 中的模组元数据；这是候选配置，尚未完成构建或游戏启动测试。

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
| AE2 | 19.2.18 | 19.2.17 | 19.2.17，待运行验证 |
| Mystical Agriculture | 8.0.28 | 8.0.27 | 8.0.27，待运行验证 |
| GuideME（AE2 的必需库） | 21.1.19 | 21.1.18 | 21.1.18，待运行验证 |
| Cucumber Library（神秘农业的必需库） | 8.0.16 | 8.0.15 | 至少 8.0.16，上游明确要求 |

神秘农业 8.0.27 与 8.0.28 的 JAR 元数据均要求 Cucumber `[8.0.16,)`，因此 8.0.15 无法作为该组合的最低目标。这里保留上游兼容性约束，不覆盖它。

AE2 19.2.18 的 JAR 声明 NeoForge `[21.1.169,)`、Minecraft `[1.21.1]`、GuideME `[21.1.1,)`。这些是上游要求，不等于本项目已验证的支持下限。

### 待核实的元数据差异

Modrinth / CurseForge 将神秘农业 8.0.27、8.0.28 标注为支持 1.21.1，但下载 JAR 中的 `META-INF/neoforge.mods.toml` 写有 Minecraft 范围 `[1.21,1.21.1)`。下载文件的 SHA512 与 Modrinth 声明一致。

该范围文字与发布平台标注存在差异。尚未测试加载器如何处理该元数据，不能据此声称已经可用或已经不兼容。建立开发环境时应优先核实作者构件与实际启动结果；不修改上游 JAR 以绕过检查。

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

- 已完成：稳定发布序列查询、最新主线／移植依赖 JAR 的元数据检查、主线神秘农业两个候选 JAR 的 SHA512 校验。
- 未完成：Gradle 开发环境、源码编译、客户端启动、独立服务器启动及最低版本组合验证。
- 下一步：建立 1.21.1 NeoForge 官方模板工程，锁定具体加载器构建及 JDK 21，优先解决上述元数据差异，再加入最小 Mod 入口。
