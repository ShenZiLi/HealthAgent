#!/usr/bin/env python3
"""
HealthAgent 保单查询 API 客户端

用于调用 HealthAgent 系统的保单查询 API。

使用方式:
    python policy_api.py --action query --id-card "110101199001011234" --base-url "http://localhost:8080" --token "your_jwt_token"
    python policy_api.py --action get-by-pol-no --pol-no "POL001" --base-url "http://localhost:8080" --token "your_jwt_token"
    python policy_api.py --action get-by-holder --holder-name "张三" --base-url "http://localhost:8080" --token "your_jwt_token"

认证:
    所有接口需要 JWT Token 认证，通过 --token 参数传入。
    Token 通过 HealthAgent 的 /auth/login 接口获取。
"""

import argparse
import json
import sys
import urllib.request
import urllib.error
from datetime import datetime
from typing import Optional, Dict, Any, List


class PolicyAPIClient:
    """HealthAgent 保单 API 客户端"""
    
    def __init__(self, base_url: str, token: str):
        self.base_url = base_url.rstrip('/')
        self.token = token
    
    def _make_request(self, method: str, path: str, data: Optional[Dict] = None) -> Dict[str, Any]:
        """发送 HTTP 请求"""
        url = f"{self.base_url}{path}"
        headers = {
            'Authorization': f'Bearer {self.token}',
            'Content-Type': 'application/json',
            'Accept': 'application/json'
        }
        
        body_data = None
        if data:
            body_data = json.dumps(data).encode('utf-8')
        
        req = urllib.request.Request(url, data=body_data, headers=headers, method=method)
        
        try:
            with urllib.request.urlopen(req, timeout=30) as response:
                response_data = response.read().decode('utf-8')
                return json.loads(response_data)
        except urllib.error.HTTPError as e:
            error_body = e.read().decode('utf-8') if e.fp else ''
            try:
                error_json = json.loads(error_body)
                return {'error': True, 'status': e.code, 'message': error_json.get('message', error_body)}
            except json.JSONDecodeError:
                return {'error': True, 'status': e.code, 'message': error_body}
        except urllib.error.URLError as e:
            return {'error': True, 'message': f'网络错误: {str(e.reason)}'}
        except Exception as e:
            return {'error': True, 'message': f'请求失败: {str(e)}'}
    
    def get_by_pol_no(self, pol_no: str) -> Dict[str, Any]:
        """按保单号查询"""
        return self._make_request('GET', f'/api/policy/{pol_no}')
    
    def get_by_holder_name(self, holder_name: str) -> Dict[str, Any]:
        """按投保人姓名查询"""
        # URL 编码姓名
        encoded_name = urllib.request.quote(holder_name, safe='')
        return self._make_request('GET', f'/api/policy/holder/{encoded_name}')
    
    def get_by_id_card(self, id_card: str) -> Dict[str, Any]:
        """按身份证号查询"""
        return self._make_request('GET', f'/api/policy/idcard/{id_card}')
    
    def query_all(self) -> Dict[str, Any]:
        """查询所有保单"""
        return self._make_request('GET', '/api/policy/all')
    
    def query_by_params(self, params: Dict[str, Any]) -> Dict[str, Any]:
        """条件查询"""
        return self._make_request('POST', '/api/policy/query', params)
    
    def query_with_pagination(self, page: int = 1, size: int = 10, params: Optional[Dict] = None) -> Dict[str, Any]:
        """分页查询"""
        body = {
            'page': page,
            'size': size
        }
        if params:
            body.update(params)
        return self._make_request('POST', '/api/policy/page', body)
    
    def create_policy(self, policy_data: Dict[str, Any]) -> Dict[str, Any]:
        """创建保单"""
        return self._make_request('POST', '/api/policy', policy_data)
    
    def update_policy(self, policy_data: Dict[str, Any]) -> Dict[str, Any]:
        """更新保单"""
        return self._make_request('PUT', '/api/policy', policy_data)
    
    def delete_policy(self, pol_no: str) -> Dict[str, Any]:
        """删除保单"""
        return self._make_request('DELETE', f'/api/policy/{pol_no}')


