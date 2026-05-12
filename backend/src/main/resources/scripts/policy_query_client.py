#!/usr/bin/env python3
"""
HealthAgent 保单查询外部接口客户端

用于对外发起网络请求查询保单信息。接口和出入参均为 Mock 数据。
实际对接时替换 BASE_URL 和移除 Mock 开关即可。

用法:
    python policy_query_client.py --action list                  # 查询所有保单
    python policy_query_client.py --action get --pol-no POL001   # 按保单号查询
    python policy_query_client.py --action holder --name 张三     # 按投保人姓名查询
    python policy_query_client.py --action idcard --id 110101199001011234  # 按身份证查询
    python policy_query_client.py --action query --status active  # 条件查询
    python policy_query_client.py --action page --page 1 --size 5 # 分页查询
    python policy_query_client.py --action create                 # 新增保单(Mock)
    python policy_query_client.py --action update                 # 更新保单(Mock)
    python policy_query_client.py --action delete --pol-no POL001 # 删除保单(Mock)

环境变量:
    HEALTHAGENT_BASE_URL  - API 基础地址 (默认: http://localhost:8084)
    HEALTHAGENT_TOKEN     - JWT Token (默认: 空)
"""

import argparse
import json
import os
import sys
import time
from datetime import datetime, date
from typing import Optional
from urllib.request import Request, urlopen
from urllib.error import URLError, HTTPError
from urllib.parse import urlencode

# ============================================================
# 配置
# ============================================================

BASE_URL = os.environ.get("HEALTHAGENT_BASE_URL", "http://localhost:8084")
TOKEN = os.environ.get("HEALTHAGENT_TOKEN", "")

# Mock 开关: True=使用本地 Mock 数据, False=发起真实 HTTP 请求
MOCK_MODE = True

# ============================================================
# Mock 数据
# ============================================================

MOCK_POLICIES = [
    {
        "polNo": "POL2024001",
        "policyHolderName": "张三",
        "insuredName": "张三",
        "idCardNo": "110101199001011234",
        "insuranceCompany": "中国人寿",
        "productName": "康宁终身重大疾病保险",
        "insuranceType": "健康险",
        "premiumAmount": 5000.00,
        "insuredAmount": 500000.00,
        "status": "active",
        "effectiveDate": "2024-01-01T00:00:00",
        "expiryDate": "2025-01-01T00:00:00",
    },
    {
        "polNo": "POL2024002",
        "policyHolderName": "张三",
        "insuredName": "李四",
        "idCardNo": "110101199001011234",
        "insuranceCompany": "中国平安",
        "productName": "平安福重疾险",
        "insuranceType": "健康险",
        "premiumAmount": 8000.00,
        "insuredAmount": 800000.00,
        "status": "active",
        "effectiveDate": "2024-03-15T00:00:00",
        "expiryDate": "2025-03-15T00:00:00",
    },
    {
        "polNo": "POL2024003",
        "policyHolderName": "王五",
        "insuredName": "王五",
        "idCardNo": "320102198805052345",
        "insuranceCompany": "太平洋保险",
        "productName": "金佑人生寿险",
        "insuranceType": "寿险",
        "premiumAmount": 12000.00,
        "insuredAmount": 1000000.00,
        "status": "active",
        "effectiveDate": "2024-06-01T00:00:00",
        "expiryDate": "2034-06-01T00:00:00",
    },
    {
        "polNo": "POL2024004",
        "policyHolderName": "赵六",
        "insuredName": "赵六",
        "idCardNo": "440103199512126789",
        "insuranceCompany": "新华保险",
        "productName": "健康无忧重大疾病保险",
        "insuranceType": "健康险",
        "premiumAmount": 6500.00,
        "insuredAmount": 600000.00,
        "status": "expired",
        "effectiveDate": "2023-01-01T00:00:00",
        "expiryDate": "2024-01-01T00:00:00",
    },
    {
        "polNo": "POL2024005",
        "policyHolderName": "张三",
        "insuredName": "张小三",
        "idCardNo": "110101199001011234",
        "insuranceCompany": "泰康人寿",
        "productName": "全能保意外险",
        "insuranceType": "意外险",
        "premiumAmount": 300.00,
        "insuredAmount": 200000.00,
        "status": "active",
        "effectiveDate": "2024-09-01T00:00:00",
        "expiryDate": "2025-09-01T00:00:00",
    },
    {
        "polNo": "POL2024006",
        "policyHolderName": "陈七",
        "insuredName": "陈七",
        "idCardNo": "510104200003034567",
        "insuranceCompany": "中国人保",
        "productName": "关爱e生医疗保险",
        "insuranceType": "医疗险",
        "premiumAmount": 1500.00,
        "insuredAmount": 300000.00,
        "status": "active",
        "effectiveDate": "2024-07-01T00:00:00",
        "expiryDate": "2025-07-01T00:00:00",
    },
    {
        "polNo": "POL2024007",
        "policyHolderName": "王五",
        "insuredName": "王小五",
        "idCardNo": "320102198805052345",
        "insuranceCompany": "中国太平",
        "productName": "福禄双至终身寿险",
        "insuranceType": "寿险",
        "premiumAmount": 15000.00,
        "insuredAmount": 1500000.00,
        "status": "cancelled",
        "effectiveDate": "2023-04-01T00:00:00",
        "expiryDate": "2053-04-01T00:00:00",
    },
]

