# HealthAgent 系统设计文档

## 一、项目概述

HealthAgent 是一个智能健康助手系统，为用户提供保单查询、体检预约、健康咨询等一站式健康服务。系统采用前后端分离架构，后端基于 Spring Boot 3.4.1 + Java 21，前端基于 Vue 3 + TypeScript，集成多种大语言模型（GLM、通义千问）提供智能对话能力。

---

## 二、系统架构

### 2.1 技术栈

| 层级          | 技术选型                    | 说明                             |
| ------------- | --------------------------- | -------------------------------- |
| **前端**      | Vue 3 + TypeScript          | 采用 Composition API + Vite 构建 |
| **后端**      | Spring Boot 3.4.1 + Java 21 | 现代化微服务架构基础             |
| **数据库**    | MySQL 8.0                   | 关系型数据存储                   |
| **ORM 框架**  | MyBatis-Plus 3.5.5          | 简化数据库操作                   |
| **AI 大模型** | 智谱 GLM-4 + 通义千问       | 双模型支持，可配置切换           |
| **认证鉴权**  | JWT + Spring AOP            | Token 认证 + 拦截器              |
| **API 文档**  | SpringDoc OpenAPI           | Swagger UI 自动生成接口文档      |
| **日志框架**  | SLF4J + Logback + MDC       | 彩色日志 + 链路追踪              |

### 2.2 系统架构图

```
┌─────────────────────────────────────────────────────────────┐
│                        用户端 (Browser)                       │
│                                                               │
│  ┌─────────┐  ┌──────────┐  ┌──────────┐  ┌──────────┐      │
│  │  登录页  │  │  首页   │  │ 聊天页面 │  │ 保单页面 │      │
│  └─────────┘  └──────────┘  └──────────┘  └──────────┘      │
│                                                               │
│  ┌──────────┐  ┌──────────┐  ┌──────────┐                   │
│  │ 体检套餐 │  │ 体检预约 │  │ 预约记录 │                   │
│  └──────────┘  └──────────┘  └──────────┘                   │
└──────────────────────────────┬──────────────────────────────┘
                               │ HTTP REST API
┌──────────────────────────────┴──────────────────────────────┐
│                      后端服务 (Spring Boot)                   │
│                                                               │
│  ┌─────────────┐  ┌─────────────┐  ┌─────────────┐         │
│  │  认证控制器  │  │  聊天控制器  │  │  智能对话    │         │
│  │  AuthCtrl   │  │  ChatCtrl   │  │ SmartChatCtrl│         │
│  └──────┬──────┘  └──────┬──────┘  └──────┬──────┘         │
│         │                │                │                 │
│  ┌──────┴──────┐  ┌──────┴──────┐  ┌──────┴──────┐         │
│  │  保单控制器  │  │  体检控制器  │             │         │
│  │ PolicyCtrl  │  │  ExamCtrl   │             │         │
│  └──────┬──────┘  └──────┬──────┘             │         │
│         │                │                    │         │
│         └────────────────┼────────────────────┘         │
│                          │                              │
│  ┌───────────────────────┴──────────────────────────┐   │
│  │                  服务层 (Service)                  │   │
│  │                                                   │   │
│  │  ┌─────────────┐  ┌─────────────┐  ┌──────────┐ │   │
│  │  │  AuthService │  │ SmartChat   │  │  AiService│ │   │
│  │  └─────────────┘  └─────────────┘  └──────────┘ │   │
│  │                                                   │   │
│  │  ┌─────────────┐  ┌─────────────┐  ┌──────────┐ │   │
│  │  │ PolicyService│  │ExamService  │  │ IntentRec │ │   │
│  │  └─────────────┘  └─────────────┘  └──────────┘ │   │
│  │                                                   │   │
│  │  ┌─────────────────────────────────────────────┐ │   │
│  │  │           聊天客户端 (Template Method)       │ │   │
│  │  │  ┌──────────────┐  ┌──────────────┐        │ │   │
│  │  │  │  GlmChatClient│  │ QwenChatClient│        │ │   │
│  │  │  └──────────────┘  └──────────────┘        │ │   │
│  │  │           ↑                                │ │   │
│  │  │  ┌──────────────┐                          │ │   │
│  │  │  │AbstractChat  │                          │ │   │
│  │  │  └──────────────┘                          │ │   │
│  │  └─────────────────────────────────────────────┘ │   │
│  └───────────────────────────────────────────────────┘   │
│                          │                              │
│  ┌───────────────────────┴──────────────────────────┐   │
│  │                  数据访问层 (Mapper)               │   │
│  │                                                   │   │
│  │  ┌──────────┐  ┌──────────────┐  ┌────────────┐ │   │
│  │  │PolInfo   │  │ExamHospital  │  │ExamBooking │ │   │
│  │  │Mapper    │  │Mapper        │  │Mapper      │ │   │
│  │  └──────────┘  └──────────────┘  └────────────┘ │   │
│  │                                                   │   │
│  │  ┌──────────────┐  ┌──────────────┐             │   │
│  │  │ExamPackage   │  │ExamPlan      │             │   │
│  │  │Mapper        │  │Mapper        │             │   │
│  │  └──────────────┘  └──────────────┘             │   │
│  └───────────────────────────────────────────────────┘   │
└──────────────────────────────┬──────────────────────────────┘
                               │ MyBatis-Plus
┌──────────────────────────────┴──────────────────────────────┐
│                        MySQL 数据库                          │
│                                                               │
│  ┌──────────┐  ┌──────────────┐  ┌────────────┐             │
│  │ pol_info │  │exam_hospital │  │exam_booking│             │
│  └──────────┘  └──────────────┘  └────────────┘             │
│                                                               │
│  ┌──────────────┐  ┌──────────────┐                         │
│  │exam_package  │  │exam_plan     │                         │
│  └──────────────┘  └──────────────┘                         │
└─────────────────────────────────────────────────────────────┘
```