def format_policy_as_text(policy: Dict[str, Any]) -> str:
    """格式化保单信息为文本"""
    if not policy:
        return "无保单信息"
    
    lines = []
    lines.append(f"保单号: {policy.get('polNo', '未知')}")
    lines.append(f"投保人: {policy.get('policyHolderName', '未知')}")
    lines.append(f"身份证号: {policy.get('idCardNo', '未知')}")
    lines.append(f"保险产品: {policy.get('insuranceProduct', '未知')}")
    
    sum_insured = policy.get('sumInsured')
    if sum_insured:
        lines.append(f"保额: {sum_insured} 元")
    
    premium = policy.get('premium')
    if premium:
        lines.append(f"保费: {premium} 元")
    
    effective_date = policy.get('effectiveDate')
    if effective_date:
        lines.append(f"生效日期: {effective_date}")
    
    expiry_date = policy.get('expiryDate')
    if expiry_date:
        lines.append(f"到期日期: {expiry_date}")
    
    status = policy.get('status', '未知')
    status_text = {'active': '有效', 'expired': '已过期'}.get(status, status)
    lines.append(f"状态: {status_text}")
    
    return '\n'.join(lines)


def format_policies_list(policies: List[Dict[str, Any]]) -> str:
    """格式化保单列表为文本"""
    if not policies:
        return "未查询到保单"
    
    lines = [f"共查询到 {len(policies)} 条保单记录:\n"]
    for i, policy in enumerate(policies, 1):
        lines.append(f"--- 保单 {i} ---")
        lines.append(format_policy_as_text(policy))
        lines.append("")
    
    return '\n'.join(lines)


def login(base_url: str, username: str, password: str) -> Dict[str, Any]:
    """登录获取 Token"""
    url = f"{base_url.rstrip('/')}/auth/login"
    data = json.dumps({'username': username, 'password': password}).encode('utf-8')
    
    req = urllib.request.Request(
        url,
        data=data,
        headers={'Content-Type': 'application/json'},
        method='POST'
    )
    
    try:
        with urllib.request.urlopen(req, timeout=30) as response:
            response_data = response.read().decode('utf-8')
            return json.loads(response_data)
    except urllib.error.HTTPError as e:
        error_body = e.read().decode('utf-8') if e.fp else ''
        return {'error': True, 'status': e.code, 'message': error_body}
    except Exception as e:
        return {'error': True, 'message': str(e)}


