# Summy Reliquary: Apocrypha

> Summy Reliquary 的扩展包 · Minecraft 1.20.1 Forge

**前置：** [Summy Reliquary](https://github.com/zhizi10086/summy-reliquary)（必需）｜ Curios 5.14.1+1.20.1（必需）｜
Forge 1.20.1（推荐 47.4.10+）

[CHANGELOG](CHANGELOG.md) | [CREDITS](CREDITS.md) | [LICENSE](LICENSE)

## 内容

| 名称 | 类型 | 说明 |
| --- | --- | --- |
| 新约-光环 | Curios 栏位 | 本模组自己的栏位（面板里排在 SR「光环」之后），目前只接受「新约」 |
| 新约 | 饰品（新约-光环） | **需要天使标记**才能佩戴与查阅。弓 / 弩的箭矢命中目标后：自身回复 0.5 点生命；有 35% 概率在目标碰撞箱顶部上方 3 格处散射落下 3~5 支箭矢（散布角度 0~25°），落箭固定 **5 点真实伤害**（无视护甲 / 保护附魔 / 抗性 / 无敌帧），可享受 Summy Reliquary 的全伤害增幅，并受 SR「圣心」的箭矢追踪牵引；散射触发时给佩戴者本人播一次箭矢命中音效 |

## 前置与依赖

- **Summy Reliquary**：本模组的所有玩法耦合都走它的公开 API（`CurioItemSupport` 的天使门槛与读档放行、
  `ReliquaryTooltips` 的提示格式、`CurioHelper` 的佩戴判定、`PlayerFlags`、真实伤害与增伤乘区）。
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
  Apocrypha.java            主入口：注册物品 / 创造页 / 实体 / 配置
  ApocryphaClient.java      客户端入口：实体渲染器
  config/                   配置（config/summy_reliquary_apocrypha-common.toml）
  entity/                   落箭实体
  effect/                   游戏内事件（命中判定与散射）
  integration/ReliquaryRef  **对 SR 的唯一访问出口**（后续所有跨模组调用都从这里走）
  item/                     饰品「新约」
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
4. **真实伤害**：在 `data/summy-reliquary-apocrypha/damage_type/` 加类型文件，并把类型 id 追加进
   `data/minecraft/tags/damage_type/bypasses_{armor,effects,enchantments,resistance}.json`；
   落地时先把目标 `invulnerableTime = 0` 再 `hurt(...)`（SR 的 `RevelationBeam#applyTrueDamage` 就是这套口径）。
5. **想要 SR 的增伤**：不要自己复刻倍率。把伤害源的**造成者**设为玩家、**直接来源**设为投掷物实体，
   并且不把伤害类型写进 SR 的 `isDivine` / `isExactDamage` 名单 —— SR 的 `LivingHurtEvent`
   会自动叠加七罪 / 七德与「全伤害最终倍率」乘区。
6. **给 SR 接入新实体**：SR 的圣心追踪筛选的是「`AbstractArrow` + owner 是该玩家」，
   所以只要你的投掷物继承 `AbstractArrow` 且 owner 设为玩家，就会自动获得追踪（不需要任何代码）；
   反之若不想被追踪，得在 SR 侧加排除（本工程的原则是**不改 SR**）。

## 更新前置 jar（SR 出新版本时）

1. 把 SR 产物 `../JAR/summy-reliquary/summy-reliquary-<版本>.jar` 拷进本工程的 `libs/`
   （文件名保持 `summy-reliquary-<版本>.jar`）；
2. 改 `gradle.properties` 的 `summy_reliquary_version=<版本>`；
3. `gradlew build` 通过即可（`compileOnly` 与 `run/mods` 的那份都会跟着这个版本号走）。

## 排查

- **启动就报缺少前置**：`run/mods` 里没有 SR（`gradlew runClient` 会自动投放；直接用 IDE 启动时要手动放）。
- **装上饰品却查不到功能描述**：没有天使标记（`/summyreliquary angel grant`）。
- **落箭没伤害**：确认 `data/summy-reliquary-apocrypha/damage_type/new_testament_arrow.json` 与四个
  `data/minecraft/tags/damage_type/bypasses_*.json` 都在 jar 里（被打包时丢文件会退回原版魔法伤害源）。