### 2.3 分层架构

```
┌─────────────────────────────────────────┐
│          Presentation Layer             │
│  (Controller + Interceptor + Filter)    │
├─────────────────────────────────────────┤
│          Business Layer                 │
│  (Service + Intent Recognition)         │
├─────────────────────────────────────────┤
│          Data Access Layer              │
│  (Mapper + Entity)                      │
├─────────────────────────────────────────┤
│          Infrastructure Layer           │
│  (Database + External AI APIs)          │
└─────────────────────────────────────────┘
```

---

## 三、核心模块设计

### 3.1 智能对话模块 (Smart Chat)

#### 3.1.1 意图识别流程

```
用户输入消息
     │
     ▼
┌─────────────────┐
│  IntentRecognition │
│     Service        │
└────────┬─────────┘
         │ 调用 LLM
         ▼
┌─────────────────┐
│  意图分类         │
│  • query_policy    │
│  • book_examination│
│  • health_consult  │
│  • general_conv    │
└────────┬─────────┘
         │
         ▼
┌─────────────────┐
│  SmartChatService│
│  路由到对应处理   │
└────────┬─────────┘
         │
    ┌────┼────┐
    ▼    ▼    ▼
  保单  体检  健康
  查询  预约  咨询
```

#### 3.1.2 意图类型定义

| 意图代码               | 描述     | 触发关键词       | 对应服务           |
| ---------------------- | -------- | ---------------- | ------------------ |
| `query_policy`         | 查询保单 | 保单、保险、理赔 | PolicyService      |
| `book_examination`     | 体检预约 | 体检、预约、检查 | ExaminationService |
| `health_consultation`  | 健康咨询 | 健康、症状、疾病 | AiService          |
| `general_conversation` | 一般对话 | 其他             | AiService          |

#### 3.1.3 Function Calling 保单参数提取

```
用户输入: "帮我查一下保单，身份证号是110101199001011234"
                              │
                              ▼
┌─────────────────────────────────────┐
│  PolicyInfoExtractor                │
│  (Function Calling)                 │
│                                     │
│  提取参数:                          │
│  - userId: null                     │
│  - idCardNo: "110101199001011234"   │
│  - polNo: null                      │
│  - policyHolderName: null           │
└─────────────┬───────────────────────┘
              │
              ▼
┌─────────────────────────────────────┐
│  queryPoliciesByParams              │
│  根据参数调用对应查询方法             │
└─────────────┬───────────────────────┘
              │
              ▼
┌─────────────────────────────────────┐
│  PolicyService                      │
│  返回保单列表                        │
└─────────────────────────────────────┘
```

