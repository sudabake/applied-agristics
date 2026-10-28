# Applied Agristics

Minecraft **1.21.1 / NeoForge 21.1.248 / Java 21** 的 AE2 × Mystical Agriculture 农业附属。

**Beta 1.0** 是首个公开测试版，仍处于测试和平衡阶段。下载见 [Releases](https://github.com/sudabake/applied-agristics/releases)。当前不支持 1.20.1 Forge。

## 内容

- 独立免电基础种植站：耕地、可选用水加速、静态作物展示与管道接口。
- 3×3×4 ME 农业阵列：外壳各层可替换，连续外框，速度与并行模块各最多四块。
- 种子仓、ME 种子仓、输出口与只读精华存储器；通过独立 ME 接入模块联网。
- 按全网库存目标自动补货，目标 -1 持续生产；新增种子优先补并行，转换只处理新增的多余种子。
- 生物采样、一次性培养、萃取、生物种子与精华兑换；支持耕地种植、JEI 与 GuideME。
- ME 注魔祭坛保留八个真实基座，支持 AE 处理样板、四张独立加速卡与自动输出。
- 仅 ME 种子仓和精华存储器拆卸后保留库存数据，其余设备单独掉出内部物品。

## 安装

将 `applied-agristics-beta1.0.jar` 放入 Minecraft 1.21.1 NeoForge 实例的 `mods` 目录，并安装以下前置。不要同时加载多个本 Mod 版本。

| 前置 | 最低要求 | 开发版本 |
| --- | --- | --- |
| NeoForge | 21.1.248 | 21.1.248 |
| Applied Energistics 2 | 19.2.17 | 19.2.18 |
| Mystical Agriculture | 8.0.27 | 8.0.28 |
| Cucumber | 8.0.16 | 8.0.16 |
| GuideME | 21.1.18 | 21.1.19 |

JEI 为推荐的可选模组，Botany Pots 为可选兼容。依赖 JAR 不随本 Mod 打包。

## 从源码构建

安装 64 位 **JDK 21**，让 `JAVA_HOME` 指向该 JDK。无需另外安装 Gradle，首次构建需要联网下载工具与依赖。

```powershell
git clone https://github.com/sudabake/applied-agristics.git
cd applied-agristics
.\gradlew.bat build
```

Linux / macOS 使用 `bash gradlew build`。产物为 `build/libs/applied-agristics-beta1.0.jar`。

内部版本号为 `1.0.0-beta.1`；下载名称与 Git 标签采用 `beta1.0`。

## 开发

```powershell
.\gradlew.bat test
.\gradlew.bat runGameTestServer
.\gradlew.bat runClient -PjeiTests=true
python tools/check_resource_rebuild.py
python tools/resource_audit.py
```

资源重建检查需要 Python 3.10+，使用隔离副本，不改工作资源。普通构建直接使用已提交资源，不需要 Aseprite、Blender 或 Python。

`run/`、存档和日志属于本地开发数据，不纳入源码与安装包。`-PclientSmoke=true` 只适用于专用测试存档，会修改该存档并自动退出。测试类和测试空结构不会进入安装包。

搭建规则见 [多方块说明](docs/ME_MULTIBLOCK.md)，种植扩展见 [种植兼容](docs/CROP_DATA.md)。问题反馈请使用 [Issues](https://github.com/sudabake/applied-agristics/issues)。

许可证：**LGPL-3.0-only**，见 [LICENSE](LICENSE)、[COPYING](COPYING)。第三方声明见 [THIRD_PARTY_NOTICES.md](THIRD_PARTY_NOTICES.md)。
