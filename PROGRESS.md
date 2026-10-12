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

### 恶魔线：凋亡系列（5 件，第 1~4 件已实装）

与天使线对称：全部**需要恶魔标记**（走 `CurioItemSupport.canEquipInto` 的自定义门槛重载，第 5 个参数传
`PlayerFlags::isDemon` +「需要恶魔标记」），对应栏位**仅恶魔线开放**（与天使线一样每秒 `SlotSizing.sync` 动态 0/1 格，
列表在 `ApocryphaSlots.DEMON_SERIES_SLOTS`）。**素材按约定先用占位**：栏位图标按顺序借新约套
（提喻←光环、释经←才能、弥撒←恩典、悲悯←祝福、终歌←启示），物品贴图借原版
（堕天=`wither_rose`、坠羽=`phantom_membrane`、笞心=`heart_of_the_sea`、枯笔=`ink_sac`、终歌=`goat_horn`）。

> 贴图占位的**踩坑记录**：凋灵玫瑰在 1.20.1 只有**方块**贴图，模型的 `layer0` 必须写
> `minecraft:block/wither_rose`；写 `minecraft:item/wither_rose` 会渲染成黑紫块（客户端日志会有一条
> `Missing textures in model ...`）。其余四件都是标准物品贴图，写 `minecraft:item/<id>` 即可。
> 「坠羽」爆条时展开的翅膀**暂时复用光羽贴图**（`light_feather.png`），等恶魔线美术到位再换。

| # | 栏位 id（order） | 饰品 id | 效果要点 | 状态 |
| --- | --- | --- | --- | --- |
| 1 | `perdition_synecdoche`（20） | `fallen_heaven`「堕天」 | 近战直击推「凋亡损伤条」（每次 +1/6 条）；满条**爆条**（30 点元素伤害）→ 目标进入**损伤状态**（受伤附加 10% 元素伤害、治疗 −50%、条在 5 秒内线性回退到 0）→ **损伤抵抗** 5 秒 | **已实装** |
| 2 | `perdition_exegesis`（21） | `fallen_plume`「坠羽」 | 4 刀满（每次 +1/4 条）；爆伤 = 本轮**推条那几刀伤害之和 × 60%**；损伤状态附加 20%；爆条时引爆者背后展开翅膀 5 秒（刷新） | **已实装** |
| 3 | `perdition_mass`（22） | `scourged_heart`「笞心」 | 最终爆伤 ×1.5、附加比例 ×1.5；**斩杀**：目标受伤后生命 ≤15% 时补一击「最大生命 100%」元素伤害，每段损伤状态只落地一次、且只由佩戴笞心者的伤害激活 | **已实装** |
| 4 | `perdition_mercy`（23） | `withered_brush`「枯笔」 | 近战直击「损伤抵抗中」的目标 → 抵抗 −1 秒（减到 0 立即结束）；引爆时目标获 1 秒恐惧（**无法移动 + 无法攻击**；玩家档默认关，指令可开） | **已实装** |
| 5 | `perdition_requiem`（24） | `final_song`「终歌」 | 损伤状态中的目标每秒受「爆伤 × 5%」的元素伤害；减疗 50% → 80%；受到所有伤害 +10% | 待做 |

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

### 3.8 凋亡（恶魔线核心机制，`effect/Apoptosis`）

- **推条**：`LivingHurtEvent`（LOWEST）里要求「**直接来源是 `ServerPlayer`**」+ 该玩家佩戴「堕天」**或**「坠羽」，
  且目标**既不在损伤状态、也不在损伤抵抗**；每次 +`bar_max / 刀数`（堕天 6 刀 ≈ 16.67 / 坠羽 4 刀 = 25）。
  **弓箭 / 投掷物 / 落箭 / 宠物天然不算**（直接来源不是玩家本人），本模组自己的元素伤害也被排除（防递归）。
  每次推条同时把该刀的 `LivingHurtEvent` 金额累加进 `apoptosis_push_damage`（坠羽的爆伤口径要用）。
- **爆条**：推条值触及 `bar_max`（判定留 1e-6 浮点余量以兜住累加误差）→ 造成**元素伤害**；
  条**置满**并进入**损伤状态** `injured_seconds`（5 秒），期间条按同一条速率**线性回退到 0**。
  爆伤口径与附加比例都在这一刻由**引爆者**的佩戴决定并写进目标数据（`apoptosis_extra_percent`），
  同时 `apoptosis_push_damage` 清零：戴坠羽 → 本轮推条伤害之和 × `burst_ratio_with_second_percent`（60%）；
  只戴堕天 → 固定 `burst_damage`（30）；戴笞心 → 上面结果再 × `burst_multiplier_with_third`（1.5）、
  附加比例再 × `extra_multiplier_with_third`（1.5）。
