# Skill: 保单查询

## 基本信息

- **名称**: policy_query
- **版本**: 1.0.0
- **描述**: 查询用户保单信息，并按固定格式返回
- **触发词**: 查询保单, 我的保险, 保单信息, 保险查询, 有哪些保险, 保险状态

---

## 输入参数

| 参数名 | 类型 | 必填 | 描述 |
|--------|------|------|------|
| userId | string | 是 | 用户ID |
| policyNo | string | 否 | 保单号，不填则返回所有保单 |
| status | string | 否 | 状态筛选：active/expired/all，默认active |

---

## 输出格式

```json
{
  "success": true,
  "policyList": [
    {
      "policyId": "POL****01",
      "policyName": "健康医疗保险",
      "status": "生效中",
      "coverage": "500,000.00",
      "premium": "3,650.00",
      "effectiveDate": "2023-11-02",
      "expiryDate": "2024-11-02",
      "insuranceCompany": "平安保险"
    }
  ],
  "totalCount": 1,
  "message": "查询成功"
}
```

### 输出字段说明

| 字段 | 类型 | 描述 |
|------|------|------|
| success | boolean | 查询是否成功 |
| policyList | array | 保单列表 |
| policyId | string | 保单号（已脱敏） |
| policyName | string | 产品名称 |
| status | string | 状态：生效中/已过期/待生效 |
| coverage | string | 保障额度（格式化） |
| premium | string | 年缴保费（格式化） |
| effectiveDate | string | 生效日期 |
| expiryDate | string | 到期日期，终身险显示"终身" |
| insuranceCompany | string | 保险公司 |
| totalCount | integer | 保单总数 |
| message | string | 提示信息 |

---

## 敏感数据脱敏规则

| 字段 | 规则 | 示例 |
|------|------|------|
| policyId | 保留前2+后2位 | `POL****01` |
| idCardNo | 保留前6+后4位 | `110101********1234` |
| phone | 保留前3+后4位 | `138****5678` |
| bankAccount | 保留前4+后4位 | `6225****1234` |
| email | 隐藏中间部分 | `z****@example.com` |
| policyHolderName | 只显示姓 | `张*` |

---

## 错误处理

| 错误码 | 描述 |
|--------|------|
| USER_NOT_FOUND | 未找到该用户的保单信息 |
| NO_ACTIVE_POLICY | 该用户暂无有效保单 |
| SYSTEM_ERROR | 系统繁忙，请稍后再试 |

---

## 使用示例

### 输入

```
用户: 查询我的保单
userId: user001
```

### 输出

```
✅ 查询成功

您共有 2 份有效保单：

【保单1】
• 保单号：POL****01
• 产品名称：健康医疗保险
• 保险公司：平安保险
• 保障额度：500,000.00元
• 年缴保费：3,650.00元
• 生效日期：2023-11-02
• 到期日期：2024-11-02

【保单2】
• 保单号：PO****02
• 产品名称：终身寿险
• 保险公司：中国人寿
• 保障额度：1,000,000.00元
• 年缴保费：12,000.00元
• 生效日期：2023-05-12
• 保障期限：终身
```
