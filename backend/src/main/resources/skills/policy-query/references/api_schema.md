# HealthAgent 保单 API 文档

## 概述

HealthAgent 是一个智能健康助手系统，提供保单查询、体检预约、健康咨询等功能。本文档描述保单查询相关的 API 接口。

## 基础信息

- **Base URL**: `http://localhost:8080` (默认)
- **认证方式**: JWT Bearer Token
- **内容类型**: `application/json`

## 认证

### 登录获取 Token

```http
POST /auth/login
Content-Type: application/json

{
  "username": "用户名",
  "password": "密码"
}
```

**响应**:
```json
{
  "code": 0,
  "message": "success",
  "data": {
    "accessToken": "eyJhbGciOiJIUzI1NiIs...",
    "refreshToken": "eyJhbGciOiJIUzI1NiIs...",
    "tokenType": "Bearer",
    "expiresIn": 86400000
  }
}
```

### 使用 Token

在所有需要认证的请求中添加 Authorization 头：

```http
Authorization: Bearer <accessToken>
```

---

## 保单 API

### 保单数据结构

| 字段 | 类型 | 说明 |
|------|------|------|
| `polNo` | String | 保单号 (主键) |
| `policyHolderName` | String | 投保人姓名 |
| `idCardNo` | String | 身份证号 |
| `insuranceProduct` | String | 保险产品名称 |
| `sumInsured` | Decimal | 保额 (元) |
| `premium` | Decimal | 保费 (元) |
| `effectiveDate` | Date | 生效日期 (YYYY-MM-DD) |
| `expiryDate` | Date | 到期日期 (YYYY-MM-DD) |
| `status` | String | 状态: `active` (有效) / `expired` (已过期) |
| `remark` | String | 备注 |

### 1. 按保单号查询

```http
GET /api/policy/{polNo}
Authorization: Bearer <token>
```

**示例**:
```http
GET /api/policy/POL2024001
```

**响应**:
```json
{
  "code": 0,
  "message": "success",
  "data": {
    "polNo": "POL2024001",
    "policyHolderName": "张三",
    "idCardNo": "110101199001011234",
    "insuranceProduct": "健康险",
    "sumInsured": 500000.00,
    "premium": 5000.00,
    "effectiveDate": "2024-01-01",
    "expiryDate": "2025-01-01",
    "status": "active"
  }
}
```

### 2. 按投保人姓名查询

```http
GET /api/policy/holder/{name}
Authorization: Bearer <token>
```

**示例**:
```http
GET /api/policy/holder/张三
```

**响应**: 返回该投保人的所有保单列表

### 3. 按身份证号查询

```http
GET /api/policy/idcard/{idCardNo}
Authorization: Bearer <token>
```

**示例**:
```http
GET /api/policy/idcard/110101199001011234
```

**响应**: 返回该身份证号对应的所有保单列表

### 4. 条件查询

```http
POST /api/policy/query
Authorization: Bearer <token>
Content-Type: application/json

{
  "polNo": "POL2024",        // 可选，支持模糊匹配
  "policyHolderName": "张",  // 可选，支持模糊匹配
  "idCardNo": "110101",      // 可选，支持模糊匹配
  "status": "active"         // 可选
}
```

### 5. 分页查询

```http
POST /api/policy/page
Authorization: Bearer <token>
Content-Type: application/json

{
  "page": 1,                 // 页码，从 1 开始
  "size": 10,                // 每页大小
  "polNo": "POL",            // 可选筛选条件
  "policyHolderName": "",    // 可选筛选条件
  "status": "active"         // 可选筛选条件
}
```

**响应**:
```json
{
  "code": 0,
  "message": "success",
  "data": {
    "content": [...],           // 保单列表
    "totalElements": 100,       // 总记录数
    "totalPages": 10,           // 总页数
    "number": 1,                // 当前页码
    "size": 10,                 // 每页大小
    "first": true,              // 是否首页
    "last": false               // 是否末页
  }
}
```

### 6. 查询所有保单

```http
GET /api/policy/all
Authorization: Bearer <token>
```

**注意**: 此接口返回所有保单，数据量大时建议使用分页查询。

### 7. 新增保单

```http
POST /api/policy
Authorization: Bearer <token>
Content-Type: application/json

{
  "polNo": "POL2024001",
  "policyHolderName": "张三",
  "idCardNo": "110101199001011234",
  "insuranceProduct": "健康险",
  "sumInsured": 500000.00,
  "premium": 5000.00,
  "effectiveDate": "2024-01-01",
  "expiryDate": "2025-01-01",
  "status": "active",
  "remark": "备注信息"
}
```

### 8. 更新保单

```http
PUT /api/policy
Authorization: Bearer <token>
Content-Type: application/json

{
  "polNo": "POL2024001",
  "policyHolderName": "张三",
  "idCardNo": "110101199001011234",
  "insuranceProduct": "健康险升级版",
  "sumInsured": 800000.00,
  "premium": 8000.00,
  "effectiveDate": "2024-01-01",
  "expiryDate": "2026-01-01",
  "status": "active"
}
```

### 9. 删除保单

```http
DELETE /api/policy/{polNo}
Authorization: Bearer <token>
```

---

## 错误响应

所有 API 在出错时返回统一格式：

```json
{
  "code": 400,
  "message": "错误描述",
  "data": null
}
```

### 常见错误码

| HTTP 状态码 | code | 说明 |
|-------------|------|------|
| 400 | 400 | 请求参数错误 |
| 401 | 401 | 未认证或 Token 过期 |
| 403 | 403 | 无权限访问 |
| 404 | 404 | 资源不存在 |
| 500 | 500 | 服务器内部错误 |

---

## 使用示例

### Python 调用示例

```python
import requests

# 1. 登录获取 Token
login_resp = requests.post('http://localhost:8080/auth/login', json={
    'username': 'admin',
    'password': 'password'
})
token = login_resp.json()['data']['accessToken']

# 2. 查询保单
headers = {'Authorization': f'Bearer {token}'}
policy_resp = requests.get('http://localhost:8080/api/policy/POL2024001', headers=headers)
print(policy_resp.json())
```

### curl 调用示例

```bash
# 登录
TOKEN=$(curl -s -X POST http://localhost:8080/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"admin","password":"password"}' | jq -r '.data.accessToken')

# 查询保单
curl -X GET http://localhost:8080/api/policy/POL2024001 \
  -H "Authorization: Bearer $TOKEN"
```

---

## 配置

### 环境变量

| 变量名 | 说明 | 默认值 |
|--------|------|--------|
| `HEALTHAGENT_BASE_URL` | API 基础 URL | `http://localhost:8080` |
| `HEALTHAGENT_TOKEN` | JWT Token (可选) | - |

### 配置文件

可在 `~/.config/healthagent/config.json` 中保存配置：

```json
{
  "baseUrl": "http://localhost:8080",
  "token": "your_jwt_token"
}
```