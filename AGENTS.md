# SimpMC-BedWars 1.8.8 分支维护规则

- 仅支持简体中文。只加载 `Languages/messages_zh_cn.yml`，移除 `/bw lang`、`/bw message`、自定义语言扫描、玩家偏好与语言切换事件；旧语言文件和数据库旧记录保留但不再使用。保留中文文件管理员自定义文本，缺失文本补中文默认值。
- 直接提交并推送原仓库 `legacy-1.8.8` 分支，不创建临时分支。
- 兼容 Java 21/25，字节码目标为 Java 21；Java 25 必须 fork javac。
- 插件与迁移工具发布前均使用 Java 21/25 执行 `mvn -B -ntp clean verify`，等待 CI 成功后核对实际 Release 下载文件的版本和 SHA-256。
- 保留无关用户文件、配置自定义值；功能移除使用主版本号，发布记录写入 CHANGELOG。
