# API 开发

外部插件应将 BedWars API 声明为 `provided` 依赖，由运行服务器提供 API 类：

```xml
<dependency>
    <groupId>com.andrei1058.bedwars</groupId>
    <artifactId>bedwars-api</artifactId>
    <version>4.0.2</version>
    <scope>provided</scope>
</dependency>
```


## 4.0.0 中文消息 API

1.8.8 分支完全删除多语言契约。旧的 `api.language.Language`、`PlayerLangChangeEvent`、按 ISO/玩家选择语言、语言注册表及玩家语言数据库接口均已移除；依赖这些接口的附属插件需要重新编译。消息常量移动至 `com.andrei1058.bedwars.api.messages.Messages`，唯一的中文配置通过 `BedWars#getMessages()` 或 `ChineseMessages.get()` 获取。

继续使用原有 `Languages/messages_zh_cn.yml`，保留管理员自定义文字，缺失键补简体中文。其他旧语言文件与数据库旧记录保留但不读取，不再有语言兼容空实现。队伍显示名使用 `ITeam#getDisplayName()`，生成器中文全息字使用 `IGenerator#getHolograms()`，不再传递语言参数。

## 1.8.8 固定火球规则

以 paper-1.21.11 提交 `90d13d530835c67442bd79d683a3c95ab68390b6` 的默认平衡和行为为基准，所有参数写入代码：范围 3.25、普通初速 1.5、潜行初速 2.4、普通/潜行加速度 0.1/0.2、潜行后坐力 0.1、冷却 0.4 秒、水平/垂直击退参数 1.35/0.75、自伤 2、敌伤 3.5、队友无伤、不点火，随机累计飞行路程 200～300 格后移除。

自身和队友受击退但不把发射者记为攻击者；保留之前的敌人击杀归属。敌人使用原生爆炸伤害分类，保留防具/保护效果及伤害事件，取消火球原生接触和爆炸的重复伤害。1.8.8 使用对应 NMS 加速度与爆炸伤害入口，不依赖新版 Paper API。

旧 `fireball` 节启动时先备份到 `config.yml.before-fixed-fireball.bak` 再移除。不存在手动配置入口，迁移器不会重新带入旧火球设置。