### 3.2 LLM 客户端架构（模板方法模式）

```
┌──────────────────────────────┐
│      AbstractChatClient       │
│  ┌────────────────────────┐  │
│  │ chat() [模板方法]       │  │
│  │  1. buildMessages()     │  │
│  │  2. buildRequestBody()  │  │
│  │  3. callApi()           │  │
│  │  4. parseResponse()     │  │
│  └────────────────────────┘  │
│                              │
│  ┌────────────────────────┐  │
│  │ chatWithFunctionCall()  │  │
│  │  1. buildMessagesForFC  │  │
│  │  2. buildReqWithFunc    │  │
│  │  3. callApi()           │  │
│  │  4. parseFCResponse()   │  │
│  └────────────────────────┘  │
│                              │
│  抽象方法:                    │
│  - buildRequestBody()        │
│  - callApi()                 │
│  - parseResponse()           │
│  - parseFunctionCallResponse│
└──────────┬───────────────────┘
           │ 继承
    ┌──────┴──────┐
    ▼             ▼
┌───────────┐ ┌─────────────┐
│GlmChat    │ │ QwenChat    │
│Client     │ │ Client      │
├───────────┤ ├─────────────┤
│ GLM API   │ │ DashScope   │
│ 实现      │ │ SDK 实现    │
└───────────┘ └─────────────┘
```

#### 3.2.1 客户端工厂

```java
ChatClientFactory.createClient(provider, apiKey, baseUrl, model)
    │
    ├── "glm" → GlmChatClient
    │           • API: /api/paas/v4/chat/completions
    │           • 请求格式: {model, messages, stream}
    │           • 响应格式: choices[0].message.content
    │
    └── "qwen" / "dashscope" → QwenChatClient
                • SDK: com.alibaba.dashscope.Application
                • 模型: qwen-turbo / qwen-plus / qwen-max
                • 响应格式: ApplicationResult.getOutput().getText()
```

### 3.3 用户认证模块

```
┌──────────┐     POST /auth/login      ┌──────────────┐
│  用户    │ ─────────────────────────→ │ AuthController│
└──────────┘                           └──────┬───────┘
                                             │
                                             ▼
                                      ┌──────────────┐
                                      │ AuthService   │
                                      │               │
                                      │ • verifyUser  │
                                      │ • genTokens   │
                                      │   (Access +   │
                                      │    Refresh)   │
                                      └──────┬───────┘
                                             │
                                             ▼
                                      返回 JWT Tokens
                                             
┌──────────┐     带 Authorization 头    ┌──────────────┐
│  用户    │ ─────────────────────────→ │ AuthInterceptor│
└──────────┘                           └──────┬───────┘
                                             │
                                    ┌────────┼────────┐
                                    ▼                 ▼
                              验证 Access       过期则验证
                              Token 签名        Refresh Token
```

### 3.4 保单查询模块

