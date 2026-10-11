# 更新日志

## 0.1.0（2026-10-11）

首个版本：以 Summy Reliquary 为前置的扩展包框架，以及第一个栏位与饰品。

### 新增

- **工程框架**：Forge 1.20.1 / Java 17 / 官方映射 / Gradle 8.7，modId `summy_reliquary_apocrypha`，
  命名空间 `summy-reliquary-apocrypha`；`mods.toml` 强制依赖 Forge / Minecraft / **Summy Reliquary** / Curios；
  `build` 结束后产物自动复制到 `../JAR/summy-reliquary-apocrypha/`。
- **新栏位「新约-光环」**（`new_testament_halo`，order 15，1 格）：面板里排在 SR「光环」之后，
  栏位图标由作者提供；目前只接受「新约」。
- **饰品「新约」**（`new_testament`，装入「新约-光环」）：
  - 门槛＝**天使标记**（复用 SR 的 `CurioItemSupport`，未达标时装不上、也看不到功能描述）；
  - 弓 / 弩的箭矢命中目标后，自身回复 0.5 点生命；
  - 35% 概率在目标碰撞箱顶部上方 3 格处散射落下 3~5 支箭矢（散布角度 0~25°）；
  - 落箭固定 5 点**真实伤害**（独立伤害类型 `new_testament_arrow`，四个 `bypasses_*` 标签全开），
    结算前清零无敌帧，因此无视无敌帧；
  - 落箭以玩家为造成者、落箭实体为直接来源 → 自动享受 SR 的七罪 / 七德与「全伤害最终倍率」乘区；
  - 落箭是 owner 为玩家的 `AbstractArrow` → 自动获得 SR「圣心」的箭矢追踪；
  - 散射判定成功时给佩戴者本人播一次箭矢命中音效（`SoundEvents.ARROW_HIT`，只给本人听）。
- 全部数值进配置：`[new_testament] enable / heal_amount / scatter_chance_percent /
  scatter_arrow_min / scatter_arrow_max / scatter_spawn_height / scatter_angle_max_degrees /
  scatter_initial_speed / arrow_damage`。

### 备注

- 本轮**不新增网络包**：新约的全部逻辑都在服务端，落箭实体用原版 `AbstractArrow` 自带的同步。
- 「新约」的物品贴图本轮**借原版贴图占位**（`minecraft:item/arrow`），模组图标临时复用 SR 的图标。
- 本轮不改动前置模组 Summy Reliquary 的任何代码。