def main():
    parser = argparse.ArgumentParser(description='HealthAgent 保单查询 API 客户端')
    parser.add_argument('--base-url', default='http://localhost:8080', help='API 基础 URL')
    parser.add_argument('--token', required=False, help='JWT Token (可通过 --login 获取)')
    
    # 登录选项
    parser.add_argument('--login', action='store_true', help='执行登录获取 Token')
    parser.add_argument('--username', help='登录用户名')
    parser.add_argument('--password', help='登录密码')
    
    # 查询选项
    parser.add_argument('--action', choices=[
        'get-by-pol-no', 'get-by-holder', 'get-by-id-card', 
        'query-all', 'query', 'query-page',
        'create', 'update', 'delete'
    ], required=False, help='执行的操作')
    
    # 查询参数
    parser.add_argument('--pol-no', help='保单号')
    parser.add_argument('--holder-name', help='投保人姓名')
    parser.add_argument('--id-card', help='身份证号')
    parser.add_argument('--page', type=int, default=1, help='分页页码')
    parser.add_argument('--size', type=int, default=10, help='分页大小')
    
    # 保单数据 (用于创建/更新)
    parser.add_argument('--policy-data', help='保单 JSON 数据 (用于创建/更新)')
    
    args = parser.parse_args()
    
    # 登录模式
    if args.login:
        if not args.username or not args.password:
            print('错误: 登录需要 --username 和 --password')
            sys.exit(1)
        result = login(args.base_url, args.username, args.password)
        if 'error' in result:
            print(f'登录失败: {result.get("message", "未知错误")}')
            sys.exit(1)
        print(json.dumps(result, ensure_ascii=False, indent=2))
        sys.exit(0)
    
    # API 操作模式
    if not args.action:
        parser.print_help()
        sys.exit(1)
    
    if not args.token:
        print('错误: 需要 --token 参数 (可通过 --login 获取)')
        sys.exit(1)
    
    client = PolicyAPIClient(args.base_url, args.token)
    
    result = None
    
    if args.action == 'get-by-pol-no':
        if not args.pol_no:
            print('错误: 需要 --pol-no 参数')
            sys.exit(1)
        result = client.get_by_pol_no(args.pol_no)
        if 'error' not in result:
            print(format_policy_as_text(result.get('data', result)))
    
    elif args.action == 'get-by-holder':
        if not args.holder_name:
            print('错误: 需要 --holder-name 参数')
            sys.exit(1)
        result = client.get_by_holder_name(args.holder_name)
        if 'error' not in result:
            data = result.get('data', result)
            if isinstance(data, list):
                print(format_policies_list(data))
            else:
                print(format_policy_as_text(data))
    
    elif args.action == 'get-by-id-card':
        if not args.id_card:
            print('错误: 需要 --id-card 参数')
            sys.exit(1)
        result = client.get_by_id_card(args.id_card)
        if 'error' not in result:
            data = result.get('data', result)
            if isinstance(data, list):
                print(format_policies_list(data))
            else:
                print(format_policy_as_text(data))
    
    elif args.action == 'query-all':
        result = client.query_all()
        if 'error' not in result:
            data = result.get('data', result)
            print(format_policies_list(data if isinstance(data, list) else [data]))
    
    elif args.action == 'query':
        params = {}
        if args.pol_no:
            params['polNo'] = args.pol_no
        if args.holder_name:
            params['policyHolderName'] = args.holder_name
        if args.id_card:
            params['idCardNo'] = args.id_card
        result = client.query_by_params(params)
        if 'error' not in result:
            data = result.get('data', result)
            print(format_policies_list(data if isinstance(data, list) else [data]))
    
    elif args.action == 'query-page':
        params = {}
        if args.pol_no:
            params['polNo'] = args.pol_no
        if args.holder_name:
            params['policyHolderName'] = args.holder_name
        if args.id_card:
            params['idCardNo'] = args.id_card
        result = client.query_with_pagination(args.page, args.size, params)
        if 'error' not in result:
            data = result.get('data', {})
            items = data.get('content', data.get('items', []))
            total = data.get('totalElements', data.get('total', len(items)))
            print(f"分页查询结果 (第 {args.page} 页, 每页 {args.size} 条):\n")
            print(format_policies_list(items))
            print(f"总计: {total} 条记录")
    
    elif args.action == 'create':
        if not args.policy_data:
            print('错误: 需要 --policy-data 参数 (JSON 格式保单数据)')
            sys.exit(1)
        try:
            policy_data = json.loads(args.policy_data)
        except json.JSONDecodeError:
            print('错误: --policy-data 必须是有效的 JSON')
            sys.exit(1)
        result = client.create_policy(policy_data)
        if 'error' not in result:
            print('保单创建成功')
            print(json.dumps(result.get('data', result), ensure_ascii=False, indent=2))
    
    elif args.action == 'update':
        if not args.policy_data:
            print('错误: 需要 --policy-data 参数 (JSON 格式保单数据)')
            sys.exit(1)
        try:
            policy_data = json.loads(args.policy_data)
        except json.JSONDecodeError:
            print('错误: --policy-data 必须是有效的 JSON')
            sys.exit(1)
        result = client.update_policy(policy_data)
        if 'error' not in result:
            print('保单更新成功')
            print(json.dumps(result.get('data', result), ensure_ascii=False, indent=2))
    
    elif args.action == 'delete':
        if not args.pol_no:
            print('错误: 需要 --pol-no 参数')
            sys.exit(1)
        result = client.delete_policy(args.pol_no)
        if 'error' not in result:
            print(f'保单 {args.pol_no} 删除成功')
    
    # 输出错误信息
    if result and 'error' in result:
        print(f'请求失败: {result.get("message", "未知错误")}')
        if result.get('status'):
            print(f'HTTP 状态码: {result["status"]}')
        sys.exit(1)


if __name__ == '__main__':
    main()