```
┌─────────────────────────────────────────────────────┐
│                 PolicyController                     │
│                                                      │
│  POST /api/policy/query       条件查询               │
│  POST /api/policy/page        分页查询               │
│  GET  /api/policy/{polNo}     按保单号查询            │
│  GET  /api/policy/holder/{name} 按投保人查询         │
│  GET  /api/policy/idcard/{id} 按身份证查询           │
│  GET  /api/policy/all         查询所有               │
│  POST /api/policy             新增保单               │
│  PUT  /api/policy             更新保单               │
│  DEL  /api/policy/{polNo}     删除保单               │
└──────────────────────┬───────────────────────────────┘
                       │
                       ▼
┌─────────────────────────────────────────────────────┐
│                 PolicyService                        │
│                                                      │
│  • getByPolNo()         根据保单号查询                │
│  • getByPolicyHolderName()  按投保人姓名查询          │
│  • getByIdCardNo()      根据身份证号查询              │
│  • queryPolicies()      条件查询                     │
│  • queryPoliciesPage()  分页查询                     │
│  • formatPoliciesAsText() 格式化保单文本             │
└──────────────────────┬───────────────────────────────┘
                       │
                       ▼
┌─────────────────────────────────────────────────────┐
│                 PolInfoMapper                        │
│                                                      │
│  继承 BaseMapper<PolInfoEntity>                      │
│  利用 MyBatis-Plus 自动生成 CRUD SQL                  │
└──────────────────────┬───────────────────────────────┘
                       │
                       ▼
┌─────────────────────────────────────────────────────┐
│                   pol_info 表                        │
│                                                      │
│  pol_no          VARCHAR(50)  PK  保单号              │
│  policy_holder_name VARCHAR(50)   投保人姓名          │
│  id_card_no      VARCHAR(20)      身份证号            │
│  insurance_product VARCHAR(100)   保险产品            │
│  sum_insured     DECIMAL          保额               │
│  premium         DECIMAL          保费               │
│  effective_date  DATE             生效日期            │
│  expiry_date     DATE             到期日期            │
│  status          VARCHAR(20)      状态               │
└─────────────────────────────────────────────────────┘
```

### 3.5 体检预约模块

```
┌────────────────────────────────────────────────────────────────┐
│                     ExaminationController                       │
│                                                                 │
│  GET  /api/exam/hospitals        查询医院列表                   │
│  GET  /api/exam/packages         查询体检套餐                   │
│  GET  /api/exam/plans            查询体检计划                   │
│  GET  /api/exam/bookings/{userId} 查询预约记录                  │
│  POST /api/exam/bookings         创建体检预约                   │
│  PUT  /api/exam/bookings/{id}    更新预约                       │
│  DEL  /api/exam/bookings/{id}    取消预约                       │
└──────────────────────────┬─────────────────────────────────────┘
                           │
                           ▼
┌────────────────────────────────────────────────────────────────┐
│                     ExaminationService                          │
│                                                                 │
│  • getHospitalList()         获取医院列表                       │
│  • getPackageList()          获取套餐列表                       │
│  • getPlanList()             获取体检计划                       │
│  • bookExamination()         预约体检                           │
│  • cancelBooking()           取消预约                           │
│  • getUserBookings()         查询用户预约记录                   │
└──────────────────────────┬─────────────────────────────────────┘
                           │
                           ▼
┌────────────────────────────────────────────────────────────────┐
│                     Mappers                                     │
│                                                                 │
│  • ExaminationHospitalMapper   医院信息                        │
│  • ExaminationPackageMapper    体检套餐                        │
│  • ExaminationPlanMapper       体检计划（关联医院+套餐）        │
│  • ExaminationBookingMapper    预约记录                        │
└──────────────────────────┬─────────────────────────────────────┘
                           │
                           ▼
┌────────────────────────────────────────────────────────────────┐
│                     数据库表                                    │
│                                                                 │
│  exam_hospital     医院信息表                                   │
│  exam_package      体检套餐表                                   │
│  exam_plan         体检计划表                                   │
│  exam_booking      体检预约表                                   │
└────────────────────────────────────────────────────────────────┘
```

---

## 四、数据库设计

### 4.1 保单信息表 (pol_info)

```sql
CREATE TABLE pol_info (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键',
    pol_no VARCHAR(50) NOT NULL UNIQUE COMMENT '保单号',
    policy_holder_name VARCHAR(50) NOT NULL COMMENT '投保人姓名',
    id_card_no VARCHAR(20) NOT NULL COMMENT '身份证号',
    insurance_product VARCHAR(100) COMMENT '保险产品',
    sum_insured DECIMAL(12,2) COMMENT '保额',
    premium DECIMAL(10,2) COMMENT '保费',
    effective_date DATE COMMENT '生效日期',
    expiry_date DATE COMMENT '到期日期',
    status VARCHAR(20) DEFAULT 'active' COMMENT '状态: active/expired',
    remark VARCHAR(500) COMMENT '备注',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    INDEX idx_policy_holder (policy_holder_name),
    INDEX idx_id_card (id_card_no),
    INDEX idx_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='保单信息表';
```

