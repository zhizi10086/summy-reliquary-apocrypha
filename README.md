# Summy Reliquary: Apocrypha

> Summy Reliquary 的扩展包 · Minecraft 1.20.1 Forge

**前置：** [Summy Reliquary](https://github.com/zhizi10086/summy-reliquary)（必需）｜ Curios 5.14.1+1.20.1（必需）｜
Forge 1.20.1（推荐 47.4.10+）

[CHANGELOG](CHANGELOG.md) | [CREDITS](CREDITS.md) | [LICENSE](LICENSE)

## 内容

| 名称 | 类型 | 说明 |
| --- | --- | --- |
| 新约-光环 | Curios 栏位 | 新约系列 #1 的栏位（order 15，面板里排在 SR「光环」之后），只接受「新约」 |
| 新约-才能 | Curios 栏位 | 新约系列 #2 的栏位（order 16），只接受「迅羽·协约」 |
| 新约-恩典 | Curios 栏位 | 新约系列 #3 的栏位（order 17），只接受「翼羽·圣餐」 |
| 新约-祝福 | Curios 栏位 | 新约系列 #4 的栏位（order 18），只接受「逆羽·滞空」 |
| 新约 | 饰品（新约-光环） | **需要天使标记**才能佩戴与查阅。弓 / 弩的箭矢命中目标后：自身回复 0.5 点生命；有 35% 概率在目标碰撞箱顶部上方 8 格处**错峰落下** 3~5 支箭矢（每支间隔 2 tick、散布 0~25°），落箭固定 **5 点真实伤害**（无视护甲 / 保护附魔 / 抗性 / 无敌帧，命中前后各清一次无敌帧），可享受 Summy Reliquary 的全伤害增幅，并受 SR「圣心」的箭矢追踪牵引；散射触发时给佩戴者本人播一次箭矢命中音效 |
| 迅羽·协约 | 饰品（新约-才能） | **需要天使标记**。本身不给任何属性，只在**与「新约」同戴**时改写其落箭参数：触发概率 35% → **50%**、落箭初速 **+50%**、落箭伤害由固定 5 点改为**触发之箭最终伤害的 40%**（该值**钉住**、不再叠加任何增伤乘区）；落箭仍是真实伤害、无视无敌帧、不击退 |
| 翼羽·圣餐 | 饰品（新约-恩典） | **需要天使标记**，与「新约」同戴时生效：**每次治疗触发**在背后凝聚一对光羽（自定义实体渲染、沿背部逐层顺时针 45° 展开；最多显示 3 对、实际可存 4 对）；攒到**第 4 对**后射出箭矢会消耗 1 对，使该箭成为**圣餐之矢**（伤害 **+30%**、**必定触发新约**、落箭**固定 5 支**，且伤害口径仍按是否同戴迅羽决定）；**脱战 10 秒后每 2 秒**熄一对 |
| 逆羽·滞空 | 饰品（新约-祝福） | **需要天使标记**。处于**下落状态**（不在地面且正在下降）时：箭矢伤害 **+15%**（与圣餐之矢的 +30% **同轴相加**，同发即 ×1.45）、新约触发概率 **×1.5**（35% → 52.5%，同戴迅羽 50% → 75%）、落箭伤害改为在触发那一刻**按箭数逐笔瞬时结算**（落箭实体照常落下但只作视觉）。被动：空中长按**潜行键**缓降（下落速度 −75%，落地按「坠落高度 −75%」计伤害）；空中按**跳跃键**可额外跳 1 次（落地重置） |

> **新约系列**（共 5 件，栏位与素材均已就位）：**新约-光环 / 新约-才能 / 新约-恩典 / 新约-祝福 / 新约-启示**（`扩展包` 目录里已有对应 16×16 栏位图标）。全部**需要天使标记**，对应栏位**仅天使线开放**（有天使标记才出现 1 格，否则 0 格 —— 由本模组每秒调用 SR 的 `SlotSizing.sync` 动态校正，缩格时先把格内物品退回背包、满了掉脚下）；物品图标在作者统一给出前一律**用原版贴图占位**（目前：新约 = 箭、迅羽·协约 = 羽毛、翼羽·圣餐 = 鞘翅、逆羽·滞空 = 幻翼膜）。

## 散射观感

- **落点宽度**：高度 8 格 + 初速 1.2 的组合下，落点半径平均约 **1.8 格**、最远约 3.6 格（旧参数是平均 0.47 / 最远 1.0，几乎砸在同一点）。
- **角度下限仍为 0°**：所以每批箭里总有 1~2 支近乎垂直落在目标正上方 —— 这是保留「0~25° 均匀散布」的必然结果，不是 bug。
- **戴圣心时会被收拢**：SR 的追踪每 tick 把箭的方向朝最近的敌对生物拉一半，落点会重新并到约 0.1~0.3 格。要么接受这个联动（默认），要么把落箭排除追踪（需要改 SR，本轮没做）。
- **头顶空间**：生成高度会在每次生成前向上探一次，撞到方块就压到「命中点 − 0.5 格」，且不低于「目标顶部 + 0.5 格」（这两个余量是写死的实现细节）；露天时与配置高度完全一致。洞穴里高度会被压低、落点自然更集中 —— 想看明显的散射请在露天或净空 ≥ 5 格处射；净空不足 3 格时基本等同于原位砸落。
- **不击退**：落箭命中**不会把目标推走**（结算前后把目标速度原样恢复，连原版击退附带的水平减速与上浮一起抵消），避免目标被推出后续落箭的落点。
- **装了「迅羽·协约」更利落**：落箭初速 ×1.5（1.2 → 1.8）。下落时间随之变短、横向积累时间也变短，所以落点半径基本维持原样（约 1.8 格），差别是整批箭落得更快。

## 光羽（翼羽·圣餐）

- **计数**：佩戴「新约 + 翼羽·圣餐」时，**每一次**弓 / 弩箭矢命中（也就是每次「新约治疗触发」，含没掷中落箭的那几次）都会在背后凝聚 **1 对**（= 2 片）。
- **上限与消耗**：显示上限 **6 片（3 对）**、存储上限 **8 片（4 对）**；只有片数 **> 6（即攒到第 4 对）** 时射击才会消耗 1 对，把那一发变成**圣餐之矢**（打空也消耗、不退还）。
- **圣餐之矢**：伤害 **+30%**（在 SR 的增伤乘区之前放大，即"箭矢伤害本身 +30%"）、**必定触发新约**（跳过概率判定）、落箭数量**固定 5 支**；伤害口径仍按是否同戴迅羽决定（同戴 → 触发箭最终伤害的 40% 钉住）。
- **渲染**：每对一个自定义实体，沿背部上下排布、**逐层顺时针 45° 展开**（上对朝上外 / 中对水平外张 / 下对朝下外），每对的左右两片互为镜像；平面固定在玩家背后（随朝向旋转，侧面看是薄的），带轻微上下浮动，别人也能看到。
- **熄灭**：以最后一次治疗触发为起点，脱战 **10 秒**后每 **2 秒**熄 1 对（先熄下对）；卸下饰品只隐藏实体、计数保留。

## 滞空（逆羽·滞空）

- **判定时点**：三项增益**都在箭矢命中的那一刻**判定（与 35% / 50% 掷点同一时刻，口径统一、便于解释）——
  射手当时「不在地面且垂直速度 < 0」才算处于下落状态，**站地面射箭走的是原来的物理命中路径**。
- **瞬时结算**：下落中触发时，伤害在**触发这一刻**就对命中目标逐笔结算 N 笔（N = 本批箭数，3~5 或圣餐之矢的固定 5 支），
  每笔口径与物理落箭完全一致（同一真实伤害类型、命中前后各清一次无敌帧、无击退的速度还原）。
  实现上先**就地生成一支落箭当直接来源**，用它的 `DamageSource` 结算 —— 这样 SR 仍按「远程」口径处理，
  迅羽档的钉住值也照旧生效。箭矢本身照常错峰落下来，但打了 `visualOnly` 标记，命中实体时直接消失、不再结算伤害。
- **缓降**：空中**长按潜行键**时，每 tick 把垂直速度压到 25%（水平速度不动），并把当 tick 的下降量按同一比例
  从 `fallDistance` 里扣掉 —— 因此落地伤害按**「坠落高度 −75%」**计算（不是完全免伤）。
- **额外跳跃**：空中按**跳跃键**向上弹一次（初速 0.42，写死），**每段离地只能用一次**，落地即重置。
  原版的跳跃键状态存在 protected 字段里、服务端读不到，所以这是本模组**首条网络通道**（`summy-reliquary-apocrypha:main`，
  协议 `"1"`）唯一的 C2S 包：客户端做按下沿检测、只在佩戴该饰品时上报；服务端收到后再校验佩戴、是否离地、本段是否还有剩余跳数。

## 清帧补强

- **落箭本体**：命中前清一次（保证这一箭不被吞），`hurt()` 真的落地后再清一次（不把无敌帧留给同批其它落箭与随后落下的近战/箭矢）。只在真正打中时清。
- **SR 的真伤**（启示之光 / 圣光 / 圣光爆发 / 神性光环 / 亚巴顿光环 / 恶魔之焰 / 狱火 / 契约献祭 / 献祭自伤）：由本模组**旁路兜底**补上「命中后清帧」，**不改动 SR 仓库**。做法是在 `LivingHurtEvent` 里按 SR 的伤害类型名单识别，Forge 上就地清零、Kilt/Connector 上登记后在 server tick 末尾清零（Kilt 的事件时机在原版写字段之前，就地写会被覆盖）。
- **名单**：SR 的 11 个伤害类型里 10 个是真伤；`golden_razor`（金刀片，物理）被排除。`godhead_aura` 与 `holy_light` 共用同一个 `message_id`，去重后是 9 个 id，登记在 `integration/ReliquaryRef.java`。SR 若新增真伤类型，打开调试日志时会看到「清帧名单可能已漂移」的提醒。
- **开关**：`[general] clear_sr_iframes_after_hit`（默认 true）。关掉即恢复 SR 原本「只在命中前清帧」的行为。
- **代价**：真伤不再给受害者留 5 tick 保护帧（含玩家被真伤命中时），同 tick 内随后落下的普通伤害也会照常结算。
- **普通攻击不受影响**：近战、原版箭矢、金刀片等仍按原版无敌帧语义处理。

## 配置

文件：`config/summy_reliquary_apocrypha-common.toml`（改完 Forge 会热加载，无需重启）。

| 键 | 默认 | 说明 |
| --- | --- | --- |
| `[general] enable` | true | 玩法总开关 |
| `[general] clear_sr_iframes_after_hit` | true | 是否给 SR 真伤补「命中后清帧」 |
| `[new_testament] heal_amount` | 0.5 | 弓 / 弩命中后的回血 |
| `scatter_chance_percent` | 35 | 触发落箭散射的概率（%） |
| `scatter_arrow_min` / `scatter_arrow_max` | 3 / 5 | 每批落箭数量区间 |
| `scatter_spawn_height` | 8.0 | 生成高度（相对目标碰撞箱顶部，格） |
| `scatter_angle_max_degrees` | 25.0 | 散布角上限（竖直向下为 0°） |
| `scatter_initial_speed` | 1.2 | 落箭初速（格/tick） |
| `arrow_damage` | 5.0 | 落箭固定伤害（真实伤害） |
| `log_trigger` | false | 调试日志开关（见下） |
| `[swift_feather_covenant] chance_percent` | 50 | 迅羽档的落箭触发概率（替换基础 35%） |
| `speed_bonus_percent` | 50 | 迅羽档的落箭初速加成（%） |
| `damage_ratio_percent` | 40 | 迅羽档的落箭伤害系数（触发之箭金额的 %，钉住不吃乘区） |
| `[wing_feather_sacrament] decay_delay_seconds` | 10 | 脱战多少秒后开始熄灭光羽 |
| `decay_interval_seconds` | 2 | 开始熄灭后每隔多少秒熄 1 对 |
| `damage_bonus_percent` | 30 | 圣餐之矢的伤害加成（%，在 SR 乘区之前放大） |
| `arrow_count` | 5 | 圣餐之矢的落箭数量（固定值） |
| `[inverse_feather_hover] falling_damage_bonus_percent` | 15 | 下落状态下的箭矢伤害加成（%，与圣餐之矢同轴相加） |
| `falling_chance_percent` | 150 | 下落状态下新约触发概率的倍率（150 = 生效基础值 ×1.5） |
| `glide_reduction_percent` | 75 | 缓降：下落速度与坠落高度的折减（%，75 = 只按 25% 计） |
| `air_jumps` | 1 | 每段离地可用的额外跳跃次数（落地重置） |

以上 22 项就是全部配置面。**错峰间隔 2 tick；天花板夹紧余量 0.5 格、夹紧下限 0.5 格；命中方块后贴地停留 10 tick、寿命兜底 200 tick；光羽的显示上限 6 片 / 存储上限 8 片 / 消耗门槛 >6 片；光羽的层高、角度、尺寸、跟随延迟；空中跳的初速 0.42、下落判定的速度阈值 0** 都是写死的实现细节（没人会去调，留在配置里只是噪音），不再出现在配置文件中。

**注意 `arrow_damage` 是「基准值」，不是最终数值**：落箭以玩家为造成者出伤，因此会吃 Summy Reliquary 的乘区。实测（戴着美德＝全赎罪档）的构成是
`5 × 谦逊 1.15 × 思想套装 1.1 ×（圣心 30% + 神性 20%）1.5 × 暴怒随机 0.5~2.0` → **单支实际约 4.7 ~ 19 浮动**（实测 5.09~18.89 与之吻合）。
其中「暴怒随机」按档位逐击掷点：全赎罪档 `wrath_random_max_redeemed = 2.0`，未赎罪档为 `1.5`。若希望落箭恒定不波动，需要另开一轮处理（把落箭排除在 SR 出伤管线外需改 SR；在 SRA 侧自算固定乘区则会与 SR 版本漂移）。

### 调试日志

把 `[new_testament] log_trigger` 改成 `true` 后，日志里会出现这几类 `[新约]` 行（默认关闭时零输出）：

```
[新约] {玩家} 箭矢命中 {目标}：掷点 12.3（阈值 35.0）→ 未触发；回血 0.50
[新约] {玩家} 箭矢命中 {目标}：掷点 68.1（阈值 35.0）→ 触发，落箭 4 支（间隔 2 tick）；回血 0.50
[新约] {玩家} 箭矢命中 {目标}：掷点 68.1（阈值 52.5）→ 触发，落箭 4 支（间隔 2 tick）；回血 0.50   ← 逆羽·滞空的下落中命中
[新约] 落箭起点：实际高度 8.00（配置 8.00）、中心 277.8/-55.2/7.8
[新约] 落箭命中 {目标}：结算 5.00 点（各类倍率后）、命中前血量 20.0/20.0、速度 命中前(0.00,0.00,0.00) → 击退后(-0.31,0.40,-0.12) → 还原后(0.00,0.00,0.00)、击退被抵消 0.522
[新约] 落箭瞬时结算：{目标} ×4 笔、合计 20.0 点（口径：各类倍率后）   ← 逆羽·滞空：逐笔当场结算，取代上面那行“落箭命中”
[新约] 逆羽·滞空：{玩家} 空中跳跃 1/1
[新约] 清帧：来源 summy-reliquary.holy_light、受害者 {目标}、通道 Forge
```

- **阈值**会随佩戴与状态变化：基础 35、同戴迅羽 50、下落中 ×1.5 即 52.5 / 75；圣餐之矢那行写「必定触发」。
- **下落中触发的伤害是「瞬时结算」一行汇总**（×N 笔、合计多少点），落箭实体只作视觉、不再各打一行。
- 「结算」取的是各类倍率结算后的金额 —— 戴圣心（全伤害最终倍率 +30%）时应看到约 6.5（=5×1.3）。
- 「实际高度」用来核对天花板夹紧；洞穴里应小于配置值。
- **命中行是在击退被还原之后才打的**（原版击退发生在 `hurt()` 内部、我们的还原在其之后），所以「速度 命中前 → 还原后」两段应当**完全一致**，而「击退被抵消」是非 0 值 —— 这两点合起来就是「无击退」的日志证据。
- 一次性筛选：`Select-String -Pattern '\[新约\]' run\logs\latest.log`

## 前置与依赖

- **Summy Reliquary**：本模组的所有玩法耦合都走它的公开 API（`CurioItemSupport` 的天使门槛与读档放行、
  `ReliquaryTooltips` 的提示格式、`CurioHelper` 的佩戴判定、`DamagePools.fabricSide()`、真实伤害与增伤乘区）。
- **Curios**：栏位与栏位面板（SR 本身也硬依赖它）。
- 开发运行期：`gradlew runClient` 会自动把 `libs/` 里的 SR jar 反转成 dev 映射并投放到 `run/mods`。

## 开发

- 构建：`gradlew build`（需要 Java 17），产物会自动复制到 `../JAR/summy-reliquary-apocrypha/`
- clone 后请先把 `gradle.properties` 里的 `org.gradle.java.home` 指向本机 JDK 17
  （仓库里那一行是注释示例，不填会用系统默认 JDK，可能不是 17 而构建失败）
- 运行：`gradlew runClient` / `gradlew runServer`
- 本地测试用指令：`/summyreliquary angel grant`（来自 SR，用于拿到佩戴「新约」所需的天使标记）

### 目录

```
src/main/java/com/summy/reliquary/apocrypha/   本模组源码（包名挂在 SR 的 group 下，便于对照前置）
  Apocrypha.java            主入口：注册物品 / 创造页 / 实体 / 配置 / 网络通道
  ApocryphaClient.java      客户端入口：实体渲染器
  client/ApocryphaClientInput.java  客户端输入上报（逆羽·滞空的空中跳跃按下沿检测）
  config/                   配置（config/summy_reliquary_apocrypha-common.toml）
  effect/NewTestamentEffect 命中判定、掷点、音效、落箭调度
  effect/ScatterVolley      待落批次：错峰放出 + 天花板夹紧
  effect/ArrowDamageBonuses 箭矢伤害加成（圣餐 +30% 与滞空 +15% 同轴相加，排在 SR 乘区之前）
  effect/SacramentArrows    圣餐之矢：放箭登记 → 给箭打标记 → 放大伤害
  effect/FeatherCharge      光羽计数、脱战衰减与实体校正
  effect/HoverFlight        逆羽·滞空的缓降、额外跳跃与下落判定
  effect/TrueDamageFrames   SR 真伤的「命中后清帧」旁路
  entity/                   落箭实体、光羽实体
  integration/ReliquaryRef  **对 SR 的唯一访问出口**（含 SR 真伤名单与 Kilt/Forge 探测）
  item/                     各件饰品（新约 / 迅羽·协约 / 翼羽·圣餐 / 逆羽·滞空）
  net/                      网络通道（协议 "1"：逆羽·滞空的空中跳跃 C2S 包）
  slot/                     栏位校验谓词
src/main/resources/
  META-INF/mods.toml        模组元数据与前置声明
  assets/summy-reliquary-apocrypha/   图标、语言文件、物品模型、栏位贴图
  data/summy-reliquary-apocrypha/     栏位定义、玩家栏位表、伤害类型
  data/minecraft/tags/damage_type/    把本模组的真实伤害类型加进原版减免标签
libs/                       前置模组 Summy Reliquary 的生产 jar（**必须入库**）
```

## 如何扩展（接入 SR 的内容）

1. **新增饰品 / 物品**：在 `Apocrypha.java` 里加 `ITEMS.register(...)`，并在创造页的
   `displayItems` 里追加；物品贴图放 `assets/summy-reliquary-apocrypha/textures/item/`。
2. **新增栏位**：在 `data/summy-reliquary-apocrypha/curios/slots/<id>.json` 定义栏位，
   把 id 追加进 `data/summy-reliquary-apocrypha/curios/entities/player.json` 的 `slots`，
   并在 `slot/ApocryphaSlots.java` 注册校验谓词（`CuriosApi.registerCurioPredicate`）。
3. **用 SR 的门槛 / 提示**：佩戴门槛直接用 `CurioItemSupport.canEquipInto(context, stack, 栏位id, true)`
   （第 4 个参数 = 需要天使标记），提示行用 `ReliquaryTooltips.add / statComponent / angelLocked /
   appendAngelHint / shiftHint`；名字按派系上色用 `SinTexts.factionName(key, ReliquaryFaction.ANGEL)`。
4. **真实伤害（新约定）**：在 `data/summy-reliquary-apocrypha/damage_type/` 加类型文件，并把类型 id 追加进
   `data/minecraft/tags/damage_type/bypasses_{armor,effects,enchantments,resistance}.json`；
   落地时**命中前清一次无敌帧**（保证不被吞）、`hurt()` 返回 true 时**再清一次**（真伤不留帧）。
5. **想要 SR 的增伤**：不要自己复刻倍率。把伤害源的**造成者**设为玩家、**直接来源**设为投掷物实体，
   并且不把伤害类型写进 SR 的 `isDivine` / `isExactDamage` 名单 —— SR 的 `LivingHurtEvent`
   会自动叠加七罪 / 七德与「全伤害最终倍率」乘区。
6. **给 SR 接入新实体**：SR 的圣心追踪筛选的是「`AbstractArrow` + owner 是该玩家」，
   所以只要你的投掷物继承 `AbstractArrow` 且 owner 设为玩家，就会自动获得追踪（不需要任何代码）；
   反之若不想被追踪，得在 SR 侧加排除（本工程的原则是**不改 SR**）。
7. **新增「无视无敌帧」的 SR 真伤类型**：如果 SR 将来加了新的真伤类型，把它登记进
   `ReliquaryRef.TRUE_DAMAGE_MSG_IDS`，旁路清帧才会覆盖到它（漏登记时调试日志会提醒）。
8. **新增网络包**（需要把客户端输入送到服务端时）：在 `net/ApocryphaNetworking` 里按顺序追加
   `CHANNEL.registerMessage(序号, ...)` 并同步升协议版本；客户端发送侧照 `client/ApocryphaClientInput`
   的写法（FORGE 总线 + `Dist.CLIENT`）做**按下沿检测**，服务端在 handler 里再做一次完整校验。

## 更新前置 jar（SR 出新版本时）

1. 把 SR 产物 `../JAR/summy-reliquary/summy-reliquary-<版本>.jar` 拷进本工程的 `libs/`
   （文件名保持 `summy-reliquary-<版本>.jar`）；
2. 改 `gradle.properties` 的 `summy_reliquary_version=<版本>`；
3. `gradlew build` 通过即可（`compileOnly` 与 `run/mods` 的那份都会跟着这个版本号走）。

## 排查

- **启动就报缺少前置**：`run/mods` 里没有 SR（`gradlew runClient` 会自动投放；直接用 IDE 启动时要手动放）。
- **装上饰品却查不到功能描述**：没有天使标记（`/summyreliquary angel grant`）。
- **更新到本轮后落箭还是老样子（落点依旧集中）**：Forge 会**保留已存在**的配置值，旧配置文件里的
  `scatter_spawn_height`（3.0）与 `scatter_initial_speed`（0.3）不会自动变成新默认值。
  删掉 `config\summy_reliquary_apocrypha-common.toml` 让它重新生成，或手动把这两个值改成 8.0 / 1.2。
- **落箭没伤害**：确认 `data/summy-reliquary-apocrypha/damage_type/new_testament_arrow.json` 与四个
  `data/minecraft/tags/damage_type/bypasses_*.json` 都在 jar 里（被打包时丢文件会退回原版魔法伤害源）。
- **落箭看着还是集中**：确认 `scatter_spawn_height` / `scatter_initial_speed` 没被改回小值；洞穴里会被
  天花板压回（看调试日志的「落箭起点」行）；戴圣心时会被追踪收拢（预期）。
- **真伤还是被无敌帧吞**：确认 `[general] clear_sr_iframes_after_hit=true`；若某条 SR 真伤没生效，
  打开 `log_trigger` 看有没有「清帧名单可能已漂移」的提醒，并按提示把类型登记进 `ReliquaryRef`。
