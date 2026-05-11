---
name: policy-query
description: |
  HealthAgent 保单查询技能，用于查询和管理保险保单信息。
  
  使用场景：
  - 用户询问保单信息（"查询保单"、"我的保险保单"、"保单号 POL001"）
  - 用户提供身份证号查询保单（"身份证号是 110101...，查保单"）
  - 用户提供投保人姓名查询保单（"投保人张三的保单"）
  - 用户需要新增、更新、删除保单
  - 用户需要分页浏览保单列表
  
  触发关键词：保单、保险、投保人、保额、保费、理赔、insurance、policy
---

# Policy Query 保单查询

HealthAgent 保单查询技能，提供保险保单信息的查询和管理功能。

## 快速开始

### 前置条件

1. **HealthAgent 服务运行**: 确保 HealthAgent 后端服务在运行（默认 `http://localhost:8080`）
2. **认证凭证**: 需要有效的 JWT Token（通过登录获取）

### 获取 Token

```bash
# 使用脚本登录
python scripts/policy_api.py --login --base-url "http://localhost:8080" --username "your_username" --password "your_password"

# 或手动登录
curl -X POST http://localhost:8080/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"your_username","password":"your_password"}'
```

将返回的 `accessToken` 用于后续查询。

---

## 查询操作

### 按保单号查询

```bash
python scripts/policy_api.py --action get-by-pol-no --pol-no "POL2024001" --base-url "http://localhost:8080" --token "your_token"
```

用户请求示例：
- "查询保单号 POL2024001 的信息"
- "保单 POL001 的详情"

### 按投保人姓名查询

```bash
python scripts/policy_api.py --action get-by-holder --holder-name "张三" --base-url "http://localhost:8080" --token "your_token"
```

用户请求示例：
- "查询投保人张三的所有保单"
- "张三有什么保险"

### 按身份证号查询

```bash
python scripts/policy_api.py --action get-by-id-card --id-card "110101199001011234" --base-url "http://localhost:8080" --token "your_token"
```

用户请求示例：
- "身份证号 110101199001011234 的保单信息"
- "我的身份证是...，查一下保单"

### 分页查询

```bash
python scripts/policy_api.py --action query-page --page 1 --size 10 --base-url "http://localhost:8080" --token "your_token"
```

用户请求示例：
- "显示所有保单"
- "保单列表第一页"

### 条件查询

```bash
python scripts/policy_api.py --action query --holder-name "张" --status "active" --base-url "http://localhost:8080" --token "your_token"
```

---

## 管理操作

### 新增保单

```bash
python scripts/policy_api.py --action create --policy-data '{"polNo":"POL2024002","policyHolderName":"李四","idCardNo":"110101199002022345","insuranceProduct":"意外险","sumInsured":300000.00,"premium":300.00,"effectiveDate":"2024-01-01","expiryDate":"2025-01-01","status":"active"}' --base-url "http://localhost:8080" --token "your_token"
```

用户请求示例：
- "新增保单，保单号 POL002，投保人李四..."

### 更新保单

```bash
python scripts/policy_api.py --action update --policy-data '{"polNo":"POL2024001","sumInsured":600000.00}' --base-url "http://localhost:8080" --token "your_token"
```

用户请求示例：
- "更新保单 POL001，保额改为 60 万"

### 删除保单

```bash
python scripts/policy_api.py --action delete --pol-no "POL2024001" --base-url "http://localhost:8080" --token "your_token"
```

用户请求示例：
- "删除保单 POL001"

---

## 工作流程

### 查询保单流程

1. **识别查询意图**: 用户提到保单、保险、投保人等关键词
2. **提取参数**: 从用户输入中提取查询参数（保单号、姓名、身份证号）
3. **获取认证**: 确保有有效的 Token
4. **执行查询**: 调用对应的查询接口
5. **格式化输出**: 将保单信息以易读格式呈现给用户

### 参数提取指南

| 用户输入 | 提取参数 | 查询方式 |
|----------|----------|----------|
| "保单号 POL001" | `polNo=POL001` | get-by-pol-no |
| "投保人张三" | `holderName=张三` | get-by-holder |
| "身份证号 110101..." | `idCard=110101...` | get-by-id-card |
| "所有保单" | 无参数 | query-all |
| "第2页保单" | `page=2` | query-page |

---

## 保单数据说明

### 保单状态

| 状态值 | 中文含义 |
|--------|----------|
| `active` | 有效 |
| `expired` | 已过期 |

### 保单字段

| 字段 | 说明 |
|------|------|
| 保单号 (polNo) | 保单的唯一标识 |
| 投保人 (policyHolderName) | 购买保险的人 |
| 身份证号 (idCardNo) | 投保人身份证号 |
| 保险产品 (insuranceProduct) | 保险类型名称 |
| 保额 (sumInsured) | 保险赔付上限金额 |
| 保费 (premium) | 购买保险的费用 |
| 生效日期 (effectiveDate) | 保险开始生效时间 |
| 到期日期 (expiryDate) | 保险结束时间 |
| 状态 (status) | 保单当前状态 |

---

## API 文档

详细的 API 规范请参考：[references/api_schema.md](references/api_schema.md)

包含：
- 完整的 API 端点列表
- 请求/响应格式
- 错误处理
- 认证机制

---

## 配置

### 默认配置

- Base URL: `http://localhost:8080`
- Token: 需通过登录获取

### 环境变量配置

可设置环境变量简化调用：

```bash
export HEALTHAGENT_BASE_URL="http://localhost:8080"
export HEALTHAGENT_TOKEN="your_jwt_token"
```

---

## 常见问题

### Token 过期

Token 有效期 24 小时。过期后使用 refreshToken 刷新或重新登录：

```bash
curl -X POST http://localhost:8080/auth/refresh \
  -H "Content-Type: application/json" \
  -d '{"refreshToken":"your_refresh_token"}'
```

### 服务未运行

确保 HealthAgent 后端服务在运行：

```bash
# 检查服务状态
curl http://localhost:8080/actuator/health
```

### 网络问题

如果 HealthAgent 部署在其他地址，使用 `--base-url` 参数指定：

```bash
python scripts/policy_api.py --action query-all --base-url "http://your-server:8080" --token "your_token"
```