### 4.2 体检预约相关表

```sql
-- 医院信息表
CREATE TABLE exam_hospital (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    hospital_code VARCHAR(50) NOT NULL UNIQUE COMMENT '医院编码',
    hospital_name VARCHAR(100) NOT NULL COMMENT '医院名称',
    address VARCHAR(200) COMMENT '医院地址',
    contact_phone VARCHAR(20) COMMENT '联系电话',
    status VARCHAR(20) DEFAULT 'active' COMMENT '状态',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='医院信息表';

-- 体检套餐表
CREATE TABLE exam_package (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    package_code VARCHAR(50) NOT NULL UNIQUE COMMENT '套餐编码',
    package_name VARCHAR(100) NOT NULL COMMENT '套餐名称',
    description VARCHAR(500) COMMENT '套餐描述',
    price DECIMAL(10,2) COMMENT '价格',
    items JSON COMMENT '检查项目',
    status VARCHAR(20) DEFAULT 'active' COMMENT '状态',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='体检套餐表';

-- 体检计划表
CREATE TABLE exam_plan (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    plan_name VARCHAR(100) NOT NULL COMMENT '计划名称',
    hospital_id BIGINT NOT NULL COMMENT '医院ID',
    package_id BIGINT NOT NULL COMMENT '套餐ID',
    available_dates JSON COMMENT '可预约日期',
    status VARCHAR(20) DEFAULT 'active' COMMENT '状态',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_hospital (hospital_id),
    INDEX idx_package (package_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='体检计划表';

-- 体检预约表
CREATE TABLE exam_booking (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    booking_no VARCHAR(50) NOT NULL UNIQUE COMMENT '预约号',
    user_id VARCHAR(50) NOT NULL COMMENT '用户ID',
    user_name VARCHAR(50) NOT NULL COMMENT '用户姓名',
    id_card_no VARCHAR(20) COMMENT '身份证号',
    phone VARCHAR(20) COMMENT '联系电话',
    hospital_id BIGINT COMMENT '医院ID',
    hospital_name VARCHAR(100) COMMENT '医院名称',
    plan_id BIGINT COMMENT '计划ID',
    package_name VARCHAR(100) COMMENT '套餐名称',
    examination_date DATE COMMENT '体检日期',
    examination_time VARCHAR(20) COMMENT '体检时段',
    status VARCHAR(20) DEFAULT 'pending' COMMENT '状态: pending/confirmed/completed/cancelled',
    notes VARCHAR(500) COMMENT '备注',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_user (user_id),
    INDEX idx_hospital (hospital_id),
    INDEX idx_status (status),
    INDEX idx_date (examination_date)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='体检预约表';
```

---

## 五、接口设计

### 5.1 认证接口

| 方法 | 路径                 | 描述             | 认证 |
| ---- | -------------------- | ---------------- | ---- |
| POST | `/auth/login`        | 用户登录         | 否   |
| POST | `/auth/refresh`      | 刷新 Token       | 否   |
| GET  | `/auth/current-user` | 获取当前用户信息 | 是   |

### 5.2 聊天接口

| 方法   | 路径                              | 描述                   | 认证 |
| ------ | --------------------------------- | ---------------------- | ---- |
| POST   | `/chat/send`                      | 发送消息               | 是   |
| POST   | `/chat/session`                   | 创建会话               | 是   |
| DELETE | `/chat/session/{sessionId}`       | 清除会话               | 是   |
| GET    | `/chat/session/{sessionId}/count` | 获取消息数             | 是   |
| POST   | `/smart-chat/send`                | 智能对话（含意图识别） | 是   |
| POST   | `/smart-chat/intent`              | 意图识别               | 是   |

### 5.3 保单接口

