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

## 创作来源与素材声明

本模组是 Summy Reliquary 的独立扩展包。新约与凋亡的题材、名词和机制设计灵感参考《明日方舟》，
但本模组不是《明日方舟》的官方作品，与相关权利人无隶属或合作关系，也未使用其代码或素材。

本模组**与《以撒的结合》（The Binding of Isaac）没有任何关联**，未使用其代码、名称、设定、贴图或其它素材。
全部贴图（含 10 件物品贴图、10 个栏位图标、光翼与恶魔翅膀）均由本模组美术自行绘制并提供。

本模组完全免费，禁止任何商业用途。若任何内容无意中侵犯了您的权益，请联系 **525277385@qq.com**，
一经核实我们将立即移除相关素材并发布修复版本。

**English**: Summy Reliquary: Apocrypha is an independent expansion for Summy Reliquary. The New Testament
and Apoptosis themes, terminology, and mechanics are inspired by *Arknights*, but this is not an official
*Arknights* product and is not affiliated with or endorsed by its rights holders. The mod is **not associated
with *The Binding of Isaac*** and does not use its code, names, settings, textures, or other assets. All textures,
including the 10 item textures, 10 slot icons, and both wing textures, were independently drawn for this project.
The mod is completely free and **must not be used commercially**.

## 贴图

- **5 件饰品物品贴图**（`textures/item/new_testament.png`、`swift_feather_covenant.png`、`wing_feather_sacrament.png`、
  `inverse_feather_hover.png`、`paired_wings.png`）：**由本模组美术自行绘制并提供**（32×32 原图，未经二次加工）。
  创造页标签图标的图案取自「比翼·连携」。
- **全部 10 个栏位图标**：由本模组美术自行绘制并提供。
- **恶魔线（凋亡）素材**：**物品贴图由本模组美术自行绘制并提供** —— 堕天 / 坠羽 / 笞心 / 枯笔 / 终歌
  （`textures/item/<物品id>.png`，未做加工）；**坠羽爆条翅膀**为作者提供的**整对**图
  `textures/entity/demon_feather.png`（79×32：左翼 + 中间核心 + 右翼；作者已按需求把左缘 1px 裁掉以保证居中）。
  至此**两套共 10 件物品贴图全部由本模组美术自行绘制**；仅五个恶魔线**栏位图标**仍暂借新约套
  （提喻←光环、释经←才能、弥撒←恩典、悲悯←祝福、终歌←启示），作者补图后逐项替换。
- **MOD 图标**（`assets/summy-reliquary-apocrypha/icon.png`）：使用作者提供的「比翼·连携」物品贴图。

## 代码参考

- **凋亡的「不可闪避」**：SE（Summy Enchantment）一侧新增的 `#summy_enchantment:no_dodge` 伤害类型标签判断
  （`EnchantmentEffects#rollNimbleSteps` 接收 `DamageSource`、命中标签即不闪避；mixin 相应传 `source`），
  标签内容由本模组提供（`data/summy_enchantment/tags/damage_type/no_dodge.json`，未装 SE 时该文件无副作用）。
- 自定义投掷物（`NewTestamentArrow`）的结构与「命中后贴地停留再删除」的做法参考了 SR 的
  `com.summy.reliquary.entity.ThrownSpear` / `ThrownRazor`。
- 真实伤害的落地口径（先清零无敌帧再结算、伤害减免交给 `bypasses_*` 标签）参考 SR 的
  `com.summy.reliquary.effect.RevelationBeam#applyTrueDamage`。
- 开发运行期消费 SR 生产 jar 的写法参考同作者的 `summy-enchantment-forge` 工程。
- **二段跳的手感与输入状态机**（客户端预测 + 双端施加、`hasImpulse` 同步、`fallDistance` 清零、
  「离地后松开过跳跃键」的按下沿判定）参考 [Artifacts](https://github.com/ochotonida/artifacts)（MIT）1.20.1 分支的
  `CloudInABottleItem` / `CloudInABottleInputHandler` / `DoubleJumpPacket`（「云朵瓶」）。本模组为独立实现，
  未复制其代码或素材；差异见 README 的「滞空」小节（音效只响一次、服务端用一 tick 滞后量做离地判据）。
