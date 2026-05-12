# 技术规范文档 (SDD)

## 1. 项目概述

健康助手（HealthAgent）是一个智能健康服务平台，提供体检预约、保险查询和智能对话等功能。项目采用前后端分离架构，前端使用Vue 3构建，后端基于Spring Boot实现。

### 1.1 技术栈概览

| 层级 | 技术选型 | 版本要求 |
|------|----------|----------|
| 前端框架 | Vue 3 + TypeScript | ^3.4.15 / ~5.3.3 |
| 构建工具 | Vite | ^5.0.12 |
| UI框架 | Tailwind CSS | ^3.4.1 |
| 后端框架 | Spring Boot | 3.4.1 |
| 开发语言 | Java | 21 |
| ORM框架 | MyBatis-Plus | 3.5.5 |
| 数据库 | MySQL | 8.0+ |
| 缓存 | Redis | - |
| API文档 | SpringDoc OpenAPI | 2.3.0 |

## 2. 前端开发规范

### 2.1 项目结构

```
frontend/
├── src/
│   ├── assets/              # 静态资源
│   ├── components/           # Vue组件
│   ├── composables/          # 组合式函数
│   ├── lib/                  # 工具库
│   ├── pages/                # 页面组件
│   ├── router/               # 路由配置
│   ├── types/                # TypeScript类型定义
│   ├── utils/                 # 工具函数
│   ├── App.vue               # 根组件
│   └── main.ts               # 入口文件
├── index.html
├── vite.config.ts           # Vite配置
├── tsconfig.json            # TypeScript配置
├── tailwind.config.js       # Tailwind配置
└── package.json
```

### 2.2 TypeScript配置规范

```json
{
  "extends": "@vue/tsconfig/tsconfig.dom.json",
  "compilerOptions": {
    "strict": false,
    "noUnusedLocals": false,
    "noUnusedParameters": false,
    "baseUrl": "./",
    "paths": {
      "@/*": ["./src/*"]
    }
  }
}
```

**规范要求**：
- 启用路径别名 `@/*` 指向 `src/` 目录
- 关闭严格模式以提高开发效率
- 允许未使用的局部变量和参数

### 2.3 Vite构建配置

```typescript
// vite.config.ts
export default defineConfig({
  build: {
    sourcemap: 'hidden',
  },
  plugins: [
    vue(),
    Inspector(),              // 开发定位插件
    traeBadgePlugin({         // Trae标识插件
      variant: 'dark',
      position: 'bottom-right',
      prodOnly: true,
    }),
  ],
  resolve: {
    alias: {
      '@': path.resolve(__dirname, './src'),
    },
  },
  server: {
    proxy: {
      '/api': {
        target: 'http://localhost:8084',
        changeOrigin: true,
      },
    },
  },
})
```

**规范要求**：
- 生产构建使用hidden sourcemap
- API请求代理到后端 `http://localhost:8084`
- 路径别名使用 `@` 符号

### 2.4 样式规范

```javascript
// tailwind.config.js
export default {
  darkMode: "class",
  content: ["./index.html", "./src/**/*.{js,ts,vue}"],
  theme: {
    container: {
      center: true,
    },
    extend: {},
  },
  plugins: [],
}
```

**规范要求**：
- 使用CSS类名方式控制暗色模式
- 内容扫描范围包含所有Vue和TypeScript文件
- 使用Tailwind工具类进行样式开发

### 2.5 路由规范

```typescript
// router/index.ts
import { createRouter, createWebHistory } from 'vue-router'
import type { RouteRecordRaw } from 'vue-router'

const routes: RouteRecordRaw[] = [
  {
    path: '/login',
    name: 'Login',
    component: () => import('@/pages/LoginPage.vue'),
  },
  {
    path: '/dashboard',
    name: 'Dashboard',
    component: () => import('@/pages/DashboardPage.vue'),
    meta: { requiresAuth: true }
  },
]

const router = createRouter({
  history: createWebHistory(),
  routes,
})

export default router
```

**规范要求**：
- 使用懒加载方式导入页面组件
- 认证页面需设置 `meta.requiresAuth: true`
- 使用命名路由便于维护

### 2.6 API调用规范