MOCK_CREATE_RESPONSE = {
    "polNo": "POL2024008",
    "policyHolderName": "新投保人",
    "insuredName": "新被保人",
    "idCardNo": "330102199707078901",
    "insuranceCompany": "阳光保险",
    "productName": "阳光重疾险",
    "insuranceType": "健康险",
    "premiumAmount": 4500.00,
    "insuredAmount": 450000.00,
    "status": "active",
    "effectiveDate": datetime.now().strftime("%Y-%m-%dT00:00:00"),
    "expiryDate": f"{datetime.now().year + 1}-{datetime.now().strftime('%m-%d')}T00:00:00",
}

MOCK_LOGIN_RESPONSE = {
    "code": 200,
    "message": "success",
    "data": {
        "token": "mock_access_token_eyJhbGciOiJIUzI1NiJ9.mock_payload.mock_signature",
        "refreshToken": "mock-refresh-uuid-550e8400-e29b-41d4-a716-446655440000",
        "user": {
            "username": "admin",
            "idCardNo": "110101199001011234",
            "realName": "张三",
        },
    },
}


# ============================================================
# HTTP 客户端
# ============================================================

class PolicyClient:
    """保单查询 HTTP 客户端"""

    def __init__(self, base_url: str = BASE_URL, token: str = TOKEN):
        self.base_url = base_url.rstrip("/")
        self.token = token

    def _headers(self, content_type: Optional[str] = None) -> dict:
        """构建请求头"""
        h = {}
        if self.token:
            h["Authorization"] = f"Bearer {self.token}"
        if content_type:
            h["Content-Type"] = content_type
        return h

    def _request(self, method: str, path: str, body: Optional[dict] = None) -> dict:
        """发起 HTTP 请求"""
        url = f"{self.base_url}{path}"
        headers = self._headers("application/json" if body else None)

        data = json.dumps(body).encode("utf-8") if body else None
        req = Request(url, data=data, headers=headers, method=method)

        try:
            with urlopen(req, timeout=30) as resp:
                return json.loads(resp.read().decode("utf-8"))
        except HTTPError as e:
            error_body = e.read().decode("utf-8", errors="replace")
            try:
                return json.loads(error_body)
            except json.JSONDecodeError:
                return {"code": e.code, "message": f"HTTP {e.code}: {e.reason}", "data": None}
        except URLError as e:
            return {"code": 500, "message": f"连接失败: {e.reason}", "data": None}

    # ---- 认证 ----

    def login(self, username: str, password: str) -> dict:
        """登录获取 Token"""
        return self._request("POST", "/api/auth/login", {
            "username": username,
            "password": password,
        })

    # ---- 查询 ----

    def get_by_pol_no(self, pol_no: str) -> dict:
        """按保单号查询"""
        return self._request("GET", f"/api/policy/{pol_no}")

    def get_by_holder(self, name: str) -> dict:
        """按投保人姓名查询"""
        return self._request("GET", f"/api/policy/holder/{name}")

    def get_by_idcard(self, id_card: str) -> dict:
        """按身份证号查询"""
        return self._request("GET", f"/api/policy/idcard/{id_card}")

    def get_all(self) -> dict:
        """查询所有保单"""
        return self._request("GET", "/api/policy/all")

    def query(self, **conditions) -> dict:
        """条件查询"""
        return self._request("POST", "/api/policy/query", conditions)

    def page(self, page_num: int = 1, page_size: int = 10, **conditions) -> dict:
        """分页查询"""
        body = {"pageNum": page_num, "pageSize": page_size, **conditions}
        return self._request("POST", "/api/policy/page", body)

    # ---- 写操作 ----

    def create(self, policy: dict) -> dict:
        """新增保单"""
        return self._request("POST", "/api/policy", policy)

    def update(self, policy: dict) -> dict:
        """更新保单"""
        return self._request("PUT", "/api/policy", policy)

    def delete(self, pol_no: str) -> dict:
        """删除保单（逻辑删除）"""
        return self._request("DELETE", f"/api/policy/{pol_no}")


# ============================================================
# Mock 客户端
# ============================================================

