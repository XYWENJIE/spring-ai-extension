# Spring AI Moyin

### 模块简介

**Spring AI Moyin**是Spring AI Extension项目的一部分，为墨音AI提供Spring AI框架的集成支持。该模块基于Spring AI基础架构，封装了墨音AI的RESTful API接口，使开发者能够在Spring应用中轻松使用墨音AI的大模型服务。

墨音AI官方文档：https://openapi.moyin.com/document

### 功能特性

支持墨音AI的主要功能模块：

1. **对话模型** - 支持文本对话、问答等场景
2. **图像生成** - 支持AI图像生成能力
3. **嵌入模型** - 支持文本向量化处理

### 安装与配置

#### 1. 添加依赖

在你的Spring项目的`pom.xml`中添加依赖：

```xml
<dependency>
	<groupId>org.xywenjie.spring-ai-extension</groupId>
	<artifactId>spring-ai-mojin</artifactId>
	<version>1.0.0-SNAPSHOT</version>
</dependency>
```

#### 2. 配置API密钥

在Spring Boot的`application.properties`或`application.yml`中配置：

```properties
spring.ai.moyin.api-key=你的api-key
```

或使用YAML格式：

```yaml
spring:
  ai:
    moyin:
      api-key: 你的api-key
```

#### 3. 使用示例

```java
@Autowired
private MoyinChatClient chatClient;

public String chat(String message) {
    return chatClient.call(message);
}
```

### 注意事项

- 请确保已获取有效的墨音AI API密钥
- 建议在生产环境使用稳定版本，SNAPSHOT版本请谨慎评估
- 由于Spring AI框架正在快速迭代，可能会出现破坏性更新

### 联系方式

有任何疑问可以提交issue或邮箱联系：xywenjie@outlook.com