```typescript
// utils/api.ts
const API_BASE_URL = import.meta.env.VITE_API_BASE_URL || '/api'

export async function request<T>(
  endpoint: string,
  options: RequestInit = {}
): Promise<T> {
  const token = localStorage.getItem('accessToken')
  
  const headers: HeadersInit = {
    'Content-Type': 'application/json',
    ...(token ? { 'Authorization': `Bearer ${token}` } : {}),
    ...options.headers,
  }

  const response = await fetch(`${API_BASE_URL}${endpoint}`, {
    ...options,
    headers,
  })

  const result = await response.json()
  
  if (result.code !== 200) {
    throw new Error(result.message)
  }
  
  return result.data
}
```

**规范要求**：
- 从环境变量读取API基础URL
- 自动携带Authorization头
- 统一处理响应结果和错误

### 2.7 类型定义规范

```typescript
// types/auth.ts
export interface LoginRequest {
  username: string
  password: string
}

export interface LoginResponse {
  token: string
  refreshToken: string
  user: {
    username: string
  }
}

export interface AuthState {
  isAuthenticated: boolean
  user: { username: string } | null
  accessToken: string | null
  refreshToken: string | null
}
```

**规范要求**：
- 类型文件按功能模块划分
- 接口命名使用PascalCase
- 属性使用camelCase

## 3. 后端开发规范

### 3.1 项目结构

```
backend/src/main/
├── java/com/healthagent/
│   ├── aspect/              # AOP切面
│   ├── common/              # 公共类（Result等）
│   ├── config/              # 配置类
│   ├── controller/          # 控制器层
│   ├── dto/                 # 数据传输对象
│   ├── entity/              # 实体类
│   ├── interceptor/         # 拦截器
│   ├── mapper/              # 数据访问层
│   └── service/             # 业务逻辑层
├── resources/
│   ├── mapper/              # MyBatis XML映射文件
│   ├── skills/              # 技能配置
│   ├── sql/                 # 数据库脚本
│   └── application.yml      # 应用配置
└── pom.xml
```

### 3.2 分层架构规范

```
┌─────────────────┐
│   Controller    │  请求处理、参数校验、响应封装
├─────────────────┤
│    Service      │  业务逻辑、事务管理
├─────────────────┤
│     Mapper      │  数据库操作（MyBatis-Plus）
├─────────────────┤
│     Entity      │  数据模型、数据库映射
└─────────────────┘
```

**规范要求**：
- Controller：处理HTTP请求，使用@RestController注解
- Service：处理业务逻辑，使用@Service注解
- Mapper：数据访问，使用@Mapper注解或MyBatis-Plus接口
- Entity：数据模型，使用@Data注解

### 3.3 统一响应格式

```java
// common/Result.java
@Data
public class Result<T> {
    private Integer code;
    private String message;
    private T data;
    
    public static final Integer SUCCESS_CODE = 200;
    public static final Integer ERROR_CODE = 500;
    public static final Integer UNAUTHORIZED_CODE = 401;
    
    public static <T> Result<T> success(T data) {
        Result<T> result = new Result<>();
        result.setCode(SUCCESS_CODE);
        result.setMessage("操作成功");
        result.setData(data);
        return result;
    }
    
    public static <T> Result<T> error(String message) {
        Result<T> result = new Result<>();
        result.setCode(ERROR_CODE);
        result.setMessage(message);
        return result;
    }
    
    public static <T> Result<T> unauthorized(String message) {
        Result<T> result = new Result<>();
        result.setCode(UNAUTHORIZED_CODE);
        result.setMessage(message);
        return result;
    }
}
```

**响应码规范**：
| 状态码 | 含义 | 使用场景 |
|--------|------|----------|
| 200 | 成功 | 正常业务处理成功 |
| 401 | 未认证 | Token无效或已过期 |
| 500 | 服务器错误 | 业务处理失败或系统异常 |

### 3.4 Controller规范