class MockPolicyClient:
    """Mock 保单客户端，模拟 HTTP 响应，不发起真实网络请求"""

    def __init__(self, base_url: str = BASE_URL, token: str = TOKEN):
        self.base_url = base_url
        self.token = token
        self._policies = [dict(p) for p in MOCK_POLICIES]  # 深拷贝

    def _success(self, data) -> dict:
        return {"code": 200, "message": "success", "data": data}

    def _error(self, code: int, msg: str) -> dict:
        return {"code": code, "message": msg, "data": None}

    def _mask_idcard(self, id_card: str) -> str:
        """脱敏身份证号"""
        if len(id_card) >= 15:
            return id_card[:6] + "********" + id_card[-4:]
        return id_card

    def _mask_policy(self, policy: dict) -> dict:
        """脱敏保单信息"""
        masked = dict(policy)
        if masked.get("idCardNo"):
            masked["idCardNo"] = self._mask_idcard(masked["idCardNo"])
        return masked

    def login(self, username: str, password: str) -> dict:
        """Mock 登录"""
        time.sleep(0.1)  # 模拟网络延迟
        if username == "admin" and password == "admin123":
            return MOCK_LOGIN_RESPONSE
        return self._error(401, "用户名或密码错误")

    def get_by_pol_no(self, pol_no: str) -> dict:
        """Mock 按保单号查询"""
        time.sleep(0.05)
        for p in self._policies:
            if p["polNo"] == pol_no:
                return self._success(self._mask_policy(p))
        return self._error(404, f"保单不存在: {pol_no}")

    def get_by_holder(self, name: str) -> dict:
        """Mock 按投保人姓名查询"""
        time.sleep(0.05)
        results = [self._mask_policy(p) for p in self._policies if name in p["policyHolderName"]]
        return self._success(results)

    def get_by_idcard(self, id_card: str) -> dict:
        """Mock 按身份证号查询"""
        time.sleep(0.05)
        results = [self._mask_policy(p) for p in self._policies if p["idCardNo"] == id_card]
        return self._success(results)

    def get_all(self) -> dict:
        """Mock 查询所有保单"""
        time.sleep(0.1)
        return self._success([self._mask_policy(p) for p in self._policies])

    def query(self, **conditions) -> dict:
        """Mock 条件查询"""
        time.sleep(0.05)
        results = self._policies
        if "polNo" in conditions and conditions["polNo"]:
            results = [p for p in results if conditions["polNo"] in p["polNo"]]
        if "policyHolderName" in conditions and conditions["policyHolderName"]:
            results = [p for p in results if conditions["policyHolderName"] in p["policyHolderName"]]
        if "idCardNo" in conditions and conditions["idCardNo"]:
            results = [p for p in results if conditions["idCardNo"] in p["idCardNo"]]
        if "status" in conditions and conditions["status"]:
            results = [p for p in results if p["status"] == conditions["status"]]
        if "insuranceType" in conditions and conditions["insuranceType"]:
            results = [p for p in results if p["insuranceType"] == conditions["insuranceType"]]
        return self._success([self._mask_policy(p) for p in results])

    def page(self, page_num: int = 1, page_size: int = 10, **conditions) -> dict:
        """Mock 分页查询"""
        time.sleep(0.05)
        # 先按条件过滤
        all_results = self.query(**conditions)["data"]
        total = len(all_results)
        start = (page_num - 1) * page_size
        end = start + page_size
        page_data = all_results[start:end]
        return self._success({
            "content": page_data,
            "totalElements": total,
            "totalPages": max(1, (total + page_size - 1) // page_size),
            "number": page_num,
            "size": page_size,
            "first": page_num == 1,
            "last": end >= total,
        })

    def create(self, policy: dict) -> dict:
        """Mock 新增保单"""
        time.sleep(0.05)
        new_policy = dict(MOCK_CREATE_RESPONSE)
        new_policy.update(policy)
        new_policy["polNo"] = f"POL{datetime.now().strftime('%Y%m%d%H%M%S')}"
        self._policies.append(new_policy)
        return self._success(self._mask_policy(new_policy))

    def update(self, policy: dict) -> dict:
        """Mock 更新保单"""
        time.sleep(0.05)
        pol_no = policy.get("polNo", "")
        for i, p in enumerate(self._policies):
            if p["polNo"] == pol_no:
                self._policies[i].update(policy)
                return self._success(self._mask_policy(self._policies[i]))
        return self._error(404, f"保单不存在: {pol_no}")

    def delete(self, pol_no: str) -> dict:
        """Mock 删除保单"""
        time.sleep(0.05)
        for i, p in enumerate(self._policies):
            if p["polNo"] == pol_no:
                self._policies[i]["status"] = "cancelled"
                return self._success(f"保单 {pol_no} 已逻辑删除")
        return self._error(404, f"保单不存在: {pol_no}")


# ============================================================
# CLI 入口
# ============================================================

def get_client() -> PolicyClient | MockPolicyClient:
    """获取客户端实例"""
    if MOCK_MODE:
        return MockPolicyClient(BASE_URL, TOKEN)
    return PolicyClient(BASE_URL, TOKEN)


def print_json(data: dict):
    """格式化输出 JSON"""
    print(json.dumps(data, ensure_ascii=False, indent=2))


def main():
    parser = argparse.ArgumentParser(
        description="HealthAgent 保单查询客户端",
        formatter_class=argparse.RawDescriptionHelpFormatter,
        epilog="""
示例:
  python policy_query_client.py --action list
  python policy_query_client.py --action get --pol-no POL2024001
  python policy_query_client.py --action holder --name 张三
  python policy_query_client.py --action idcard --id 110101199001011234
  python policy_query_client.py --action query --status active --insuranceType 健康险
  python policy_query_client.py --action page --page 1 --size 3
  python policy_query_client.py --action create --data '{"policyHolderName":"测试"}'
  python policy_query_client.py --action update --data '{"polNo":"POL2024001","status":"expired"}'
  python policy_query_client.py --action delete --pol-no POL2024001
        """,
    )
    parser.add_argument("--action", required=True,
                        choices=["list", "get", "holder", "idcard", "query", "page",
                                 "create", "update", "delete", "login"],
                        help="操作类型")
    parser.add_argument("--pol-no", help="保单号 (get/delete 用)")
    parser.add_argument("--name", help="投保人姓名 (holder 用)")
    parser.add_argument("--id", help="身份证号 (idcard 用)")
    parser.add_argument("--page", type=int, default=1, help="页码 (page 用, 默认1)")
    parser.add_argument("--size", type=int, default=10, help="每页大小 (page 用, 默认10)")
    parser.add_argument("--status", help="保单状态筛选 (query/page 用)")
    parser.add_argument("--insurance-type", help="保险类型筛选 (query/page 用)")
    parser.add_argument("--data", help="JSON 格式请求体 (create/update 用)")
    parser.add_argument("--username", default="admin", help="登录用户名 (login 用)")
    parser.add_argument("--password", default="admin123", help="登录密码 (login 用)")
    parser.add_argument("--mock", action="store_true", default=True, help="使用 Mock 模式 (默认开启)")
    parser.add_argument("--live", dest="mock", action="store_false", help="使用真实 HTTP 请求")

    args = parser.parse_args()

    # 覆盖全局 Mock 开关
    global MOCK_MODE
    MOCK_MODE = args.mock

    client = get_client()

    if args.action == "login":
        result = client.login(args.username, args.password)
    elif args.action == "list":
        result = client.get_all()
    elif args.action == "get":
        if not args.pol_no:
            print("错误: --pol-no 必填", file=sys.stderr)
            sys.exit(1)
        result = client.get_by_pol_no(args.pol_no)
    elif args.action == "holder":
        if not args.name:
            print("错误: --name 必填", file=sys.stderr)
            sys.exit(1)
        result = client.get_by_holder(args.name)
    elif args.action == "idcard":
        if not args.id:
            print("错误: --id 必填", file=sys.stderr)
            sys.exit(1)
        result = client.get_by_idcard(args.id)
    elif args.action == "query":
        conds = {}
        if args.status:
            conds["status"] = args.status
        if args.insurance_type:
            conds["insuranceType"] = args.insurance_type
        result = client.query(**conds)
    elif args.action == "page":
        conds = {}
        if args.status:
            conds["status"] = args.status
        if args.insurance_type:
            conds["insuranceType"] = args.insurance_type
        result = client.page(args.page, args.size, **conds)
    elif args.action == "create":
        if not args.data:
            print("错误: --data 必填 (JSON 格式)", file=sys.stderr)
            sys.exit(1)
        try:
            body = json.loads(args.data)
        except json.JSONDecodeError as e:
            print(f"错误: --data JSON 解析失败: {e}", file=sys.stderr)
            sys.exit(1)
        result = client.create(body)
    elif args.action == "update":
        if not args.data:
            print("错误: --data 必填 (JSON 格式, 含 polNo)", file=sys.stderr)
            sys.exit(1)
        try:
            body = json.loads(args.data)
        except json.JSONDecodeError as e:
            print(f"错误: --data JSON 解析失败: {e}", file=sys.stderr)
            sys.exit(1)
        result = client.update(body)
    elif args.action == "delete":
        if not args.pol_no:
            print("错误: --pol-no 必填", file=sys.stderr)
            sys.exit(1)
        result = client.delete(args.pol_no)
    else:
        parser.print_help()
        sys.exit(1)

    print_json(result)


if __name__ == "__main__":
    main()
