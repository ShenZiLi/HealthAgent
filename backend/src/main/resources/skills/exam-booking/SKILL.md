---
name: exam-booking
description: |
  HealthAgent 体检预约技能，用于查询和管理体检预约信息。
  
  使用场景：
  - 用户询问体检医院（"有哪些体检医院"、"体检中心列表"）
  - 用户查询体检套餐（"体检套餐有哪些"、"基础体检套餐"）
  - 用户查询体检计划（"什么时候可以体检"、"可预约日期"）
  - 用户预约体检（"我想预约体检"、"预约6月1日的体检"）
  - 用户查询预约记录（"我的体检预约"、"预约状态"）
  - 用户取消或修改预约
  
  触发关键词：体检、预约、医院、套餐、检查、examination、booking、health check
---

# Exam Booking 体检预约

HealthAgent 体检预约技能，提供体检医院查询、套餐查询、计划查询、预约管理等功能。

## 快速开始

### 前置条件

1. **HealthAgent 服务运行**: 确保 HealthAgent 后端服务在运行（默认 `http://localhost:8080`）
2. **认证凭证**: 需要有效的 JWT Token（通过登录获取）

### 获取 Token

```bash
# 使用脚本登录
python scripts/exam_api.py --login --base-url "http://localhost:8080" --username "your_username" --password "your_password"

# 或手动登录
curl -X POST http://localhost:8080/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"your_username","password":"your_password"}'
```

---

## 查询操作

### 查询医院列表

```bash
python scripts/exam_api.py --action hospitals --base-url "http://localhost:8080" --token "your_token"
```

用户请求示例：
- "有哪些体检医院"
- "体检中心列表"
- "查询医院"

### 查询体检套餐

```bash
python scripts/exam_api.py --action packages --base-url "http://localhost:8080" --token "your_token"
```

用户请求示例：
- "体检套餐有哪些"
- "基础体检套餐多少钱"
- "有什么检查项目"

### 查询体检计划

```bash
# 查询所有计划
python scripts/exam_api.py --action plans --base-url "http://localhost:8080" --token "your_token"

# 按医院筛选
python scripts/exam_api.py --action plans --hospital-id 1 --base-url "http://localhost:8080" --token "your_token"

# 按套餐筛选
python scripts/exam_api.py --action plans --package-id 2 --base-url "http://localhost:8080" --token "your_token"
```

用户请求示例：
- "什么时候可以体检"
- "6月份有哪些可预约日期"
- "北京体检中心的计划"

### 查询预约记录

```bash
python scripts/exam_api.py --action bookings --user-id "user001" --base-url "http://localhost:8080" --token "your_token"
```

用户请求示例：
- "我的体检预约"
- "查询预约状态"
- "预约记录"

---

## 预约操作

### 创建预约

**方式 1: 快捷参数**

```bash
python scripts/exam_api.py --action book \
  --user-id "user001" \
  --user-name "张三" \
  --hospital-id 1 \
  --package-id 1 \
  --exam-date "2024-06-01" \
  --phone "13800138000" \
  --base-url "http://localhost:8080" \
  --token "your_token"
```

**方式 2: JSON 数据**

```bash
python scripts/exam_api.py --action book \
  --booking-data '{"userId":"user001","userName":"张三","hospitalId":1,"packageId":1,"examinationDate":"2024-06-01","phone":"13800138000"}' \
  --base-url "http://localhost:8080" \
  --token "your_token"
```

用户请求示例：
- "我想预约体检"
- "预约北京体检中心6月1日的体检"
- "帮我预约基础套餐，日期是下周一"

### 更新预约

```bash
python scripts/exam_api.py --action update \
  --booking-id 1 \
  --exam-date "2024-06-02" \
  --exam-time "下午" \
  --base-url "http://localhost:8080" \
  --token "your_token"
```

用户请求示例：
- "修改预约日期到6月2日"
- "改一下体检时间"
- "预约改成下午"

### 取消预约

