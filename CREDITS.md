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

- **「新约」物品贴图**（本轮）：**直接引用 Minecraft 原版贴图** `minecraft:item/arrow`
  （写在 `models/item/new_testament.json` 的 `layer0` 里），受 Minecraft EULA 约束；后续换原创贴图时本条失效。
- **栏位图标**「新约-光环」（`textures/slot/new_testament_halo.png`）：由本模组作者提供。
- **MOD 图标**（`assets/summy-reliquary-apocrypha/icon.png`）：**本轮临时复用**前置模组 Summy Reliquary
  的图标（`assets/summy-reliquary/icon.png`），待专属图标做好后替换。

## 代码参考

- 自定义投掷物（`NewTestamentArrow`）的结构与「命中后贴地停留再删除」的做法参考了 SR 的
  `com.summy.reliquary.entity.ThrownSpear` / `ThrownRazor`。
- 真实伤害的落地口径（先清零无敌帧再结算、伤害减免交给 `bypasses_*` 标签）参考 SR 的
  `com.summy.reliquary.effect.RevelationBeam#applyTrueDamage`。
- 开发运行期消费 SR 生产 jar 的写法参考同作者的 `enchantment-reforged-forge` 工程。