- **A 口径（关键）**：**损伤状态与损伤抵抗期间都不累计、也不能再次爆条** —— 一次循环固定为
  「攒满 → 爆条 → 5 秒损伤状态 → 5 秒抵抗 → 从零重新攒」，即一次爆条后 10 秒内同一目标不会再爆。
- **损伤状态**：目标每次受伤额外附加「该次伤害 × 附加比例」的元素伤害（钉住、不推条）；
  比例取目标数据里的 `apoptosis_extra_percent`（堕天 10% / 坠羽 20%），读不到才退回配置默认；
  治疗经 `LivingHealEvent` 静默缩放 `(1 − heal_reduction_percent%)`（50%）。
- **损伤抵抗**：条归 0 后 `resistance_seconds`（5 秒）内不累积推条，到期自动清除。
- **时间口径（读档自愈）**：损伤状态与抵抗都存**到期时刻**（目标所在关卡的 `getGameTime()`）而不是倒计时，
  所以中途退游戏 / 长时间离线都不会留下卡死的残留；旧键（`*_ticks`）不再读取 —— 旧存档等价于「状态已结束」。
  损伤状态期间的条值按剩余时间**派生**（不再逐 tick 写盘）；每次受伤若发现目标还带着未过期的状态或未清空的进度，
  会顺手把它重新纳入 `ACTIVE`（读档后头顶条会在 5 tick 内恢复显示）。
- **空闲清空**：累积阶段若连续 `idle_clear_seconds`（30）秒没被推过条（`apoptosis_last_push`），
  进度值与已累计的推条伤害一起清零；损伤 / 抵抗期（合计 10 秒）不会触发。
- **斩杀（笞心）**：`LivingHurtEvent`（LOWEST）里、结算完附加伤害之后判定 —— 只有「本次伤害的**造成者**是佩戴笞心的玩家」
  （近战 / 弓箭 / 落箭都算）才激活；目标处于损伤状态且**预计落地后血量 ≤ 最大生命 × 15%**（且 > 0）时，
  补一击「最大生命 × 100%」的元素伤害（钉住、无击退、算在该佩戴者头上）；**每段损伤状态只落地一次**
  （`apoptosis_execute_used`，爆条时重置），抵抗与累积期不判定。
- **抵抗削减（枯笔）**：与推条同口径 —— 只有**近战直击**（直接来源是玩家本人）且该玩家戴枯笔、目标处于抵抗期时，
  把 `apoptosis_resist_until` 前移 `resistance_cut_seconds_with_fourth`（1）秒；每刀最多一次、可叠加，减到 0 即抵抗立刻结束。
- **恐惧（枯笔）**：爆条那一刻若引爆者戴枯笔 → `effect/WitheredBrush` 给目标 1 秒恐惧（`fear_seconds_with_fourth`）：
  **生物**每 tick（服务器 tick 末尾）清攻击目标 / 中立仇恨 + 停寻路 + 水平速度清零（垂直分量保留）；**玩家**（`fear_affects_players`，
  默认 false，指令热切换）挂两条 `MULTIPLY_TOTAL −1` 的 transient 修饰符（移动速度、攻速归零）并拦 `AttackEntityEvent` 与
  `PlayerInteractEvent.RightClickItem/RightClickBlock`；到期 / 死亡 / 登出 / 停机都会摘掉修饰符。
- **坠羽的爆条翅膀**：爆条且引爆者戴坠羽时，`effect/FallenPlumeWings` 给其坠羽堆栈写 NBT 标志
  （`ApocryphaDemonWings`，与光羽的 `ApocryphaWings` 分开）+ 记剩余 tick（`wing_seconds` × 20），
  每 tick 递减、到期清标志；登出 / 停机立刻清。客户端由同一套 Curios 渲染层读取，尺寸 1.35 / 背后偏移 0.42（与光翼错开）。
