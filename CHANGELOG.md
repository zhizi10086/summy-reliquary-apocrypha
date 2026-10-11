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
  - 35% 概率在目标碰撞箱顶部上方 8 格处落下 3~5 支箭矢（散布角度 0~25°），**每支间隔 2 tick 错峰出现**；
  - 落箭固定 5 点**真实伤害**（独立伤害类型 `new_testament_arrow`，四个 `bypasses_*` 标签全开）；
  - 落箭以玩家为造成者、落箭实体为直接来源 → 自动享受 SR 的七罪 / 七德与「全伤害最终倍率」乘区；
  - 落箭是 owner 为玩家的 `AbstractArrow` → 自动获得 SR「圣心」的箭矢追踪；
  - 散射判定成功时给佩戴者本人播一次箭矢命中音效（`SoundEvents.ARROW_HIT`，只给本人听）。

### 散射与清帧调整（同日第二轮）

- **散射观感**：生成高度 3 → **8**、初速 0.3 → **1.2**（角度仍 0~25°）。落点半径从平均 0.47 格 / 最远 1.01 格
  提到**平均 1.81 格 / p90 3.23 / 最远 3.55**（2 万次采样、无圣心）；角度下限保持 0°，因此每批仍有 1~2 支
  近乎垂直落在中心，戴圣心时仍会被追踪收拢 —— 两者均为已确认接受的取舍。
- **错峰生成**：触发时只登记一条「待落」批次，之后每 2 tick 放出一支（`scatter_spawn_interval_ticks`，0 = 同 tick 全出），
  从「5 支同时砸下来」变成连珠落箭；射手离线/换维度则作废该批剩余，队列上限 256 条，停止服务器时清空。
- **天花板夹紧**：每支箭生成前向上探一次，撞到方块就把高度压到「命中点 − 0.5 格」且不低于「目标顶部 + 0.5 格」
  （`scatter_ceiling_margin` / `scatter_min_height`）；露天时与配置完全一致。洞穴里不再整批失效。
- **落箭清帧补强**：命中前清一次（保证不被吞），`hurt()` 真的落地后再清一次（不把无敌帧留给同批其它落箭与
  随后的近战/箭矢）；只在真正打中时清。
- **SR 真伤的「命中后清帧」（纯本模组侧旁路，**不改 SR**）**：SR 的启示之光 / 圣光 / 圣光爆发 / 神性光环 /
  亚巴顿光环 / 恶魔之焰 / 狱火 / 契约献祭 / 献祭自伤 原本只在命中前清一次，命中后仍会留下 5 tick 级保护帧；
  本模组在 `LivingHurtEvent` 里按名单识别它们，Forge 上就地清零、Kilt/Connector 上登记后在 server tick 末尾
  清零（Kilt 的事件时机在原版写字段之前）。名单（9 个 `message_id`）登记在 `ReliquaryRef`，排除物理伤害
  `golden_razor`；漏登记时会在调试日志里给出「名单可能已漂移」提醒。开关 `[general] clear_sr_iframes_after_hit`（默认 true）。
- **落箭数值全部配置化**：新增 `scatter_spawn_interval_ticks` / `scatter_ceiling_margin` / `scatter_min_height` /
  `arrow_ground_stick_ticks` / `arrow_max_life_ticks`；实体里原来的贴地停留与寿命常量改为运行时读配置。
- **调试日志开关**：新增 `[new_testament] log_trigger`（默认 false）。打开后每次命中掷点、每批起点（含被夹紧后的
  实际高度）、每支落箭命中（金额为各类倍率结算后的值）与每次清帧各记一行 `[新约]` INFO；关闭时零输出、零行为差异。

### 配置

`config/summy_reliquary_apocrypha-common.toml`：

| 键 | 默认 |
| --- | --- |
| `[general] enable` | true |
| `[general] clear_sr_iframes_after_hit` | true |
| `[new_testament] heal_amount` | 0.5 |
| `scatter_chance_percent` | 35 |
| `scatter_arrow_min` / `scatter_arrow_max` | 3 / 5 |
| `scatter_spawn_height` | 8.0 |
| `scatter_angle_max_degrees` | 25.0 |
| `scatter_initial_speed` | 1.2 |
| `arrow_damage` | 5.0 |
| `log_trigger` | false |

### 配置面收敛与落箭去击退（同日第三轮）

- **「落箭命中」行补上速度证据**：这行改由落箭实体在 `hurt()` 落地、且击退已被还原之后输出，
  因此能同时给出「速度 命中前 → 击退后 → 还原后」与「击退被抵消」的标量模长 ——
  前两段一致 + 后者非 0 就是「无击退」的日志证据（金额仍取 `LivingHurtEvent`（LOWEST）里的各类倍率结算后值，
  经一个仅供日志的静态字段传给落箭实体；不新增配置键、不改任何玩法判定）。
- **配置面 16 → 11 项**：把 5 个内部实现细节从配置回调为代码常量 —— 错峰间隔 2 tick（`ScatterVolley.SPAWN_INTERVAL_TICKS`）、
  天花板夹紧余量 0.5 格、夹紧下限 0.5 格（`ScatterVolley` 私有常量）、命中方块后贴地停留 10 tick、寿命兜底 200 tick
  （`NewTestamentArrow` 私有常量）。行为完全不变；旧配置文件里这几个键会在下次启动时被 Forge 清掉。
  物品提示行也同步去掉了「间隔 N tick」那半句（中英两份语言文件与传参同步改为 5 个占位符）。
- **落箭不再击退目标**：原版 `LivingEntity#hurt` 会按 `DamageSource.getEntity()`（也就是我们的造成者「玩家」）施加
  0.4 强度击退，并附带水平速度减半与一点上浮 —— 每支落箭都会推一次，把目标推出后续落箭的落点。现在照 SR 神性光环
  的同一手法在结算前后保存 / 恢复目标速度，并在恢复后置 `hurtMarked = true` 让客户端同步（只在 `hurt()` 真的落地时恢复）。

### 备注

- 本轮**不新增网络包**：新约的全部逻辑都在服务端，落箭实体用原版 `AbstractArrow` 自带的同步。
- 「新约」的物品贴图本轮**借原版贴图占位**（`minecraft:item/arrow`），模组图标临时复用 SR 的图标。
- 本轮不改动前置模组 Summy Reliquary 的任何代码、版本号与 `libs/` 里的前置 jar：SR 真伤的「命中后清帧」完全由
  本模组旁路实现。
- 已知限制：真伤不再给受害者留 5 tick 保护帧（含玩家被真伤命中时）；落箭仍会造成击退（未改动）。
- **配置迁移提醒**：Forge 会保留已存在的配置项，所以从上一版升级上来的 `config/summy_reliquary_apocrypha-common.toml`
  里 `scatter_spawn_height` / `scatter_initial_speed` 仍是旧值（3.0 / 0.3），需要删掉配置文件重新生成，
  或手动改成 8.0 / 1.2 —— 新默认值只对新建的配置文件生效。