| 方法   | 路径                        | 描述         | 认证 |
| ------ | --------------------------- | ------------ | ---- |
| POST   | `/api/policy/query`         | 条件查询     | 是   |
| POST   | `/api/policy/page`          | 分页查询     | 是   |
| GET    | `/api/policy/{polNo}`       | 按保单号查询 | 是   |
| GET    | `/api/policy/holder/{name}` | 按投保人查询 | 是   |
| GET    | `/api/policy/idcard/{id}`   | 按身份证查询 | 是   |
| GET    | `/api/policy/all`           | 查询所有     | 是   |
| POST   | `/api/policy`               | 新增保单     | 是   |
| PUT    | `/api/policy`               | 更新保单     | 是   |
| DELETE | `/api/policy/{polNo}`       | 删除保单     | 是   |

### 5.4 体检接口

| 方法   | 路径                          | 描述     | 认证 |
| ------ | ----------------------------- | -------- | ---- |
| GET    | `/api/exam/hospitals`         | 医院列表 | 是   |
| GET    | `/api/exam/packages`          | 体检套餐 | 是   |
| GET    | `/api/exam/plans`             | 体检计划 | 是   |
| GET    | `/api/exam/bookings/{userId}` | 预约记录 | 是   |
| POST   | `/api/exam/bookings`          | 创建预约 | 是   |
| PUT    | `/api/exam/bookings/{id}`     | 更新预约 | 是   |
| DELETE | `/api/exam/bookings/{id}`     | 取消预约 | 是   |

---

## 六、配置设计

### 6.1 核心配置项

```yaml
server:
  port: 8080

spring:
  datasource:
    url: jdbc:mysql://localhost:3306/health_agent
    username: root
    password: ${DB_PASSWORD}
  ai:
    openai:
      base-url: https://open.bigmodel.cn

healthagent:
  glm:
    api-key: ${GLM_API_KEY}
  chat:
    provider: glm          # glm | qwen
    model: glm-4           # glm-4 | qwen-turbo | qwen-plus
  auth:
    interceptor-enabled: true  # debug 时可关闭
    token-expiration: 86400000  # 24 小时
    refresh-token-expiration: 604800000  # 7 天
```

### 6.2 大模型配置切换

| provider | model           | API 端点                 | 说明           |
| -------- | --------------- | ------------------------ | -------------- |
| `glm`    | `glm-4`         | https://open.bigmodel.cn | 智谱 GLM       |
| `glm`    | `glm-4.7-flash` | https://open.bigmodel.cn | 智谱免费模型   |
| `qwen`   | `qwen-turbo`    | 通过 DashScope SDK       | 通义千问基础版 |
| `qwen`   | `qwen-plus`     | 通过 DashScope SDK       | 通义千问增强版 |
| `qwen`   | `qwen-max`      | 通过 DashScope SDK       | 通义千问旗舰版 |

---

## 七、关键设计模式

### 7.1 模板方法模式 (Template Method)

用于 LLM 客户端抽象，定义标准聊天流程，各模型实现具体细节：

- **抽象类**：`AbstractChatClient`
- **具体实现**：`GlmChatClient`、`QwenChatClient`
- **模板流程**：buildMessages → buildRequestBody → callApi → parseResponse

### 7.2 工厂模式 (Factory Pattern)

用于 LLM 客户端创建，根据配置自动实例化对应客户端：

- **工厂类**：`ChatClientFactory`
- **创建逻辑**：根据 `provider` 参数返回对应的客户端实例

### 7.3 策略模式 (Strategy Pattern)

用于意图识别后的路由分发：

```java
switch (intent) {
    case QUERY_POLICY → handleInsuranceQuery()
    case BOOK_EXAMINATION → handleExaminationBooking()
    case HEALTH_CONSULTATION → handleGeneralConversation()
    case GENERAL_CONVERSATION → handleGeneralConversation()
}
```

### 7.4 拦截器模式 (Interceptor Pattern)

用于认证鉴权：

- **拦截器**：`AuthInterceptor`
- **配置开关**：`interceptor-enabled` 支持 debug 时关闭

### 7.5 切面编程 (AOP)

用于日志记录和链路追踪：

- **日志切面**：`LogAspect` - 自动记录 Controller 入参和出参
- **过滤器**：`TraceIdFilter` - 为每个请求生成 MDC TraceId

