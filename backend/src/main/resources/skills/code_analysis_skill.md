---
name: code_analysis
description: 分析给定代码的设计思路，生成清晰的架构图
trigger:
  - 逆向建模
  - 代码分析
  - 架构图
  - 分析代码
  - 生成架构
---

## 任务目标
分析给定代码的设计思路，生成清晰的架构图。

## 输入参数

| 参数名 | 类型 | 必填 | 描述 |
|--------|------|------|------|
| code | string | 是 | 待分析的代码内容 |
| language | string | 否 | 编程语言：java/python/go，默认自动识别 |
| depth | string | 否 | 分析深度：basic/detailed，默认basic |

## 输出格式

### 1. 模块架构图 (PlantUML)

```
@startuml
skinparam componentStyle uml2

[Controller层] --> [Service层]
[Service层] --> [Repository层]
[Service层] --> [External Service]

package "Domain Layer" {
  [Entity] -down-> [DTO]
  [VO] -down-> [Entity]
}

package "Config Layer" {
  [Configuration] --> [Service]
}
@enduml
```

### 2. 设计模式识别

| 模式类型 | 位置 | 说明 |
|----------|------|------|
| 单例模式 | XxxService | 全局唯一实例 |
| 工厂模式 | XxxFactory | 对象创建封装 |
| 策略模式 | XxxStrategy | 算法策略切换 |

### 3. 数据流描述

```
用户请求 → Controller → Service → Repository → Database
                ↓
            AOP增强
                ↓
            返回响应
```

## 分析维度

### 代码结构分析
- 包/目录组织结构
- 类与类的关系（继承/实现/依赖）
- 分层架构识别（MVC/三层架构/DDD等）

### 设计模式识别
- 创建型：单例、工厂、建造者
- 结构型：装饰器、代理、适配器
- 行为型：策略、观察者、模板方法

### 关键组件标注

| 组件类型 | 标识 | 说明 |
|----------|------|------|
| Controller | @RestController | 请求入口 |
| Service | @Service | 业务逻辑 |
| Repository | @Repository | 数据访问 |
| Entity | @Entity/@Data | 数据模型 |
| DTO | *DTO/*Request/*Response | 数据传输 |
| Config | @Configuration | 配置类 |
| Aspect | @Aspect | 切面 |

## 输出示例

```
## 架构分析报告

### 1. 整体架构

```
@startuml
title 基于Spring Boot的微服务架构

package "Controller Layer" {
  [UserController]
  [OrderController]
}

package "Service Layer" {
  [UserService]
  [OrderService]
}

package "Repository Layer" {
  [UserRepository]
  [OrderRepository]
}

database "MySQL"

[UserController] --> [UserService]
[OrderController] --> [OrderService]
[UserService] --> [UserRepository]
[OrderService] --> [OrderRepository]
[UserRepository] --> [MySQL]
[OrderRepository] --> [MySQL]
@enduml
```

### 2. 设计模式

| 模式 | 位置 | 用途 |
|------|------|------|
| 工厂模式 | ChatClientFactory | 创建AI客户端 |
| 模板方法 | AbstractChatClient | 聊天流程标准化 |

### 3. 数据流

1. 请求进入 Controller
2. 参数校验后调用 Service
3. Service 处理业务逻辑
4. Repository 访问数据库
5. 数据逐层返回并转换

### 4. 关键设计点

- **配置外部化**: 使用 @ConfigurationProperties 绑定配置
- **面向接口编程**: Service 定义接口，Impl 实现
- **单一职责**: 每个类职责清晰
```

## 注意事项
- 架构图使用标准 PlantUML 语法
- 类名使用 PascalCase
- 包名使用小写
- 关系箭头：--> 表示依赖，--|> 表示继承
