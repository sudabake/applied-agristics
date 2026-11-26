# Applied Agristics 1.4.0 · Forge 1.20.1

## 中文

- 重做英文和简体中文 GuideME。阵列搭建页包括最低用料、可旋转的逐层预览和故障排查。按 G 可打开对应阵列部件的说明。
- 新增阵列一键搭建。生存模式消耗背包材料补齐最低配置，创造模式搭建满配。缺料在聊天栏提示，被占用的格子会闪烁红框。
- 服务端配置按功能分类并附中英文注释，保留已有设置。ME 种子仓种类不限，默认每种最多 256 个。
- 作物仓默认容量无限。设定有限容量后，溢出优先进 ME 网络，余量尝试输出口。调低上限保留现有库存。
- 基础种植机进度在收获结算时显示完成。控制器屏幕按正面外侧取光，修复顶上有方块时屏幕过暗。

客户端和服务器需同时升级。本测试发行版运行在 Minecraft 1.20.1 Forge（Java 17），需要 Applied Energistics 2 和 Mystical Agriculture。

## English

- Rebuild GuideME in English and Simplified Chinese. The array guide shows minimum materials, rotatable layer previews, and troubleshooting. Press G on an array part to open its relevant page.
- Add Build Array to the controller. Survival uses inventory materials to complete the minimum structure. Creative builds the upgraded array. Missing items appear in chat and occupied spaces flash red.
- Group server configuration settings and document them in both languages. Existing settings migrate to their new sections. ME Seed Storage keeps unlimited seed types, with a configurable default limit of 256 per type.
- Crop Storage is unlimited by default. With a finite limit, overflow tries ME first and then the Output Port. Lowering limits preserves existing stock.
- The Basic Planter shows completion when a harvest is committed. The controller screen uses the light at its front face so it remains readable with a block above it.

Update both the client and server. This beta is for Minecraft 1.20.1 Forge (Java 17) and requires Applied Energistics 2 and Mystical Agriculture.