---

## 八、安全设计

### 8.1 认证机制

- **JWT Token**：Access Token + Refresh Token 双 Token 机制
- **Access Token**：有效期 24 小时，用于接口认证
- **Refresh Token**：有效期 7 天，用于刷新 Access Token
- **JWT Secret**：通过 `JwtSecretProvider` 统一管理密钥

### 8.2 接口保护

- **认证拦截器**：保护需要登录的接口
- **参数验证**：使用 `@Valid` + `@NotBlank` 等注解校验参数
- **MDC 链路追踪**：每个请求生成唯一 TraceId，方便问题排查

### 8.3 密码存储

- 密码使用 BCrypt 加密存储
- 不存储明文密码

---

## 九、前端架构

### 9.1 页面结构

```
frontend/
├── src/
│   ├── pages/
│   │   ├── LoginPage.vue          登录页面
│   │   ├── HomePage.vue           首页（欢迎页）
│   │   ├── DashboardPage.vue      仪表盘（健康数据展示）
│   │   ├── ChatPage.vue           聊天页面（含语音输入）
│   │   ├── PolicyPage.vue         保单查询页面
│   │   ├── ExaminationPackagesPage.vue  体检套餐列表
│   │   ├── ExaminationDetailPage.vue    体检预约表单
│   │   └── ExaminationBookingsPage.vue  预约记录管理
│   │
│   ├── composables/
│   │   ├── useAuth.ts             认证逻辑
│   │   └── useTheme.ts            主题切换
│   │
│   ├── utils/
│   │   ├── api.ts                 通用 API 封装
│   │   ├── examinationApi.ts      体检 API
│   │   └── policyApi.ts           保单 API
│   │
│   ├── types/
│   │   ├── auth.ts                认证类型定义
│   │   ├── examination.ts         体检类型定义
│   │   └── policy.ts              保单类型定义
│   │
│   ├── router/
│   │   └── index.ts               路由配置
│   │
│   └── components/
│       └── Empty.vue              空状态组件
```

### 9.2 路由设计

```
/                   → HomePage (首页)
/login              → LoginPage (登录)
/dashboard          → DashboardPage (仪表盘)
/chat               → ChatPage (聊天)
/policy             → PolicyPage (保单查询)
/exam/packages      → ExaminationPackagesPage (体检套餐)
/exam/book          → ExaminationDetailPage (体检预约)
/exam/bookings      → ExaminationBookingsPage (预约记录)
```

### 9.3 状态管理

- **认证状态**：通过 `useAuth` composable 管理，包含 Token 存储、用户信息
- **主题状态**：通过 `useTheme` composable 管理，支持明暗主题切换
- **API 状态**：通过统一的 API 工具类处理请求/响应拦截

---

## 十、部署架构

### 10.1 开发环境

```
┌─────────────────┐    ┌─────────────────┐
│  前端开发服务器   │    │  后端开发服务器   │
│  (Vite :5173)   │───→│ (Spring :8080)  │
└─────────────────┘    └────────┬────────┘
                                │
                                ▼
                       ┌─────────────────┐
                       │   MySQL 数据库    │
                       │   (localhost:3306)│
                       └─────────────────┘
```

### 10.2 生产环境建议

```
┌───────────────────────────────┐
│         Nginx / CDN           │
│  (静态资源 + 反向代理)          │
└───────────────┬───────────────┘
                │
    ┌───────────┼───────────┐
    ▼           ▼           ▼
┌───────┐ ┌───────┐ ┌───────┐
│ App 1 │ │ App 2 │ │ App 3 │
│ :8080 │ │ :8080 │ │ :8080 │
└───┬───┘ └───┬───┘ └───┬───┘
    │         │         │
    └─────────┼─────────┘
              │
    ┌─────────┼─────────┐
    ▼                   ▼
┌─────────┐         ┌─────────┐
│ MySQL   │         │ Redis   │
│ 主从复制 │         │ (可选)  │
└─────────┘         └─────────┘
```

---

## 十一、扩展性设计

### 11.1 新增大模型接入

