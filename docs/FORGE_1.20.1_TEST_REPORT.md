# 应用农业 1.3.2 Forge 移植验收

测试基线为公开版 `v1.3.2` 的 `581587d314129117a4a873fa49a068141838f700`。移植分支为 `port/forge-1.20.1`。构建和运行使用 Minecraft 1.20.1、Forge 47.4.20、Java 17、AE2 15.4.10、神秘农业 7.0.24、GuideME 20.1.15 和 Cucumber 7.0.16。

## 核心功能

| 验收项 | 实测结果 | 记录 |
| --- | --- | --- |
| 构建与资源 | 21 项 Java 单测、21 项 Python 测试、189 项资源两次重建、重映射 JAR 审计通过 | `minimal-server-final.log`、`jar-audit.json` |
| 最小独立服务端 | 115 项 GameTest 通过，覆盖阵列成型、六向端口、拆装、收容和路由、生物种植、加工返网、Shift＋右键插卡、配方和产量公式 | `minimal-server-final.log` |
| 最小客户端 | 八个机器界面、两台 ME 加工机四卡槽、IO、种子仓二次确认、控制器动画、培养器八视角、透明层、资源重载通过 | `minimal-client-2.log` |
| JEI 和 GuideME | 种植产物、带物种的加工样板、可拖动及分层结构预览和 AE 指南页通过客户端测试 | `minimal-client-2.log` |
| 真实关闭重进 | AE 磁盘保存 7 颗牛种子和 11 颗猪种子。另起客户端后分别提取成功，供应器中的加工样板仍有牛棉签与牛种子身份 | `storage-restart-prepare.log`、`storage-restart-verify.log` |
| Project Infinity 猪咪版 1.1.1 副本 | 使用交付版 JAR 进入测试存档，JEI 初始化完成并正常保存退出。细项回归在下列独立测试配置中执行 | `infinity-client-3.log` |

## 联动清单

| 联动 | 实测范围 | 记录 |
| --- | --- | --- |
| Botany Pots 13.0.0 | 实际植物盆配方补充作物，验证仙人掌、土壤和掉落。常规原版作物保持原生收获规则 | `jdte-botany-final.log` |
| Jade 11.13.3 | 客户端实际显示牛作物与 43% 生长进度 | `eaep-jade/screenshots/agr-next-plants-and-jade.png` |
| JDTE 0.5.8 | 在独立配置启用。普通及大型温室均完成 512 轮同物种生产，保留 64 颗栽培用种子。增殖概率边界、能量／流体结算、输出堵塞、缺能、缺流体和保存读取通过 | `jdte-botany-final.log` |
| ExtendedAE 1.4.20 ＋ EAEP 1.6.2 | 从 JEI 编码含物种样板，通过 EAEP 实际界面上传，供应器派单，培养完成后牛种子回到 AE 网络 | `eaep-jade-1.log` |
| Cell4 1.0.4fix3 ＋ NeoEcoAE 20.5.7 | 3 项专项通过，80 轮升级模块变化及阵列重组中核对 AE 缓存、即时库存和实际数量 | `cell4-neoeco-1.log` |
| Botany Trees 9.0.20、沉浸工程 10.2.0-183、MekMM 1.1.2 | 在附加配置运行 115 项测试。已加载玻璃罩与种植机配方合计覆盖 231 个输入项、196 种不同材料，均能在种植目录使用 | `extra-plants-1.log` |
| 神秘学 1.158.0、新生魔艺 4.12.7 | 验证普通异界树、天然伪装异界树及四种至高树的实际原木掉落 | `extra-plants-1.log` |
| 工业先锋 3.5.22 | 装载工业先锋后验证原生常规作物及树木收获。覆盖范围沿用 1.3.2 的原生生长和掉落逻辑 | `extra-plants-1.log` |

## 服务端压力回归

100 台阵列共用一个 AE 网络，每台启用 30 种作物，每种占用 256 颗种子。每台装满四个速度和四个并行模块，打开自动收容。预热 400 tick 后测量 1200 tick，检查全程保持满配、种植和耗电。

平均 18.97 ms/t，P95 58.03 ms/t，最高 98.03 ms/t。1200 tick 中有 300 tick 超过 50 ms，满配场景仍存在瞬时卡顿。原始数据见 `build/port-run/benchmark-shared/benchmark-result.json`。

## 测试环境说明

本地测试日志在 `build/port-logs`，测试世界和截图在 `build/port-run`。整包副本内停用了启动时下载超时的 I18nUpdateMod，本次启动为 Java 联网设置了超时。整包仍记录了自带 Backport 资源包的 ZIP 错误及 Sky Logistics 指南解析错误，随后完成世界加载。独立 EAEP 日志带有上游装配矩阵和可选配方的警告，样板上传、派单和返网实际通过。

本地交付包括重映射 JAR、独立可编辑资源包、SHA-256 和逐文件修改时间基线。1.20.1 Forge 与 1.21.1 NeoForge 各用对应存档。26.1.2 暂缓，平台发布另行安排。