```java
// controller/AuthController.java
@Tag(name = "认证接口")
@RestController
@RequestMapping("/api/auth")
public class AuthController {
    
    @Autowired
    private AuthService authService;
    
    @Operation(summary = "用户登录")
    @PostMapping("/login")
    public Result<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        AuthService.AuthTokens tokens = authService.login(
            request.getUsername(), 
            request.getPassword()
        );
        if (tokens != null) {
            LoginResponse.UserInfo userInfo = buildUserInfo(request.getUsername());
            LoginResponse response = new LoginResponse(
                tokens.accessToken(), 
                tokens.refreshToken(), 
                userInfo
            );
            return Result.success(response);
        }
        return Result.error("用户名或密码错误");
    }
}
```

**规范要求**：
- 使用Swagger注解@Tag和@Operation描述API
- 参数使用@Valid进行校验
- 路径映射使用@RestController和@RequestMapping
- 私有方法用于构建响应对象

### 3.5 DTO规范

```java
// dto/LoginRequest.java
@Data
public class LoginRequest {
    
    @NotBlank(message = "用户名不能为空")
    private String username;
    
    @NotBlank(message = "密码不能为空")
    private String password;
}
```

**规范要求**：
- DTO类使用@Data注解（Lombok）
- 使用Jakarta Validation注解进行参数校验
- DTO命名以功能或用途结尾（如Request/Response/DTO）

### 3.6 Entity规范

```java
// entity/ExaminationBookingEntity.java
@Data
@TableName("examination_booking")
public class ExaminationBookingEntity {
    
    @TableId(type = IdType.AUTO)
    private Long id;
    
    @TableField("booking_no")
    private String bookingNo;
    
    @TableField(value = "create_time", fill = FieldFill.INSERT)
    private LocalDateTime createTime;
    
    @TableField(value = "update_time", fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
    
    @TableLogic
    @TableField("deleted")
    private Integer deleted;
}
```

**规范要求**：
- 使用@TableName指定数据库表名
- 字段映射使用@TableField注解，驼峰转下划线
- 主键使用@TableId配置自增
- 使用@TableLogic配置逻辑删除
- 使用FieldFill自动填充创建和更新时间

### 3.7 Mapper规范

```java
// mapper/ExaminationBookingMapper.java
@Mapper
public interface ExaminationBookingMapper 
    extends BaseMapper<ExaminationBookingEntity> {
}
```

