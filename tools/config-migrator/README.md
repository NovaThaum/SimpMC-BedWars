# BedWars 配置迁移工具

此独立工具把 1.21.11 的 YAML 配置值合并进 1.8.8 插件生成的目标配置模板。它不启动服务器，不修改输入文件，也不会将 1.21.11 的 `config-version` 带入旧版配置。

需要 Java 21 或更新版本。先运行一次 1.8.8 服务器生成配置，然后停服并执行：

```text
java -jar bedwars-config-migrator-1.0.0.jar <modern.yml> <legacy-template.yml> <output-directory>
```

输出目录包含迁移后的同名 YAML 和 `migration-report.txt`。只有目标模板中已经存在的路径会被迁移；1.8 模板的结构和默认键作为目标结构。无法对应的现代版配置路径会列在报告中，不会被写入目标文件。对材料名和声音名执行的已知映射也会记录在报告里。

分别对 `config.yml`、`sounds.yml`、`shop.yml`、`upgrades.yml`、`generators.yml` 和每个竞技场 YAML 运行迁移時，输入与目标模板必须是相同用途的文件。先检查报告及输出差异，再手动放入 1.8.8 插件目录。该工具不会自动转换竞技场坐标、物品附魔/自定义数据、代理设置或数据库内容。
