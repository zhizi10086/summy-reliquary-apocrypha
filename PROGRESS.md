# Summy Reliquary: Apocrypha — 进度与设定

> 本文档是**交接与复查用的一页纸**：项目坐标、系列设定、数值口径、配置面、已完成进度、待办、准备转正与验收流程。
> 玩法细节的完整说明见 [README](README.md)，逐轮改动见 [CHANGELOG](CHANGELOG.md)。

## 一、项目概况

| 项目 | 值 |
| --- | --- |
| 名称 | Summy Reliquary: Apocrypha（SRA） |
| 定位 | [Summy Reliquary](https://github.com/zhizi10086/summy-reliquary)（SR）的扩展包，**SR 为硬前置** |
| 平台 | Minecraft 1.20.1 / Forge 47.4.10（开发）/ Java 17 / 官方映射 / Gradle 8.7 |
| 依赖 | Forge + Minecraft + `summy_reliquary` + `curios`（后两者 `ordering=AFTER`） |
| 坐标 | modId `summy_reliquary_apocrypha`；命名空间 `summy-reliquary-apocrypha`；包 `com.summy.reliquary.apocrypha` |
| 版本 | `0.1.0`（沿用同名覆盖，不随轮次升版） |
| 源码 | `C:\Users\52527\Documents\ChatGPT\MC mod\summy-reliquary-apocrypha`（git，origin = `zhizi10086/summy-reliquary-apocrypha`） |
| 产物 | 构建后自动复制到 `C:\Users\52527\Documents\ChatGPT\MC mod\JAR\summy-reliquary-apocrypha\` |
| 测试实例 | `E:\MC\PCL2快照版\.minecraft\versions\1.20.1-Forge_47.4.23-test`（**jar 需手动拷进 `mods`**） |

## 二、系列设定：新约系列（5 件，已全部实装）

全部**需要天使标记**（复用 SR 的 `CurioItemSupport.canEquipInto`，未达标时装不上、也看不到功能描述）；
对应栏位**仅天使线开放**（有标记 1 格、否则 0 格，每秒调用 SR 的 `SlotSizing.sync` 动态校正，缩格时先退物）。

| # | 栏位 id（order） | 饰品 id | 效果要点 |
| --- | --- | --- | --- |
| 1 | `new_testament_halo`（15） | `new_testament`「新约」 | 弓/弩箭矢命中：回血 0.5；35% 落箭散射（见下节） |
| 2 | `new_testament_talent`（16） | `swift_feather_covenant`「迅羽·协约」 | 与「新约」同戴时改写落箭：概率 → 50%、初速 +50%、伤害 → 触发箭最终伤害的 40% |
| 3 | `new_testament_grace`（17） | `wing_feather_sacrament`「翼羽·圣餐」 | 每次治疗触发生成一对光羽；攒到第 4 对后射击消耗 1 对 → **圣餐之矢**（本体箭 +30%、必定触发新约、落箭固定 5 支） |
| 4 | `new_testament_blessing`（18） | `inverse_feather_hover`「逆羽·滞空」 | 下落中：本体箭 +15%、触发概率 ×1.5、落箭伤害**瞬时结算**；被动缓降（潜行键）与二段跳（跳跃键） |
| 5 | `new_testament_revelation`（19） | `paired_wings`「比翼·连携」 | 20% 追加**穿透圣光矢**；与「新约」同戴时两者**永远协同触发** |

物品图标**由作者提供并已入库**（`textures/item/<物品id>.png`，32×32 原图；`models/item/*.json` 的 `layer0` 指向它们）；
**创造页图标用「比翼·连携」**。栏位图标同样由作者提供（16×16 PNG）。

## 三、核心机制与数值口径（当前定稿）

### 3.1 本体箭矢命中（`LivingHurtEvent`，LOWEST）

判定：`AbstractArrow` + owner 是玩家 + 目标不是自己；**排除** SR 的幻影矛 / 金刀片，以及本模组的落箭与圣光矢
（避免重复触发与递归）。两件各自掷点（各自佩戴各自掷、各自消耗一次随机数），**两件都没戴则直接返回**。

- **新约触发器**：回血 0.5（每次命中都回，与是否触发无关）；概率 35%（同戴迅羽 50%；逆羽下落中 ×1.5 → 52.5% / 75%）；
  圣餐之矢必定触发。触发时给本人播一次箭矢命中音效。
- **比翼触发器**：20% 追加穿透圣光矢。

### 3.2 落箭（`new_testament_arrow`）

- 生成：目标碰撞箱顶部上方 8 格（撞方块则夹紧到「命中点 −0.5 格」，不低于「目标顶部 +0.5 格」），
  散射角 0~25°、初速 1.2 格/tick、每 2 tick 错峰放一支（3~5 支；圣餐之矢固定 5 支）。
- **伤害固定、钉住**：默认 `arrow_damage` = **5 点**，**不吃 SR 的任何增伤乘区**（钉住机制：命中时在 LOWEST 把金额改回箭上携带值）；
  **同戴迅羽**时改为「触发之箭最终伤害的 40%」，同样钉住。
- 真伤口径：伤害类型 `summy-reliquary-apocrypha:new_testament_arrow`，四个 `bypasses_*` 标签全开（无视护甲/保护/抗性/无敌帧），
  命中前后各清一次无敌帧，**不造成击退**（结算前后保存/还原目标速度），命中方块贴地 10 tick 后消失、寿命兜底 200 tick。
- 撞到方块时会**对齐到撞击点**（沿来向回退 0.05 格）：我们自己冻结了速度、不再走原版的位置推进，若不补这一步，
  箭会停在"撞击前"的位置、看起来悬在地面上方约一格（调试日志会打「落箭撞方块 / 落箭贴地」两行，可用于核对）。
- **落箭无视射手本人**（`canHitEntity` 对 owner 恒 false）：站在落点区里时箭会穿过你、照常落地，而不是在半空被丢弃；
  静默丢弃路径（撞到射手 / 撞到非生物）在 `log_trigger` 打开时会打一行「落箭丢弃」留痕；上述三条日志都只在**服务端**记录（避免客户端副本重复记账）。
- 联动：owner 是玩家的 `AbstractArrow` → 会被 SR「圣心」追踪牵引（既有口径，不改 SR）。
- 逆羽下落中改用**瞬时结算**：触发那一刻按箭数逐笔结算，箭矢只作视觉（`visualOnly`）。

### 3.3 穿透圣光矢（`holy_arrow`，与 SR 的「圣光」无关）

- 从玩家眼位沿**准心**射出、初速 3.0 格/tick（等同满蓄力箭矢）、**无重力、无限穿透生物**（同一目标只吃一次），
  撞方块贴地 10 tick 后消失、寿命兜底 200 tick；外观是原版**光灵箭**（`ArrowRenderer` + spectral_arrow 贴图，不加自绘拖尾）。
- **伤害 = 触发之箭「各类倍率后」金额 × 100%**（配置 `holy_arrow_damage_ratio_percent`），**钉住**、不再吃第二次 SR 乘区；
  真伤类型 `summy-reliquary-apocrypha:holy_arrow`（四个 `bypasses_*` 全开）+ 命中前后各清无敌帧 + 无击退。
- 退出 ER 的箭矢粒子接管：`data/enchantment_reforged/tags/entity_types/no_arrow_particles.json`。

### 3.4 协同触发（仅「新约」+「比翼·连携」同戴）

| 新约侧掷点 | 圣光矢侧掷点 | 结果 |
| --- | --- | --- |
| 成功 | 成功 | 立刻发 **2 道**圣光矢（第 2 道约 3° 水平偏角）+ 落箭 **箭数 +1** |
| 成功 | 失败 | 落箭照常立刻放；圣光矢**延后 2 tick（0.1 秒）**补发 1 道 |
| 失败 | 成功 | 圣光矢立刻发 1 道；落箭**延后 2 tick**补发 |
| 失败 | 失败 | 无事发生 |

被强制带出的那一次**不再掷点、也不计 +1**；圣餐之矢视为新约侧自然成功；圣光矢自身不触发新约（不回血、不递归），
也不会被误标成圣餐之矢。延迟补发由 `effect/PairedWings` 的两个小队列承载（射手离线/换维度丢弃、停止服务器清空）。

### 3.5 光羽（翼羽·圣餐）

- 每次「新约治疗触发」+1 对；**存储上限 4 对（8 片）**，片数 **> 6** 时射击才消耗 1 对（打空也消耗、不退还）。
- **显现门槛：攒满 3 对（6 片）时才出现**；1、2 对时完全不显示，跌破 3 对立刻消失。
- 外观与跟随：贴图是**完整的单边翅膀**，渲染**不做旋转**、只用**左右镜像**构成一对；四边形 1.25 格见方、铺满整张画布
  （保住贴图 20:30 的自然比例）；中间缝隙 **0.20 格**（常量 `WING_GAP` 直接填目标值，内移量自动反推）；翼根基准在背中段约 0.65 格（叠上 ±0.05 浮动后约 0.60~0.70）、翼尖约 1.86 格，带轻微上下浮动。
  高度靠 `WING_ROOT_DROP` 控制：相对 **body 骨骼枢轴（肩线，约 1.5 格）向下 0.85 格** —— 该骨骼是 `PartPose.offset(0,0,0)`，
  且其空间里 **+y 向下**（`world_y = 1.501 − y_local`），别按"腰部枢轴"去算。
  **渲染方式照 SR「邦邦咔邦光环」**：Curios 的 `ICurioRenderer` 画在玩家模型的躯干骨骼上，**零延迟跟随**（含潜行前倾/骑乘偏移），**没有实体**；
  显隐由「翼羽·圣餐」堆栈 NBT 上的标志决定（Curios 每 tick 自动同步给客户端）。
- 以最后一次治疗触发起算，**脱战 10 秒后每 2 秒熄一对**；卸下饰品只隐藏光翼、计数保留。

### 3.6 逆羽·滞空

- **下落中（不在地面且 vy < 0）**：本体箭 +15%（与圣餐之矢的 +30% 同轴相加，同发 ×1.45）、触发概率 ×1.5、落箭瞬时结算。
- **缓降**：空中长按潜行键，下落速度与坠落高度按 −75% 折减（落地伤害 ≈ 25%）。
- **二段跳**：空中按跳跃键向上一段（初速 `air_jump_velocity` 0.5，约 1.5 格），每段离地 1 次、落地或攀爬重置；
  跳的瞬间清空 `fallDistance`；做法是 Artifacts 云朵瓶式的**客户端预测 + 服务端权威双端施加**，
  客户端要求「离地后松开过跳跃键」以免吃掉地面起跳那一次按键。首条网络通道 `summy-reliquary-apocrypha:main`（协议 `"1"`）只承载这一个空 C2S 包。

### 3.7 SR 真伤「命中后清帧」旁路（不改 SR）

SR 的 10 个真伤类型（排除物理的 `golden_razor`）在本模组侧补上「命中后清无敌帧」：
Forge 上就地清零、Kilt/Connector 上登记后在 server tick 末尾清零；开关 `[general] clear_sr_iframes_after_hit`（默认 true）。
SR 若新增真伤类型，打开调试日志时会看到「清帧名单可能已漂移」提醒（名单在 `integration/ReliquaryRef`）。

### 3.8 调试日志（默认关闭）

`[new_testament] log_trigger` 打开后输出 `[新约]` 行：触发行（掷点/阈值/箭数）、圣光矢掷点行、协同行、
落箭起点行（实际高度）、**落箭命中行 / 圣光矢命中行**（结算值 / 命中前血量 / 三段速度 / 击退被抵消）、
落箭瞬时结算汇总行、二段跳行、清帧行。关闭时零输出。口径文本：落箭 =「钉住：N 点（不吃 SR 增伤乘区）」，
圣光矢 =「钉住：触发箭伤害的 100%」。

## 四、配置面（`config/summy_reliquary_apocrypha-common.toml`，共 25 项）

| 段 | 键 | 默认 | 说明 |
| --- | --- | --- | --- |
| `[general]` | `enable` | true | 玩法总开关 |
| | `clear_sr_iframes_after_hit` | true | 是否给 SR 真伤补「命中后清帧」（独立于 enable） |
| `[new_testament]` | `heal_amount` | 0.5 | 命中回血 |
| | `scatter_chance_percent` | 35 | 落箭触发概率（%） |
| | `scatter_arrow_min` / `scatter_arrow_max` | 3 / 5 | 落箭数量区间 |
| | `scatter_spawn_height` | 8.0 | 生成高度（格） |
| | `scatter_angle_max_degrees` | 25.0 | 散布角上限 |
| | `scatter_initial_speed` | 1.2 | 落箭初速（格/tick） |
| | `arrow_damage` | 5.0 | 落箭**固定伤害**（钉住，不吃 SR 乘区） |
| | `log_trigger` | false | 调试日志开关 |
| `[swift_feather_covenant]` | `chance_percent` | 50 | 迅羽档触发概率 |
| | `speed_bonus_percent` | 50 | 落箭初速加成 |
| | `damage_ratio_percent` | 40 | 落箭伤害 = 触发箭最终伤害的 %（钉住） |
| `[wing_feather_sacrament]` | `decay_delay_seconds` | 10 | 脱战多久开始熄羽 |
| | `decay_interval_seconds` | 2 | 熄灭节奏 |
| | `damage_bonus_percent` | 30 | 圣餐之矢本体箭伤害加成 |
| | `arrow_count` | 5 | 圣餐之矢落箭数量（固定） |
| `[inverse_feather_hover]` | `falling_damage_bonus_percent` | 15 | 下落中本体箭伤害加成 |
| | `falling_chance_percent` | 150 | 下落中触发概率倍率（×1.5） |
| | `glide_reduction_percent` | 75 | 缓降折减（下落速度与坠落高度） |
| | `air_jumps` | 1 | 每段离地的额外跳跃次数 |
| | `air_jump_velocity` | 0.5 | 额外跳跃向上初速（格/tick） |
| `[paired_wings]` | `holy_arrow_chance_percent` | 20 | 圣光矢触发概率 |
| | `holy_arrow_damage_ratio_percent` | 100 | 圣光矢伤害 = 触发箭伤害的 %（钉住） |

**写死的实现细节**（不进配置）：错峰间隔 2 tick、天花板夹紧余量/下限 0.5 格、贴地停留 10 tick、寿命 200 tick、
光羽显示/存储上限与层高角度尺寸、下落判定阈值 0、圣光矢初速 3.0 与协同延迟 2 tick、共同触发 +1 道/+1 支、第二道偏角 3°。

## 五、代码与资源清单

```
Apocrypha.java                     注册物品/实体/创造页/配置/网络；挂事件总线
ApocryphaClient.java               客户端渲染器注册
client/ApocryphaClientInput.java   二段跳输入状态机（FORGE 总线 + Dist.CLIENT）
client/NewTestamentArrowRenderer   落箭渲染（原版箭贴图）
client/HolyArrowRenderer           圣光矢渲染（光灵箭贴图）
client/WingFeatherSacramentRenderer 光翼渲染层（Curios，挂在玩家模型躯干骨骼上）
config/ApocryphaConfig             25 项配置
effect/NewTestamentEffect          命中判定 / 掷点 / 协同 / 落箭调度 / 清帧旁路 / 日志
effect/ScatterVolley               落箭待落批次（错峰 + 延迟 + 天花板夹紧）
effect/PairedWings                 圣光矢发射与协同补发队列
effect/SacramentArrows             圣餐之矢（放箭登记 → 指定箭 → 本体箭增伤）
effect/FeatherCharge               光羽计数、脱战衰减、实体校正
effect/HoverFlight                 逆羽·滞空（缓降 / 二段跳 / 落阶状态）
effect/ArrowDamageBonuses          箭矢增伤（圣餐 +30% 与滞空 +15%，HIGHEST，排在 SR 乘区之前）
effect/TrueDamageFrames            SR 真伤命中后清帧旁路
effect/ApocryphaSlotSizing         新约系列栏位的动态格数（每秒）
entity/NewTestamentArrow           落箭实体（钉住 / 视觉标记 / 无击退）
entity/HolyArrow                   穿透圣光矢实体
item/*                             5 件饰品（门槛 + 提示）
integration/ReliquaryRef           对 SR 的唯一访问出口（真伤名单、Kilt/Forge 探测）
net/*                              网络通道（协议 "1"）与 AirJumpMessage
slot/ApocryphaSlots                栏位 id 常量、天使线列表、校验谓词
```

数据与资源：5 个栏位 json + `curios/entities/player.json`（挂 5 栏位）、2 个伤害类型 + 4 个 `bypasses_*` 标签追加、
1 个 ER 实体标签、5 张栏位图标（16×16）、5 个物品模型 json、中英语言文件。

## 六、进度

**已完成（按提交顺序）**：框架 + 栏位体系 → #1 新约（散射观感调优 / 配置收敛 / 去击退 / SR 真伤清帧旁路 / 调试日志）
→ #2 迅羽·协约 → #3 翼羽·圣餐 + 光羽实体（含层号同步与跟随修复）→ #4 逆羽·滞空 + 首条网络通道
→ 二段跳改为云朵瓶式双端预测 → #5 比翼·连携 + 穿透圣光矢（系列收官）
→ 伤害口径调整（落箭固定伤害、圣光矢 100%）→ 日志前缀修正。

**当前状态**：新约系列 5 件全部实装并通过实机验收（概率、箭数、起点高度、钉住值、协同交叉账目、无击退、二段跳均以日志核对过）；
jar 版本 0.1.0，最新提交见仓库 `main` 分支。

## 七、待办与已知问题

| 项 | 状态 / 说明 |
| --- | --- |
| 光羽渲染观感 | 已照 SR「邦邦咔邦光环」改成**饰品渲染层**（零延迟跟随 + 无旋转仅镜像 + 满 3 对才显现）；高度/间隙两个常量待实机复检微调 |
| 物品图标 | **已完成**：5 件物品贴图由作者提供并入库（`textures/item/<id>.png`，32×32），创造页图标改用「比翼·连携」；`扩展包` 里的源文件已全部用掉，无遗留未命名素材 |
| SR 真伤清帧 | 只在 Forge 47.4.23 实例验证过；**Kilt/Connector 分支未实测**（代码路径已实现）。**转正方案见 8.1** |
| 清帧名单漂移 | SR 若新增真伤类型需登记进 `ReliquaryRef`，调试日志会给提醒 |
| 圣心追踪 | 落箭与圣光矢都会**被 SR 圣心牵引**（owner 为玩家的 `AbstractArrow`），按既有口径接受；要排除须改 SR |
| SR 圣光联动 | 玩家另戴 SR「圣光」时，本模组箭矢命中仍会按 SR 规则掷点出光柱（SR 的规则是"任何玩家命中"） |
| `onSacramentHit` 扩展点 | 仍为空实现；#3 预留的「圣餐之矢同时触发圣光矢」已由协同规则覆盖，无需再接 |
| 版本号 | 一直沿用 0.1.0 同名覆盖；正式发布前可考虑升版 |
| 中文/英文 | 语言文件成对维护，改占位符时注意中英顺序一致（历史上踩过一次坑） |

## 八、准备转正（正式化前待办）

> 「转正」= 把只是权宜 / 旁路的做法改成**正式做法**。本节只登记**已经决定要做、尚未做**的事；
> 做完即从本节移除，并把结论补进 `CHANGELOG.md` 与相关小节（`七` 的清单保持原样，不搬动）。

### 8.1 SR 真伤「命中后清帧」由旁路转为 SR 原生（未排期）

- **来源**：2026-10-11 指定 —— 现在这件事以「SRA 侧旁路兜底」的方式实现，正式化的方向是**由 SR 本体负责**。
- **现状**：SRA 侧由 `effect/TrueDamageFrames` 在 `LivingHurtEvent` 里按名单识别 SR 的真伤并补上「命中后清帧」
  （Forge 上就地清零；Kilt/Connector 上登记后在 server tick 末尾清零，因为那边的事件时机早于原版写 `invulnerableTime`），
  名单在 `integration/ReliquaryRef.TRUE_DAMAGE_MSG_IDS`（SR 的 11 个伤害类型里 10 个是真伤，`golden_razor` 是物理伤害被排除；
  `godhead_aura` 与 `holy_light` 共用同一个 `message_id`，去重后是 9 个 id），开关是 `[general] clear_sr_iframes_after_hit`
  （默认 true，独立于 `enable`）。SR 本体目前只在命中**前**清帧（`RevelationBeam.applyTrueDamage` 里写 `target.invulnerableTime = 0`），
  命中后仍留 5 tick 保护帧。
- **改法**（SR 侧做，转正后 SRA 删旁路）：
  1. `effect/HolyLightEffect` 增一个统一的真伤判据（`isDivine || isExactDamage`，再补上 `sacrifice_self`；
     `golden_razor` 不参与、`godhead_aura` / `holy_light` 共用 `message_id` 的去重在同一处解决）——
     这两份名单本来就是「真伤类型」的事实来源，集中成单一入口后，SR 以后新增真伤类型不会再出现「SRA 名单漂移」。
  2. 真伤**落地后**再清一次无敌帧；Kilt/Connector 侧沿用 SR 1.6.5 起已有的 tick 末尾钳位通道
     （否则会被原版在 hurt 开头写入的 `invulnerableTime = 20` 覆盖）。
  3. 新增 SR 侧配置键（暂定 `[general] clear_iframes_after_true_damage`，默认 true），语义与 SRA 现在的开关一致。
  4. SRA 侧删掉 `effect/TrueDamageFrames`、`integration/ReliquaryRef` 的真伤名单与同名配置键；
     README「清帧补强」与本档 3.7 改成「由 SR 本体负责」，并给旧配置键留一条迁移说明。
- **涉及文件**：SR `effect/HolyLightEffect.java`、`ReliquaryEvents.java`、`effect/DamagePools.java`、`ReliquaryConfig.java`；
  SRA `effect/TrueDamageFrames.java`、`integration/ReliquaryRef.java`、`config/ApocryphaConfig.java`、README / PROGRESS / CHANGELOG。
- **验收**：① 只装 SR（不装 SRA）时，10 种真伤连续命中同一目标**每次全额结算**（不再被 5 tick 保护帧吞），
  普通近战 / 原版箭矢 / 金刀片仍保留原版无敌帧语义；② Forge 与 Kilt/Connector 两侧行为一致；
  ③ 关掉该配置即恢复原行为；④ SRA 删旁路后，落箭与圣光矢的清帧不受影响（那是 SRA 自己的伤害类型）。

## 九、开发与验收流程

1. **构建**：在源码目录执行 `gradlew build`（需 JDK 17，路径走全局 `~/.gradle/gradle.properties`）；
   产物自动复制到 `..\JAR\summy-reliquary-apocrypha\`。
2. **冒烟**：`gradlew runServer`（`run/mods` 里会自动投放 SR 的 dev 映射 jar）；
   期望 `Loaded 22 curio slots`（SRA 占 5 个）、配置 25 项、零 ERROR / 异常。
3. **实机**：把 jar 拷进实例 `mods`（同名覆盖）→ 启动 → 用 `/summyreliquary angel grant` 取得天使标记。
4. **验收**：先在实例配置里把 `[new_testament] log_trigger` 改成 `true`，按动作打一遍，再把
   `E:\MC\PCL2快照版\.minecraft\versions\1.20.1-Forge_47.4.23-test\logs\latest.log` 交给验收方核对，
   核对后把开关改回 `false`（代码默认值始终是 `false`）。
5. **同步**：`git add -A` → commit（提交信息避免直引号）→ 推送前先 `git ls-remote` 确认 Watt Toolkit 加速生效 → `git push`。

> 约定：**不改 SR / ER 仓库**；SR 与 `libs/` 里的前置 jar 都不随轮次变动；`libs/` 必须入库。