1. 继承 `AbstractChatClient` 创建新的客户端类
2. 实现四个抽象方法：
    - `buildRequestBody()`
    - `callApi()`
    - `parseResponse()`
    - `parseFunctionCallResponse()`
3. 在 `ChatClientFactory` 中添加对应 provider 的创建逻辑

### 11.2 新增意图类型

1. 在 `IntentType` 枚举中添加新的意图类型
2. 在 `SmartChatService` 的 switch 语句中添加对应的处理逻辑
3. 更新 `IntentRecognitionService` 的提示词

### 11.3 新增业务模块

1. 创建对应的 Entity + Mapper + Service + Controller
2. 在 `SmartChatService` 中集成意图识别和参数提取
3. 前端添加对应的页面和路由

---

## 十二、性能优化

### 12.1 数据库优化

- **索引策略**：为常用查询字段建立索引（身份证号、保单号、用户ID等）
- **分页查询**：使用 MyBatis-Plus 分页插件，避免全表扫描
- **连接池**：使用 HikariCP 连接池管理数据库连接

### 12.2 缓存策略（建议）

- **用户会话**：可引入 Redis 缓存会话信息
- **热点数据**：医院列表、体检套餐等静态数据可缓存
- **限流**：可引入 Redis 实现接口限流

### 12.3 AI 调用优化

- **会话管理**：通过 `SessionManager` 管理上下文，避免重复传递历史消息
- **超时控制**：HTTP 请求设置合理的连接超时和读取超时
- **错误重试**：可加入重试机制处理临时性故障

---

## 十三、日志与监控

### 13.1 日志设计

- **日志格式**：彩色日志 + MDC TraceId
- **日志切面**：`LogAspect` 自动记录 Controller 出入参
- **链路追踪**：每个请求生成唯一 TraceId，贯穿整个调用链

### 13.2 监控指标

- **Actuator**：Spring Boot Actuator 提供健康检查和指标监控
- **关键指标**：接口响应时间、错误率、AI 调用成功率
- **异常告警**：可集成 Prometheus + Grafana 实现可视化监控

---

## 十四、项目结构

```
HealthAgent/
├── backend/
│   ├── src/main/java/com/healthagent/
│   │   ├── aspect/              # AOP 切面
│   │   ├── common/              # 公共类（Result、IntentType）
│   │   ├── config/              # 配置类
│   │   ├── controller/          # 控制器层
│   │   ├── dto/                 # 数据传输对象
│   │   ├── entity/              # 数据库实体
│   │   ├── interceptor/         # 拦截器
│   │   ├── mapper/              # 数据访问层
│   │   ├── service/             # 服务层
│   │   │   └── chat/            # 聊天客户端实现
│   │   └── HealthAgentApplication.java
│   │
│   ├── src/main/resources/
│   │   ├── sql/                 # 数据库脚本
│   │   └── application.yml      # 配置文件
│   │
│   └── pom.xml                  # Maven 配置
│
├── frontend/
│   ├── src/
│   │   ├── pages/               # 页面组件
│   │   ├── composables/         # 组合式 API
│   │   ├── utils/               # 工具函数
│   │   ├── types/               # TypeScript 类型
│   │   ├── router/              # 路由配置
│   │   └── components/          # 通用组件
│   │
│   ├── package.json
│   └── vite.config.ts
│
└── README.md
```

---

## 十五、未来规划

### 15.1 短期目标

- [ ] 完善错误处理和异常恢复机制
- [ ] 添加单元测试和集成测试
- [ ] 优化 AI 响应速度和准确率
- [ ] 实现多轮对话中的参数补全

### 15.2 中期目标

- [ ] 引入 Redis 缓存热点数据
- [ ] 实现用户画像和个性化推荐
- [ ] 支持语音通话功能
- [ ] 集成更多大模型（Claude、Gemini 等）

### 15.3 长期目标

- [ ] 微服务架构改造
- [ ] 容器化部署（Docker + K8s）
- [ ] 智能理赔功能
- [ ] 健康管理方案定制

---

*文档版本：v1.0*
*最后更新：2026-05-10*
