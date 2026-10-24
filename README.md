# Applied Agristics

Minecraft **1.21.1 · NeoForge 21.1.248 · Java 21** 的 AE2 × Mystical Agriculture 农业附属。

当前公开源码版本为 **0.1.0-alpha.17**，仍处于测试和平衡阶段。**Alpha.17 安装包暂时下架，正在准备原创材质和下一版修正。** 源码构建方法见下文。1.20.1 Forge 是后续移植计划，当前不支持。

## 内容

- 独立免电基础种植站：耕地、可选用水加速、静态作物展示、漏斗及管道接口。
- 3×3×4 ME 农业阵列：上两层固定，下两层安装功能部件；速度和并行模块各最多四块。
- 输入／输出口各 16 格；后期可选种子仓和只读精华存储器。只有有效结构通过独立接入模块连接 AE。
- 全网库存目标补货，目标 -1 持续生产；新增种子优先补并行，种子转换不消耗原有种子。
- 生物采样、消耗式培养与萃取、生物种子及精华兑换；支持 JEI 生物变体与配方、GuideME 阵列说明。
- ME 注魔祭坛替换原中心方块，保留八个真实基座、四张独立 AE 加速卡和 AE 处理样板接口。
- 仅种子仓与精华存储器拆卸保留库存数据，其他机器单独掉出物品。

## 构建

安装 **64 位 JDK 21**，确认 `java -version` 显示 21，并让 `JAVA_HOME` 指向该 JDK。无需另装 Gradle；仓库包含 Wrapper。首次构建需要联网下载 Minecraft、NeoForge、依赖和构建工具。

Windows PowerShell：

```powershell
git clone https://github.com/sudabake/applied-agristics.git
cd applied-agristics
.\gradlew.bat build
```

Linux / macOS：

```sh
git clone https://github.com/sudabake/applied-agristics.git
cd applied-agristics
bash gradlew build
```

产物为 `build/libs/applied-agristics-0.1.0-alpha.17.jar`。安装到对应 Minecraft 实例的 mods 目录，避免同时放入多个本 Mod 版本。

开发锁定版本见 `gradle.properties`：AE2 19.2.18、Mystical Agriculture 8.0.28、Cucumber 8.0.16、GuideME 21.1.19。最低实测组合为 AE2 19.2.17、MA 8.0.27、Cucumber 8.0.16、GuideME 21.1.18。运行时需要这些前置；JEI 为推荐可选前置，Botany Pots 为可选兼容。依赖 JAR 不随本 Mod 打包。

## 开发与验证

```powershell
.\gradlew.bat test
.\gradlew.bat runGameTestServer
.\gradlew.bat runClient
.\gradlew.bat runClient -PjeiTests=true
.\gradlew.bat runGameTestServer -Pae2_version=19.2.17 -Pma_version=8.0.27 -Pguideme_version=21.1.18
python tools/check_resource_rebuild.py
```

其他系统将 `.\gradlew.bat` 换为 `bash gradlew`。资源重建检查需要 Python 3.10+，在隔离副本连续重建两次，不覆盖工作资源。普通构建直接使用已提交资源，不需要运行美术生成器。

`run/` 是可丢弃的开发游戏实例，不要放入个人存档。`-PclientSmoke=true` 仅用于已有专用 `agr-smoke` 开发存档的自动客户端测试，会修改该存档并自动退出；普通 `runClient` 不运行它。开发测试类不会进入最终 JAR。CI 验证构建和游戏测试，不创建 Release 或上传本机日志／发行附件。

Alpha.17 已修复命名种子的重种扣除、短注魔配方及余料映射、订单导出标志残留、组件转移配方接单。详见 [审阅记录](docs/CODE_REVIEW_2026-10-23.md)、[结构规则](docs/ME_MULTIBLOCK.md)、[种植兼容](docs/CROP_DATA.md) 和 [验证记录](docs/VERIFICATION.md)。

升级 Alpha.16 之前的世界时，请先备份存档并保持原有数据包／Mod 目录不变：首次加载会建立生物兑换图案映射；之后新增输出只追加，删除输出保留禁用位置。

许可证：**LGPL-3.0-only**，见 [LICENSE](LICENSE) 与 [COPYING](COPYING)。第三方资源的版权及许可证见 [THIRD_PARTY_NOTICES.md](THIRD_PARTY_NOTICES.md)。