**规范要求**：
- Mapper接口继承MyBatis-Plus的BaseMapper
- 使用@Mapper注解或@MapperScan配置
- 复杂查询可在resources/mapper/*.xml中定义

### 3.8 Service规范

```java
// service/AuthService.java
@Service
public class AuthService {
    
    @Value("${healthagent.default-username:}")
    private String defaultUsername;
    
    @Autowired
    private JwtSecretProvider jwtSecretProvider;
    
    public AuthTokens login(String username, String password) {
        if (defaultUsername != null && !defaultUsername.isEmpty()
            && defaultPassword != null && !defaultPassword.isEmpty()) {
            if (defaultUsername.equals(username) && defaultPassword.equals(password)) {
                return issueTokens(username);
            }
        }
        return null;
    }
    
    public record AuthTokens(String accessToken, String refreshToken, String username) {
    }
}
```

**规范要求**：
- 使用@Service注解
- 配置值使用@Value从application.yml读取
- 使用Lombok简化代码
- 内部类或record用于返回多个值

### 3.9 配置管理规范

```yaml
# application.yml
server:
  port: ${SERVER_PORT:8084}

spring:
  application:
    name: HealthAgent
  config:
    import:
      - optional:file:.env[.properties]
      - optional:file:backend/.env[.properties]
  data:
    redis:
      host: ${REDIS_HOST:localhost}
      port: ${REDIS_PORT:6379}
      password: ${REDIS_PASSWORD:}
  datasource:
    url: jdbc:mysql://${MYSQL_HOST:localhost}:${MYSQL_PORT:3306}/${MYSQL_DATABASE:healthagent}?useUnicode=true&characterEncoding=utf-8&useSSL=false&serverTimezone=Asia/Shanghai
    username: ${MYSQL_USERNAME:root}
    password: ${MYSQL_PASSWORD:123456}

healthagent:
  auth:
    jwt-secret: ${JWT_SECRET:}
    access-token-expiration-minutes: 120
    refresh-token-expiration-hours: 168
    interceptor-enabled: ${AUTH_INTERCEPTOR_ENABLED:false}
```

**规范要求**：
- 敏感配置使用环境变量，环境变量未设置时使用默认值
- 配置前缀统一使用项目名（如healthagent）
- 数据库连接添加时区和字符集参数

## 4. 安全规范

### 4.1 JWT认证机制

```
┌──────────┐     ┌──────────┐     ┌──────────┐
│  Client  │────▶│  Server  │────▶│   Redis  │
└──────────┘     └──────────┘     └──────────┘
                      │
                      ▼
              ┌──────────────┐
              │ JWT Access   │
              │ Token        │
              │ (2小时有效期) │
              └──────────────┘
                      │
                      ▼
              ┌──────────────┐
              │ UUID Refresh │
              │ Token        │
              │ (7天有效期)   │
              └──────────────┘
```

**Token规范**：
- Access Token：JWT格式，包含用户名和过期时间，有效期2小时
- Refresh Token：UUID格式，存储在Redis中，有效期7天
- TokenType标识：claims中包含tokenType字段区分token类型

### 4.2 认证拦截器

```java
// interceptor/AuthInterceptor.java
@Component
public class AuthInterceptor implements HandlerInterceptor {
    
    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        if ("OPTIONS".equals(request.getMethod())) {
            return true;
        }
        
        String token = request.getHeader("Authorization");
        if (token != null && token.startsWith("Bearer ")) {
            token = token.substring(7);
        }
        
        if (token == null || !authService.validateToken(token)) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setContentType("application/json;charset=UTF-8");
            response.getWriter().write("{\"code\":401,\"message\":\"未认证或认证已过期\"}");
            return false;
        }
        
        String username = authService.getUsernameByToken(token);
        request.setAttribute("username", username);
        
        return true;
    }
}
```

**规范要求**：
- 放行OPTIONS预检请求
- 从Authorization头提取Bearer Token
- Token无效返回401状态码
- 认证成功后存储用户名到请求属性

### 4.3 JWT密钥安全

```java
// config/JwtSecretProvider.java
private static final Set<String> WEAK_JWT_SECRETS = Set.of(
    "healthagent-one-jwt-secret-key-change-me-to-a-long-random-string",
    "secret",
    "password",
    "123456"
);

private String generateRandomSecret() {
    byte[] secretBytes = new byte[48];
    new SecureRandom().nextBytes(secretBytes);
    return Base64.getUrlEncoder().withoutPadding().encodeToString(secretBytes);
}
```

**规范要求**：
- 自动检测弱密钥
- 本地开发环境自动生成强密钥
- 生产环境必须显式配置安全的JWT密钥
- 密钥长度至少48字节

## 5. API规范

### 5.1 认证接口

| 方法 | 路径 | 描述 | 认证 |
|------|------|------|------|
| POST | /api/auth/login | 用户登录 | 否 |
| POST | /api/auth/logout | 用户登出 | 否 |
| POST | /api/auth/refresh | 刷新Token | 否 |
| GET | /api/auth/current | 获取当前用户 | 是 |

### 5.2 请求响应示例

**登录请求**：
```json
POST /api/auth/login
{
  "username": "admin",
  "password": "admin123"
}
```

**登录响应**：
```json
{
  "code": 200,
  "message": "操作成功",
  "data": {
    "token": "eyJhbGciOiJIUzI1NiJ9...",
    "refreshToken": "a1b2c3d4e5f6...",
    "user": {
      "username": "admin"
    }
  }
}
```

### 5.3 错误响应示例

```json
{
  "code": 401,
  "message": "用户名或密码错误"
}
```

## 6. 数据库规范

### 6.1 表结构示例

```sql
CREATE TABLE examination_booking (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    booking_no VARCHAR(64) NOT NULL COMMENT '预约编号',
    hospital_id BIGINT NOT NULL COMMENT '医院ID',
    package_id BIGINT NOT NULL COMMENT '体检套餐ID',
    schedule_date DATE NOT NULL COMMENT '预约日期',
    user_id VARCHAR(64) NOT NULL COMMENT '用户ID',
    booker_name VARCHAR(64) NOT NULL COMMENT '预约人姓名',
    booker_phone VARCHAR(32) NOT NULL COMMENT '预约人电话',
    id_card_no VARCHAR(32) COMMENT '身份证号',
    notes TEXT COMMENT '备注',
    status VARCHAR(32) NOT NULL DEFAULT 'pending' COMMENT '状态',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted TINYINT DEFAULT 0 COMMENT '逻辑删除',
    INDEX idx_user_id (user_id),
    INDEX idx_hospital_id (hospital_id),
    INDEX idx_schedule_date (schedule_date)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='体检预约表';
```

### 6.2 字段命名规范

- 表名和字段名使用小写字母
- 多个单词用下划线分隔
- 主键统一命名为 `id`
- 时间字段：`create_time`、`update_time`
- 逻辑删除字段：`deleted`
- 索引命名：`idx_字段名`

### 6.3 MyBatis-Plus配置

```yaml
mybatis-plus:
  mapper-locations: classpath*:/mapper/**/*.xml
  type-aliases-package: com.healthagent.entity
  configuration:
    map-underscore-to-camel-case: true
    cache-enabled: false
  global-config:
    db-config:
      id-type: auto
      logic-delete-field: deleted
      logic-delete-value: 1
      logic-not-delete-value: 0
