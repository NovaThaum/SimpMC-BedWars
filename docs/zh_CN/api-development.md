# API 开发

外部插件应将 BedWars API 声明为 `provided` 依赖，由运行服务器提供 API 类：

```xml
<dependency>
    <groupId>com.andrei1058.bedwars</groupId>
    <artifactId>bedwars-api</artifactId>
    <version>1.7.0</version>
    <scope>provided</scope>
</dependency>
```
