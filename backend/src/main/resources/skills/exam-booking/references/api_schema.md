# HealthAgent 体检预约 API 文档

## 概述

HealthAgent 体检预约模块提供医院查询、体检套餐查询、体检计划查询、预约管理等功能。

## 基础信息

- **Base URL**: `http://localhost:8080` (默认)
- **认证方式**: JWT Bearer Token
- **内容类型**: `application/json`

---

## 医院信息 API

### 获取医院列表

```http
GET /api/exam/hospitals
Authorization: Bearer <token>
```

**响应**:
```json
{
  "code": 0,
  "message": "success",
  "data": [
    {
      "id": 1,
      "hospitalCode": "H001",
      "hospitalName": "北京健康体检中心",
      "address": "北京市朝阳区xxx路xxx号",
      "contactPhone": "010-12345678",
      "status": "active"
    }
  ]
}
```

### 医院数据结构

| 字段 | 类型 | 说明 |
|------|------|------|
| `id` | Long | 医院 ID |
| `hospitalCode` | String | 医院编码 |
| `hospitalName` | String | 医院名称 |
| `address` | String | 医院地址 |
| `contactPhone` | String | 联系电话 |
| `status` | String | 状态: `active` (可用) / `inactive` (不可用) |

---

## 体检套餐 API

### 获取体检套餐列表

```http
GET /api/exam/packages
Authorization: Bearer <token>
```

**响应**:
```json
{
  "code": 0,
  "message": "success",
  "data": [
    {
      "id": 1,
      "packageCode": "PKG001",
      "packageName": "基础体检套餐",
      "description": "包含常规检查项目",
      "price": 299.00,
      "items": ["血常规", "尿常规", "肝功能", "肾功能", "心电图"],
      "status": "active"
    }
  ]
}
```

### 套餐数据结构

| 字段 | 类型 | 说明 |
|------|------|------|
| `id` | Long | 套餐 ID |
| `packageCode` | String | 套餐编码 |
| `packageName` | String | 套餐名称 |
| `description` | String | 套餐描述 |
| `price` | Decimal | 价格 (元) |
| `items` | JSON Array | 检查项目列表 |
| `status` | String | 状态: `active` / `inactive` |

---

## 体检计划 API

### 获取体检计划列表

```http
GET /api/exam/plans
Authorization: Bearer <token>
```

**可选筛选参数**:
- `hospitalId`: 按医院筛选
- `packageId`: 按套餐筛选

```http
GET /api/exam/plans?hospitalId=1&packageId=2
```

**响应**:
```json
{
  "code": 0,
  "message": "success",
  "data": [
    {
      "id": 1,
      "planName": "2024年6月体检计划",
      "hospitalId": 1,
      "hospitalName": "北京健康体检中心",
      "packageId": 1,
      "packageName": "基础体检套餐",
      "availableDates": ["2024-06-01", "2024-06-02", "2024-06-03"],
      "status": "active"
    }
  ]
}
```

### 计划数据结构

| 字段 | 类型 | 说明 |
|------|------|------|
| `id` | Long | 计划 ID |
| `planName` | String | 计划名称 |
| `hospitalId` | Long | 医院 ID |
| `hospitalName` | String | 医院名称 |
| `packageId` | Long | 套餐 ID |
| `packageName` | String | 套餐名称 |
| `availableDates` | JSON Array | 可预约日期列表 |
| `status` | String | 状态: `active` / `inactive` |

---

## 预约管理 API

### 获取用户预约记录

```http
GET /api/exam/bookings/{userId}
Authorization: Bearer <token>
```

**示例**:
```http
GET /api/exam/bookings/user001
```

**响应**:
```json
{
  "code": 0,
  "message": "success",
  "data": [
    {
      "id": 1,
      "bookingNo": "BK20240601001",
      "userId": "user001",
      "userName": "张三",
      "idCardNo": "110101199001011234",
      "phone": "13800138000",
      "hospitalId": 1,
      "hospitalName": "北京健康体检中心",
      "packageId": 1,
      "packageName": "基础体检套餐",
      "examinationDate": "2024-06-01",
      "examinationTime": "上午",
      "status": "confirmed",
      "notes": "备注信息",
      "createTime": "2024-05-15T10:30:00"
    }
  ]
}
```

### 创建体检预约

```http
POST /api/exam/bookings
Authorization: Bearer <token>
Content-Type: application/json

{
  "userId": "user001",
  "userName": "张三",
  "idCardNo": "110101199001011234",
  "phone": "13800138000",
  "hospitalId": 1,
  "packageId": 1,
  "examinationDate": "2024-06-01",
  "examinationTime": "上午",
  "notes": "备注信息"
}
```

