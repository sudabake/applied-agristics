# Applied Agristics

一个连接 **Applied Energistics 2（AE2）** 与 **Mystical Agriculture（神秘农业）** 的 Minecraft 附属 Mod 项目。

An independent Minecraft addon project connecting Applied Energistics 2 and Mystical Agriculture.

## 项目状态

当前处于设计与初始化阶段。仓库包含项目说明和目录骨架，尚无可运行的 Mod、构建配置或发布版本。

主开发平台已确定为 **Minecraft 1.21.1 / NeoForge / Java 21**。**Minecraft 1.20.1 / Forge / Java 17** 作为后续移植目标，当前尚未建立移植分支或完成兼容性验证。

依赖策略：按对应 Minecraft 与加载器组合，以最新稳定版开发和测试，将上一稳定版作为最低支持目标；最终支持范围须经过实际验证。具体版本、上游约束与待验证问题见 [依赖版本记录](docs/DEPENDENCIES.md)。

## 设计目标

- **前期种植辅助**：改善整合包前期神秘农业种子生长慢的问题，具体速度、成本与配方待设计。
- **单方块祭坛**：探索紧凑的祭坛方块，避免要求玩家搭建多方块结构，具体处理流程待确定。
- **虚拟种植**：探索与 AE2 网络结合的虚拟种植方式，种子管理、产出、能耗与平衡规则待确定。

以上是计划方向，不代表已经实现的功能。

## 项目标识

| 项目 | 值 |
| --- | --- |
| 显示名称 | Applied Agristics |
| 仓库名 | `applied-agristics`（普通 ASCII 拼写） |
| 建议 Mod ID | `appliedagristics`（暂定） |
| 建议 Java 包名 | `io.github.sudabake.appliedagristics`（暂定） |

## 初始目录

```text
applied-agristics/
├── README.md
├── LICENSE
├── COPYING
├── .gitignore
├── .gitattributes
├── docs/
│   ├── DESIGN.md
│   └── DEPENDENCIES.md
└── src/main/
    ├── java/
    │   └── README.md
    └── resources/
        ├── assets/appliedagristics/.gitkeep
        └── data/appliedagristics/.gitkeep
```

资源目录使用暂定 Mod ID 作为命名空间；确定正式标识后可以调整。

## 开发起点

1. 使用已确定的主线平台，验证 AE2 与 Mystical Agriculture 及其必需库的兼容组合。
2. 根据所选加载器的官方开发模板加入 Gradle Wrapper、构建配置与模组元数据。
3. 完成最小 Mod 入口及客户端／服务端启动验证。
4. 从一个最小功能开始实现，再逐步加入祭坛和虚拟种植。

开发环境尚未建立，本仓库目前没有构建命令，也不能直接生成可安装的 JAR。

## 许可证与归属

Copyright (c) 2026 sudabake and contributors.

本项目的原创内容采用 **GNU Lesser General Public License v3.0 only**（SPDX: `LGPL-3.0-only`）。完整许可见 [LICENSE](LICENSE)，该许可引用的 GPLv3 正文见 [COPYING](COPYING)。

第三方 Mod、Minecraft 及其代码、素材和商标保留各自的权利与许可证；仓库不包含其源码或素材。后续引入第三方内容时须保留其许可及归属声明。

Applied Agristics 是独立的附属项目，与 Minecraft、AE2 或 Mystical Agriculture 的官方团队没有隶属关系。