- **元素伤害（`apoptosis_element`）**：四个 `bypasses_*` 标签全开（无视护甲 / 保护附魔 / 抗性 / 效果），
  命中前后各清一次无敌帧、结算前后保存并还原目标速度（**无击退**）；金额**钉住** ——
  在 LOWEST 把事件金额改回自己发出的固定值，因此**不吃 SR 的七罪七德与全伤害乘区**，
  但"伤害减免"类效果（事件链上把金额改小的）仍照常生效。
  伤害源**直接来源留空、造成者＝玩家**（这样 SR 不会把它当成玩家近战命中去做圣光等掷点）。
- **不吃闪避**：该类型挂在 `#enchantment_reforged:no_dodge` 标签里（SRA 提供标签内容、ER 侧读该标签判断），
  因此 ER 的「灵动步伐」不会闪避它；未装 ER 时该文件无副作用。
- **两套显示**：目标头顶条是自定义 billboard 渲染 —— 正对相机（含俯仰）、与名称牌一致**不穿墙**、
  平滑填充 + 5 条刻度竖线切出 6 段，三段配色（累积 `#7A1220` / 损伤 `#E0203A` 脉动 / 抵抗 `#5A6B7A` 显示倒计时）；
  受害者是**玩家**时改用原版 **Boss 条**（`NOTCHED_6`，紫→红→白，名称带百分比 / 剩余秒），零自定义渲染。
- **网络**：新增 S2C 包 `ApoptosisBarMessage`（协议从 `"1"` 升到 **`"2"`**），只在阶段切换与 5 tick 节流点补发，
  客户端收到后自己逐 tick 衰减；退出世界清缓存。
- **数据**：损伤值 / 两项到期时刻 / 最近推条时刻 / 附加比例 / 斩杀标记都写在目标实体的持久数据（SRA 根键）里，
  随存档保存；计时只遍历"活跃目标"登记表（不做全实体扫描），退服 / 死亡 / 登出 / 换维度各自清理 Boss 条。

### 3.9 调试日志（默认关闭）

`[new_testament] log_trigger` 打开后输出 `[新约]` 行：触发行（掷点/阈值/箭数）、圣光矢掷点行、协同行、
落箭起点行（实际高度）、**落箭命中行 / 圣光矢命中行**（结算值 / 命中前血量 / 三段速度 / 击退被抵消）、
落箭瞬时结算汇总行、二段跳行、清帧行。关闭时零输出。口径文本：落箭 =「钉住：N 点（不吃 SR 增伤乘区）」，
圣光矢 =「钉住：触发箭伤害的 100%」；恶魔线另输出 `[凋亡]` 行（爆条、元素伤害（爆条 / 附加），含发出值与钉住后值）。

## 四、配置面（`config/summy_reliquary_apocrypha-common.toml`，共 44 项）

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
| `[apoptosis]` | `bar_max` | 100.0 | 凋亡损伤条总点数（满值触发爆条） |
| | `hits_base` | 6 | 打满损伤条需要的近战次数（每次 +`bar_max / 该值`） |
| | `burst_damage` | 30.0 | 爆条的元素伤害（点） |
| | `extra_damage_percent` | 10.0 | 损伤状态期间受伤额外附加的元素伤害（%） |
| | `heal_reduction_percent` | 50.0 | 损伤状态期间的治疗削减（%） |
| | `injured_seconds` | 5 | 损伤状态持续秒数（条在此期间线性回退到 0） |
| | `resistance_seconds` | 5 | 损伤抵抗持续秒数（期间不再累积） |
| | `hits_with_second` | 4 | 佩戴坠羽时打满损伤条所需的近战次数 |
| | `burst_ratio_with_second_percent` | 60.0 | 佩戴坠羽时爆伤 = 本轮推条伤害之和 × 该值（%） |
| | `extra_damage_percent_with_second` | 20.0 | 佩戴坠羽时损伤状态的附加伤害比例（%） |
| | `wing_seconds` | 5 | 坠羽爆条时翅膀的持续秒数（刷新） |
| | `idle_clear_seconds` | 30 | 连续多少秒没被推过条就清空损伤进度（0 = 关闭） |
| | `execute_threshold_percent` | 15.0 | 笞心斩杀的血量阈值（%） |
| | `execute_damage_percent_of_max` | 100.0 | 斩杀伤害 = 目标最大生命 × 该比例（%） |
| | `burst_multiplier_with_third` | 1.5 | 佩戴笞心时最终爆条伤害的乘数 |
| | `extra_multiplier_with_third` | 1.5 | 佩戴笞心时附加比例的乘数 |
| | `resistance_cut_seconds_with_fourth` | 1 | 佩戴枯笔时每次近战直击削减的抵抗秒数 |
| | `fear_seconds_with_fourth` | 1 | 佩戴枯笔引爆时目标的恐惧秒数 |
| | `fear_affects_players` | false | 恐惧是否也对玩家生效（指令热切换） |