**必填字段**:
- `userId`: 用户 ID
- `userName`: 用户姓名
- `hospitalId`: 医院 ID
- `packageId`: 套餐 ID
- `examinationDate`: 体检日期

**可选字段**:
- `idCardNo`: 身份证号
- `phone`: 联系电话
- `examinationTime`: 体检时段 (如 "上午"、"下午"、"09:00-10:00")
- `notes`: 备注

### 更新体检预约

```http
PUT /api/exam/bookings/{id}
Authorization: Bearer <token>
Content-Type: application/json

{
  "examinationDate": "2024-06-02",
  "examinationTime": "下午",
  "notes": "更新备注"
}
```

只传递需要更新的字段即可。

### 取消体检预约

```http
DELETE /api/exam/bookings/{id}
Authorization: Bearer <token>
```

**响应**:
```json
{
  "code": 0,
  "message": "预约已取消",
  "data": null
}
```

### 预约数据结构

| 字段 | 类型 | 说明 |
|------|------|------|
| `id` | Long | 预约 ID |
| `bookingNo` | String | 预约号 |
| `userId` | String | 用户 ID |
| `userName` | String | 用户姓名 |
| `idCardNo` | String | 身份证号 |
| `phone` | String | 联系电话 |
| `hospitalId` | Long | 医院 ID |
| `hospitalName` | String | 医院名称 |
| `packageId` | Long | 套餐 ID |
| `packageName` | String | 套餐名称 |
| `examinationDate` | Date | 体检日期 |
| `examinationTime` | String | 体检时段 |
| `status` | String | 状态 |
| `notes` | String | 备注 |
| `createTime` | DateTime | 创建时间 |

### 预约状态

| 状态值 | 中文含义 |
|--------|----------|
| `pending` | 待确认 |
| `confirmed` | 已确认 |
| `completed` | 已完成 |
| `cancelled` | 已取消 |

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
| 409 | 409 | 预约冲突 (日期已满等) |
| 500 | 500 | 服务器内部错误 |

---

## 预约流程

### 典型预约流程

1. **获取医院列表** → 选择医院
2. **获取体检套餐** → 选择套餐
3. **获取体检计划** → 确认可预约日期
4. **创建预约** → 提交预约信息
5. **查询预约记录** → 确认预约状态

### 流程示例

```bash
# 1. 查看医院列表
python exam_api.py --action hospitals --token $TOKEN

# 2. 查看套餐列表
python exam_api.py --action packages --token $TOKEN

# 3. 查看特定医院的计划
python exam_api.py --action plans --hospital-id 1 --token $TOKEN

# 4. 创建预约
python exam_api.py --action book \
  --user-id "user001" \
  --user-name "张三" \
  --hospital-id 1 \
  --package-id 1 \
  --exam-date "2024-06-01" \
  --phone "13800138000" \
  --token $TOKEN

# 5. 查询预约记录
python exam_api.py --action bookings --user-id "user001" --token $TOKEN
```

---

## 使用示例

### Python 调用

```python
import requests

BASE_URL = 'http://localhost:8080'
TOKEN = 'your_jwt_token'

headers = {'Authorization': f'Bearer {TOKEN}'}

# 获取医院列表
hospitals = requests.get(f'{BASE_URL}/api/exam/hospitals', headers=headers).json()
print(hospitals)

# 创建预约
booking_data = {
    'userId': 'user001',
    'userName': '张三',
    'hospitalId': 1,
    'packageId': 1,
    'examinationDate': '2024-06-01'
}
result = requests.post(f'{BASE_URL}/api/exam/bookings', 
                       headers=headers, 
                       json=booking_data).json()
print(result)
```

### curl 调用

```bash
# 获取医院列表
curl -X GET http://localhost:8080/api/exam/hospitals \
  -H "Authorization: Bearer $TOKEN"

# 创建预约
curl -X POST http://localhost:8080/api/exam/bookings \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"userId":"user001","userName":"张三","hospitalId":1,"packageId":1,"examinationDate":"2024-06-01"}'
```

---

## 配置

### 环境变量

| 变量名 | 说明 | 默认值 |
|--------|------|--------|
| `HEALTHAGENT_BASE_URL` | API 基础 URL | `http://localhost:8080` |
| `HEALTHAGENT_TOKEN` | JWT Token | - |

### 配置文件

可在 `~/.config/healthagent/config.json` 中保存配置：

```json
{
  "baseUrl": "http://localhost:8080",
  "token": "your_jwt_token"
}
```