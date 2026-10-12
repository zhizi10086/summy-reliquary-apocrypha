# 素材来源与鸣谢

> 本模组是 **Summy Reliquary（SR）的扩展包**，Minecraft 1.20.1 / Forge 47.4.10，**必须以 SR 为前置**。
> 许可见仓库根目录的 `LICENSE`（自定义许可：允许整合包收录、转载与个人修改，**必须保留声明并署名**，
> **禁止任何商业用途**）。

## 前置与依赖

- [Summy Reliquary](https://github.com/zhizi10086/summy-reliquary)（**必需前置**）：本模组的栏位体系、真实伤害
  与「全伤害增幅」乘区、天使标记门槛、圣心箭矢追踪等机制都直接复用它的公开 API。
  本模组**不包含** SR 的任何代码或素材副本（`libs/` 里的 SR jar 仅作为编译期与开发运行期的前置）。
- [Curios](https://modrinth.com/mod/curios)：饰品槽位系统（**必需前置**，转由 SR 一并要求）。
- [Forge](https://files.minecraftforge.net/) `47.4.10`（Minecraft 1.20.1）：模组加载器（**必需**）。

## 致谢与二创声明（《以撒的结合》）

本模组与 Summy Reliquary 同属基于《以撒的结合》（The Binding of Isaac）与基督教传统宗教文化符号的
**粉丝二创作品**。我们衷心感谢原作创作者 Edmund McMillen 与发行方 Nicalis, Inc.；本模组并非官方作品，
与 Edmund McMillen、Nicalis, Inc. 及任何宗教组织均无隶属或合作关系。

本模组完全免费，禁止任何商业用途。若任何内容无意中侵犯了您的权益，请联系 **525277385@qq.com**，
一经核实我们将立即移除相关素材并发布修复版本。

**English**: This is a **fan-made derivative work** inspired by *The Binding of Isaac*. It is completely free
and **must not be used commercially**. If any content unintentionally infringes your rights, please contact
**525277385@qq.com**; once verified, we will **remove the material immediately and publish a fixed release**.

## 贴图

- **5 件饰品物品贴图**（`textures/item/new_testament.png`、`swift_feather_covenant.png`、`wing_feather_sacrament.png`、
  `inverse_feather_hover.png`、`paired_wings.png`）：**由本模组作者绘制并提供**（32×32 原图，未经二次加工）。
  创造页标签图标的图案取自「比翼·连携」。
- **栏位图标**「新约-光环」（`textures/slot/new_testament_halo.png`）：由本模组作者提供。
- **恶魔线（凋亡）暂用占位**：物品贴图借原版 —— 堕天 `minecraft:block/wither_rose`（凋灵玫瑰在 1.20.1 只有方块贴图）、
  坠羽 `minecraft:item/phantom_membrane`、笞心 `minecraft:item/heart_of_the_sea`
  、枯笔 `minecraft:item/ink_sac`（后续 1 件计划沿用 `item/goat_horn`）；
  栏位图标按顺序借新约套（提喻←光环、释经←才能、弥撒←恩典、悲悯←祝福、终歌←启示）；
  「坠羽」爆条时的翅膀贴图暂用光羽的 `light_feather.png`。作者素材到位后逐项替换。
- **MOD 图标**（`assets/summy-reliquary-apocrypha/icon.png`）：**本轮临时复用**前置模组 Summy Reliquary
  的图标（`assets/summy-reliquary/icon.png`），待专属图标做好后替换。

## 代码参考

- **凋亡的「不可闪避」**：ER（Enchantment Reforged）一侧新增的 `#enchantment_reforged:no_dodge` 伤害类型标签判断
  （`EnchantmentEffects#rollNimbleSteps` 接收 `DamageSource`、命中标签即不闪避；mixin 相应传 `source`），
  标签内容由本模组提供（`data/enchantment_reforged/tags/damage_type/no_dodge.json`，未装 ER 时该文件无副作用）。
- 自定义投掷物（`NewTestamentArrow`）的结构与「命中后贴地停留再删除」的做法参考了 SR 的
  `com.summy.reliquary.entity.ThrownSpear` / `ThrownRazor`。
- 真实伤害的落地口径（先清零无敌帧再结算、伤害减免交给 `bypasses_*` 标签）参考 SR 的
  `com.summy.reliquary.effect.RevelationBeam#applyTrueDamage`。
- 开发运行期消费 SR 生产 jar 的写法参考同作者的 `enchantment-reforged-forge` 工程。
- **二段跳的手感与输入状态机**（客户端预测 + 双端施加、`hasImpulse` 同步、`fallDistance` 清零、
  「离地后松开过跳跃键」的按下沿判定）参考 [Artifacts](https://github.com/ochotonida/artifacts)（MIT）1.20.1 分支的
  `CloudInABottleItem` / `CloudInABottleInputHandler` / `DoubleJumpPacket`（「云朵瓶」）。本模组为独立实现，
  未复制其代码或素材；差异见 README 的「滞空」小节（音效只响一次、服务端用一 tick 滞后量做离地判据）。