**写死的实现细节**（不进配置）：错峰间隔 2 tick、天花板夹紧余量/下限 0.5 格、贴地停留 10 tick、寿命 200 tick、
光羽显示/存储上限与层高角度尺寸、下落判定阈值 0、圣光矢初速 3.0 与协同延迟 2 tick、共同触发 +1 道/+1 支、第二道偏角 3°、
凋亡条尺寸与位置（0.90 × 0.10 格、碰撞箱顶部 +0.55 格）、5 条刻度切 6 段、三段配色与脉动（0.5 秒周期）、
抵抗期倒计时细条、玩家侧 Boss 条（`NOTCHED_6`）、同步节流 5 tick、恶魔翅膀的尺寸与背后偏移（1.35 / 0.42 格）。

**5 件那轮要加的键**（已定稿、实现时追加）：每秒 5%、减疗 80%、受伤 +10%。
（2 号件四键、3 号件五键、4 号件三键都已落地。）

## 五、代码与资源清单

```
Apocrypha.java                     注册物品/实体/创造页/配置/网络；挂事件总线
ApocryphaClient.java               客户端渲染器注册
client/ApocryphaClientInput.java   二段跳输入状态机（FORGE 总线 + Dist.CLIENT）
client/NewTestamentArrowRenderer   落箭渲染（原版箭贴图）
client/HolyArrowRenderer           圣光矢渲染（光灵箭贴图）
client/WingFeatherSacramentRenderer 光翼 / 恶魔翅膀渲染层（Curios，挂在玩家模型躯干骨骼上；按物品切参数与标志）
client/ApoptosisBarRenderer        凋亡损伤头顶条（billboard、不穿墙、纯代码）
client/ApoptosisBarState           凋亡条的客户端缓存（本地逐 tick 衰减）
config/ApocryphaConfig             44 项配置
command/ApocryphaCommand           本模组指令（/summyapocrypha fear players on|off）
effect/Apoptosis                   凋亡核心：推条 / 爆条 / 损伤状态 / 抵抗 / 元素伤害钉住 / 玩家 Boss 条
effect/FallenPlumeWings            「坠羽」爆条时背后展开的翅膀（写饰品 NBT 标志 + 到期清标志）
effect/WitheredBrush               「枯笔」的恐惧（生物冻结 / 玩家定身与封禁近战·右击）
effect/NewTestamentEffect          命中判定 / 掷点 / 协同 / 落箭调度 / 清帧旁路 / 日志
effect/ScatterVolley               落箭待落批次（错峰 + 延迟 + 天花板夹紧）
effect/PairedWings                 圣光矢发射与协同补发队列
effect/SacramentArrows             圣餐之矢（放箭登记 → 指定箭 → 本体箭增伤）
effect/FeatherCharge               光羽计数、脱战衰减、实体校正
effect/HoverFlight                 逆羽·滞空（缓降 / 二段跳 / 落阶状态）
effect/ArrowDamageBonuses          箭矢增伤（圣餐 +30% 与滞空 +15%，HIGHEST，排在 SR 乘区之前）
effect/TrueDamageFrames            SR 真伤命中后清帧旁路
effect/ApocryphaSlotSizing         新约系列与凋亡系列栏位的动态格数（每秒）
entity/NewTestamentArrow           落箭实体（钉住 / 视觉标记 / 无击退）
entity/HolyArrow                   穿透圣光矢实体
item/*                             9 件饰品（门槛 + 提示；含恶魔线「堕天」「坠羽」「笞心」「枯笔」）
integration/ReliquaryRef           对 SR 的唯一访问出口（真伤名单、Kilt/Forge 探测）
net/*                              网络通道（协议 "2"）：AirJumpMessage（C2S）+ ApoptosisBarMessage（S2C）
slot/ApocryphaSlots                栏位 id 常量、天使线与恶魔线列表、校验谓词
```

数据与资源：9 个栏位 json + `curios/entities/player.json`（挂 9 栏位）、3 个伤害类型 + 4 个 `bypasses_*` 标签追加、
2 个 ER 标签（实体标签 `no_arrow_particles` + 伤害类型标签 `no_dodge`）、9 张栏位图标（16×16）、9 个物品模型 json、中英语言文件。

