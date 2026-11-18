# Applied Agristics 1.3.2

## 中文

- 修复 ME 注魔祭坛和 ME 生物培养器无法通过 Shift＋右键安装加速卡的问题。支持主手和副手，四槽满后不再消耗加速卡。
- 修复与 Cell4 共同使用时，种子类库存被错误显示为 9.2e 的问题。请完整重启游戏或服务器以清除旧运行时缓存。
- 输出口仅允许外部提取，开启自动输出后从图案正面即时推送收获物，所有面均可由管道抽取。保留 16 格缓冲和堵塞保护。
- 修复 ME 注魔祭坛加工状态翻译。手动加工的自动输出送往配置面相邻容器，AE 加工订单结果返回网络，网络满时保留产物。
- 扩展原生树苗识别及真实树干掉落，支持已验证的神秘学异界树苗和新生魔艺至高树苗。
- 可选读取沉浸工程园艺玻璃罩与 MekMM 种植机配方。常规成熟作物和树木沿用内置识别与掉落，特殊生成或收割行为需具体适配。
- 优化多作物满配阵列的服务端掉落估算与库存查询，保留固定产物和耗电。随机掉落采用采样估算，短期波动有所平滑。
- 客户端与服务器需同步更新。仍处于持续测试和平衡阶段。

## English

- Fix Shift + right-click acceleration-card insertion for the ME Infusion Altar and ME Biological Cultivator, including either hand and full-slot protection.
- Fix seed inventory counts incorrectly appearing as 9.2e when used with Cell4. Fully restart the game or server to clear the previous runtime cache.
- Make output ports extraction-only externally, with immediate automatic delivery through the patterned face and pipe extraction from every face. Keep the 16-slot buffer and blocked-output protection.
- Fix infusion status localization. Manual processing auto-output targets configured adjacent inventories, while AE crafting results return to the network and remain buffered when storage is full.
- Extend native sapling and trunk-loot support, including verified Occultism otherworld saplings and Ars Nouveau archwood saplings.
- Optionally import Immersive Engineering cloche and MekMM planting recipes. Common crops and trees use native recognition and loot. Special generation or harvesting behavior requires specific integration.
- Optimize server-side harvest estimates and inventory queries for fully upgraded arrays with many crops. Fixed yields and energy consumption are preserved. Sampled random yields have smoother short-term variation.
- Update both client and server. Testing and balance work continue.