```

## 7. 日志规范

### 7.1 日志配置

```yaml
logging:
  pattern:
    console: "%d{yyyy-MM-dd HH:mm:ss.SSS} %highlight([%-5level]) [%yellow(%X{traceId})] [%thread] %cyan([%logger{50}]) - %msg%n"
  level:
    root: INFO
```

**日志格式**：
- 时间戳：`yyyy-MM-dd HH:mm:ss.SSS`
- 日志级别：ERROR/WARN/INFO/DEBUG
- TraceId：用于链路追踪
- 线程名：标识请求线程
- Logger名：类全限定名
- 消息内容：具体日志信息

### 7.2 日志级别使用

| 级别 | 使用场景 |
|------|----------|
| ERROR | 异常捕获、错误堆栈 |
| WARN | 业务警告、潜在问题 |
| INFO | 业务流程、重要节点 |
| DEBUG | 开发调试、详细流程 |

## 8. 开发工具配置

### 8.1 Java版本

```bash
# .java-version
21
```

### 8.2 前端依赖脚本

```json
{
  "scripts": {
    "dev": "vite",
    "build": "vue-tsc -b && vite build",
    "preview": "vite preview",
    "check": "vue-tsc -b",
    "lint": "eslint . --ext .ts,.vue",
    "lint:fix": "eslint . --ext .ts,.vue --fix"
  }
}
```

### 8.3 后端构建命令

```bash
# 开发模式
./mvnw spring-boot:run

# 打包
./mvnw clean package

# 运行测试
./mvnw test
```

## 9. 环境变量规范

### 9.1 前端环境变量

```bash
# .env
VITE_API_BASE_URL=/api
```

### 9.2 后端环境变量

```bash
# backend/.env
SERVER_PORT=8084
MYSQL_HOST=localhost
MYSQL_PORT=3306
MYSQL_DATABASE=healthagent
MYSQL_USERNAME=root
MYSQL_PASSWORD=123456
REDIS_HOST=localhost
REDIS_PORT=6379
REDIS_PASSWORD=
JWT_SECRET=your-production-secret-key-here
AUTH_INTERCEPTOR_ENABLED=true
```

## 10. Trae集成规范

### 10.1 Trae配置文件

项目已配置Trae相关插件：

```json
// package.json
{
  "devDependencies": {
    "vite-plugin-trae-solo-badge": "^1.0.0",
    "unplugin-vue-dev-locator": "^1.0.0"
  }
}
```

### 10.2 Trae使用建议

1. **代码导航**：使用Trae的代码跳转功能快速定位类和方法
2. **智能补全**：利用Trae的AI补全提升编码效率
3. **错误检测**：Trae可帮助发现潜在的代码问题
4. **文档生成**：使用SDD规范文档辅助理解项目结构

### 10.3 规范文档维护

- 本文档存放于 `.trae/documents/sdd.md`
- 随着项目演进持续更新规范内容
- 重要变更需同步更新本文档

---

**文档版本**：v1.0  
**最后更新**：2026-05-12  
**维护者**：HealthAgent Team