```bash
python scripts/exam_api.py --action cancel \
  --booking-id 1 \
  --base-url "http://localhost:8080" \
  --token "your_token"
```

用户请求示例：
- "取消我的体检预约"
- "不想体检了，取消预约"
- "取消预约号 BK001"

---

## 预约流程

### 完整预约流程

```
1. 查询医院列表 → 选择医院
       │
       ▼
2. 查询体检套餐 → 选择套餐
       │
       ▼
3. 查询体检计划 → 确认可预约日期
       │
       ▼
4. 创建预约 → 提交预约信息
       │
       ▼
5. 查询预约记录 → 确认预约状态
```

### 流程示例

**用户**: "我想预约体检"

**Agent 执行流程**:

1. 先查询医院列表：
   ```bash
   python scripts/exam_api.py --action hospitals --token $TOKEN
   ```
   返回: 北京健康体检中心、上海体检中心...

2. 用户选择医院后，查询套餐：
   ```bash
   python scripts/exam_api.py --action packages --token $TOKEN
   ```
   返回: 基础套餐(299元)、高级套餐(599元)...

3. 用户选择套餐后，查询计划确认日期：
   ```bash
   python scripts/exam_api.py --action plans --hospital-id 1 --package-id 1 --token $TOKEN
   ```
   返回: 可预约日期 2024-06-01, 2024-06-02...

4. 用户确认日期后，创建预约：
   ```bash
   python scripts/exam_api.py --action book \
     --user-id "user001" \
     --user-name "张三" \
     --hospital-id 1 \
     --package-id 1 \
     --exam-date "2024-06-01" \
     --token $TOKEN
   ```

---

## 参数提取指南

| 用户输入 | 提取参数 | 查询方式 |
|----------|----------|----------|
| "北京体检中心" | `hospitalId=1` | plans (筛选) |
| "基础套餐" | `packageId=1` | plans (筛选) |
| "6月1日" | `examDate=2024-06-01` | book |
| "上午" | `examTime=上午` | book (可选) |
| "用户张三" | `userName=张三` | book |

---

## 数据说明

### 预约状态

| 状态值 | 中文含义 | 说明 |
|--------|----------|------|
| `pending` | 待确认 | 预约已提交，等待确认 |
| `confirmed` | 已确认 | 预约已确认，可以体检 |
| `completed` | 已完成 | 体检已完成 |
| `cancelled` | 已取消 | 预约已取消 |

### 常用字段

| 字段 | 说明 |
|------|------|
| 医院 (hospitalName) | 体检机构名称 |
| 套餐 (packageName) | 体检套餐类型 |
| 价格 (price) | 套餐费用 |
| 检查项目 (items) | 套餐包含的检查内容 |
| 可预约日期 (availableDates) | 可选的体检日期 |
| 体检日期 (examinationDate) | 预约的具体日期 |
| 预约号 (bookingNo) | 预约的唯一标识 |

---

## API 文档

详细的 API 规范请参考：[references/api_schema.md](references/api_schema.md)

包含：
- 完整的 API 端点列表
- 请求/响应格式
- 数据结构定义
- 错误处理
- 预约流程说明

---

## 配置

### 默认配置

- Base URL: `http://localhost:8080`
- Token: 需通过登录获取

### 环境变量配置

```bash
export HEALTHAGENT_BASE_URL="http://localhost:8080"
export HEALTHAGENT_TOKEN="your_jwt_token"
```

---

## 常见问题

### Token 过期

Token 有效期 24 小时。过期后重新登录获取新 Token。

### 预约日期冲突

如果选择的日期已满，API 返回 409 错误。建议用户选择其他日期：

```bash
python scripts/exam_api.py --action plans --hospital-id 1 --token $TOKEN
```

### 服务未运行

确保 HealthAgent 后端服务在运行：

```bash
curl http://localhost:8080/actuator/health
```

### 网络问题

如果服务部署在其他地址，使用 `--base-url` 参数指定。