## 六、进度

**已完成（按提交顺序）**：框架 + 栏位体系 → #1 新约（散射观感调优 / 配置收敛 / 去击退 / SR 真伤清帧旁路 / 调试日志）
→ #2 迅羽·协约 → #3 翼羽·圣餐 + 光羽实体（含层号同步与跟随修复）→ #4 逆羽·滞空 + 首条网络通道
→ 二段跳改为云朵瓶式双端预测 → #5 比翼·连携 + 穿透圣光矢（系列收官）
→ 伤害口径调整（落箭固定伤害、圣光矢 100%）→ 日志前缀修正 → 光翼改饰品渲染层（含多轮微调）
→ 天使线 5 件素材正式入库 → **恶魔线第一轮：凋亡核心机制 + 栏位「殁亡之提喻」+ 饰品「堕天」**
→ **恶魔线第二轮：A 口径（损伤状态不再累计）+ 贴图占位修复 + 饰品「坠羽」（4 刀 / 比例爆伤 / 附加 20% / 爆条展翅）**。
→ **恶魔线第三轮：状态改绝对时间（读档自愈）+ 2 号件比例降到 60% + 30 秒空闲清空 + 饰品「笞心」（系数 ×1.5 + 斩杀）**。
→ **恶魔线第四轮：日志补全（空闲清空 / 斩杀判定 / 抵抗削减 / 恐惧）+ 饰品「枯笔」（抵抗削减 + 恐惧）与首条指令**。

**当前状态**：新约系列 5 件全部实装并通过实机验收（概率、箭数、起点高度、钉住值、协同交叉账目、无击退、二段跳均以日志核对过）；
恶魔线第 1~4 件与核心机制已实现（待实机验收）；ER 侧同时加了 `#enchantment_reforged:no_dodge` 伤害类型标签判断（版本号不升，单独提交）。
jar 版本 0.1.0，最新提交见仓库 `main` 分支。

## 七、待办与已知问题

| 项 | 状态 / 说明 |
| --- | --- |
| 光羽渲染观感 | 已照 SR「邦邦咔邦光环」改成**饰品渲染层**（零延迟跟随 + 无旋转仅镜像 + 满 3 对才显现）；高度/间隙两个常量待实机复检微调 |
| 物品图标 | **已完成**：5 件物品贴图由作者提供并入库（`textures/item/<id>.png`，32×32），创造页图标改用「比翼·连携」；`扩展包` 里的源文件已全部用掉，无遗留未命名素材 |
| 凋亡 5 件 | **待做**：效果已定稿（见「二」的恶魔线表）——终歌（损伤状态每秒 5% 爆伤 + 减疗 80% + 受伤 +10%） |
| 恶魔线素材 | **待作者提供**：当前栏位图标借新约套（提喻←光环、释经←才能、弥撒←恩典、悲悯←祝福、终歌←启示）、物品贴图借原版（`block/wither_rose` / `item/phantom_membrane` / `item/heart_of_the_sea` / `item/ink_sac` / `item/goat_horn`）、坠羽的翅膀贴图暂用光羽 `light_feather.png` |
| 凋亡头顶条美术（可选） | 现在是纯代码绘制、已可用；若作者要换美术，按 **64×16 PNG**（上行＝底槽、下行＝满填充、左右各 1px 描边）提供，代码按 `u ∈ [0, 比例]` 裁切 |
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
   期望 `Loaded 26 curio slots`（SRA 占 9 个）、配置 44 项、零 ERROR / 异常。
3. **实机**：把 jar 拷进实例 `mods`（同名覆盖）→ 启动 → 用 `/summyreliquary angel grant` / `demon grant` 取得对应标记。
4. **验收**：先在实例配置里把 `[new_testament] log_trigger` 改成 `true`，按动作打一遍，再把
   `E:\MC\PCL2快照版\.minecraft\versions\1.20.1-Forge_47.4.23-test\logs\latest.log` 交给验收方核对，
   核对后把开关改回 `false`（代码默认值始终是 `false`）。
5. **同步**：`git add -A` → commit（提交信息避免直引号）→ 推送前先 `git ls-remote` 确认 Watt Toolkit 加速生效 → `git push`。

> 约定：**不改 SR / ER 仓库**；SR 与 `libs/` 里的前置 jar 都不随轮次变动；`libs/` 必须入库。
