#!/usr/bin/env python3
"""
HealthAgent 体检预约 API 客户端

用于调用 HealthAgent 系统的体检预约相关 API。

使用方式:
    python exam_api.py --action hospitals --base-url "http://localhost:8080" --token "your_jwt_token"
    python exam_api.py --action packages --base-url "http://localhost:8080" --token "your_jwt_token"
    python exam_api.py --action plans --base-url "http://localhost:8080" --token "your_jwt_token"
    python exam_api.py --action bookings --user-id "user001" --base-url "http://localhost:8080" --token "your_jwt_token"
    python exam_api.py --action book --booking-data '{"userId":"user001","userName":"张三","hospitalId":1,"packageId":1,"examinationDate":"2024-06-01"}' --base-url "http://localhost:8080" --token "your_jwt_token"

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


class ExamAPIClient:
    """HealthAgent 体检预约 API 客户端"""
    
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
    
    def get_hospitals(self) -> Dict[str, Any]:
        """获取医院列表"""
        return self._make_request('GET', '/api/exam/hospitals')
    
    def get_packages(self) -> Dict[str, Any]:
        """获取体检套餐列表"""
        return self._make_request('GET', '/api/exam/packages')
    
    def get_plans(self, hospital_id: Optional[int] = None, package_id: Optional[int] = None) -> Dict[str, Any]:
        """获取体检计划列表"""
        params = []
        if hospital_id:
            params.append(f'hospitalId={hospital_id}')
        if package_id:
            params.append(f'packageId={package_id}')
        
        path = '/api/exam/plans'
        if params:
            path += '?' + '&'.join(params)
        
        return self._make_request('GET', path)
    
    def get_bookings(self, user_id: str) -> Dict[str, Any]:
        """获取用户的预约记录"""
        return self._make_request('GET', f'/api/exam/bookings/{user_id}')
    
    def create_booking(self, booking_data: Dict[str, Any]) -> Dict[str, Any]:
        """创建体检预约"""
        return self._make_request('POST', '/api/exam/bookings', booking_data)
    
    def update_booking(self, booking_id: int, booking_data: Dict[str, Any]) -> Dict[str, Any]:
        """更新体检预约"""
        return self._make_request('PUT', f'/api/exam/bookings/{booking_id}', booking_data)
    
    def cancel_booking(self, booking_id: int) -> Dict[str, Any]:
        """取消体检预约"""
        return self._make_request('DELETE', f'/api/exam/bookings/{booking_id}')


def format_hospital_as_text(hospital: Dict[str, Any]) -> str:
    """格式化医院信息为文本"""
    if not hospital:
        return "无医院信息"
    
    lines = []
    lines.append(f"医院编码: {hospital.get('hospitalCode', '未知')}")
    lines.append(f"医院名称: {hospital.get('hospitalName', '未知')}")
    lines.append(f"地址: {hospital.get('address', '未知')}")
    lines.append(f"联系电话: {hospital.get('contactPhone', '未知')}")
    
    status = hospital.get('status', '未知')
    status_text = {'active': '可用', 'inactive': '不可用'}.get(status, status)
    lines.append(f"状态: {status_text}")
    
    return '\n'.join(lines)


def format_hospitals_list(hospitals: List[Dict[str, Any]]) -> str:
    """格式化医院列表为文本"""
    if not hospitals:
        return "未查询到医院信息"
    
    lines = [f"共查询到 {len(hospitals)} 家医院:\n"]
    for i, hospital in enumerate(hospitals, 1):
        lines.append(f"--- 医院 {i} ---")
        lines.append(format_hospital_as_text(hospital))
        lines.append("")
    
    return '\n'.join(lines)


def format_package_as_text(package: Dict[str, Any]) -> str:
    """格式化体检套餐信息为文本"""
    if not package:
        return "无套餐信息"
    
    lines = []
    lines.append(f"套餐编码: {package.get('packageCode', '未知')}")
    lines.append(f"套餐名称: {package.get('packageName', '未知')}")
    lines.append(f"描述: {package.get('description', '无')}")
    
    price = package.get('price')
    if price:
        lines.append(f"价格: {price} 元")
    
    items = package.get('items')
    if items:
        if isinstance(items, list):
            lines.append(f"检查项目: {', '.join(items)}")
        else:
            lines.append(f"检查项目: {items}")
    
    status = package.get('status', '未知')
    status_text = {'active': '可用', 'inactive': '不可用'}.get(status, status)
    lines.append(f"状态: {status_text}")
    
    return '\n'.join(lines)


def format_packages_list(packages: List[Dict[str, Any]]) -> str:
    """格式化体检套餐列表为文本"""
    if not packages:
        return "未查询到体检套餐"
    
    lines = [f"共查询到 {len(packages)} 个体检套餐:\n"]
    for i, package in enumerate(packages, 1):
        lines.append(f"--- 套餐 {i} ---")
        lines.append(format_package_as_text(package))
        lines.append("")
    
    return '\n'.join(lines)


def format_plan_as_text(plan: Dict[str, Any]) -> str:
    """格式化体检计划信息为文本"""
    if not plan:
        return "无计划信息"
    
    lines = []
    lines.append(f"计划名称: {plan.get('planName', '未知')}")
    lines.append(f"医院: {plan.get('hospitalName', plan.get('hospitalId', '未知'))}")
    lines.append(f"套餐: {plan.get('packageName', plan.get('packageId', '未知'))}")
    
    available_dates = plan.get('availableDates')
    if available_dates:
        if isinstance(available_dates, list):
            lines.append(f"可预约日期: {', '.join(available_dates)}")
        else:
            lines.append(f"可预约日期: {available_dates}")
    
    status = plan.get('status', '未知')
    status_text = {'active': '可用', 'inactive': '不可用'}.get(status, status)
    lines.append(f"状态: {status_text}")
    
    return '\n'.join(lines)


def format_plans_list(plans: List[Dict[str, Any]]) -> str:
    """格式化体检计划列表为文本"""
    if not plans:
        return "未查询到体检计划"
    
    lines = [f"共查询到 {len(plans)} 个体检计划:\n"]
    for i, plan in enumerate(plans, 1):
        lines.append(f"--- 计划 {i} ---")
        lines.append(format_plan_as_text(plan))
        lines.append("")
    
    return '\n'.join(lines)


def format_booking_as_text(booking: Dict[str, Any]) -> str:
    """格式化预约信息为文本"""
    if not booking:
        return "无预约信息"
    
    lines = []
    lines.append(f"预约号: {booking.get('bookingNo', '未知')}")
    lines.append(f"用户: {booking.get('userName', '未知')}")
    lines.append(f"联系电话: {booking.get('phone', '未知')}")
    lines.append(f"医院: {booking.get('hospitalName', '未知')}")
    lines.append(f"套餐: {booking.get('packageName', '未知')}")
    lines.append(f"体检日期: {booking.get('examinationDate', '未知')}")
    
    exam_time = booking.get('examinationTime')
    if exam_time:
        lines.append(f"体检时段: {exam_time}")
    
    status = booking.get('status', '未知')
    status_text = {
        'pending': '待确认',
        'confirmed': '已确认',
        'completed': '已完成',
        'cancelled': '已取消'
    }.get(status, status)
    lines.append(f"状态: {status_text}")
    
    notes = booking.get('notes')
    if notes:
        lines.append(f"备注: {notes}")
    
    return '\n'.join(lines)


def format_bookings_list(bookings: List[Dict[str, Any]]) -> str:
    """格式化预约列表为文本"""
    if not bookings:
        return "未查询到预约记录"
    
    lines = [f"共查询到 {len(bookings)} 条预约记录:\n"]
    for i, booking in enumerate(bookings, 1):
        lines.append(f"--- 预约 {i} ---")
        lines.append(format_booking_as_text(booking))
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
    parser = argparse.ArgumentParser(description='HealthAgent 体检预约 API 客户端')
    parser.add_argument('--base-url', default='http://localhost:8080', help='API 基础 URL')
    parser.add_argument('--token', required=False, help='JWT Token (可通过 --login 获取)')
    
    # 登录选项
    parser.add_argument('--login', action='store_true', help='执行登录获取 Token')
    parser.add_argument('--username', help='登录用户名')
    parser.add_argument('--password', help='登录密码')
    
    # 查询选项
    parser.add_argument('--action', choices=[
        'hospitals', 'packages', 'plans', 'bookings',
        'book', 'update', 'cancel'
    ], required=False, help='执行的操作')
    
    # 查询参数
    parser.add_argument('--user-id', help='用户 ID (查询预约记录)')
    parser.add_argument('--hospital-id', type=int, help='医院 ID (筛选计划)')
    parser.add_argument('--package-id', type=int, help='套餐 ID (筛选计划)')
    
    # 预约数据
    parser.add_argument('--booking-data', help='预约 JSON 数据 (用于创建/更新)')
    parser.add_argument('--booking-id', type=int, help='预约 ID (用于更新/取消)')
    
    # 快捷预约参数
    parser.add_argument('--user-name', help='用户姓名')
    parser.add_argument('--id-card', help='身份证号')
    parser.add_argument('--phone', help='联系电话')
    parser.add_argument('--exam-date', help='体检日期 (YYYY-MM-DD)')
    parser.add_argument('--exam-time', help='体检时段')
    parser.add_argument('--notes', help='备注')
    
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
    
    client = ExamAPIClient(args.base_url, args.token)
    
    result = None
    
    if args.action == 'hospitals':
        result = client.get_hospitals()
        if 'error' not in result:
            data = result.get('data', result)
            print(format_hospitals_list(data if isinstance(data, list) else [data]))
    
    elif args.action == 'packages':
        result = client.get_packages()
        if 'error' not in result:
            data = result.get('data', result)
            print(format_packages_list(data if isinstance(data, list) else [data]))
    
    elif args.action == 'plans':
        result = client.get_plans(args.hospital_id, args.package_id)
        if 'error' not in result:
            data = result.get('data', result)
            print(format_plans_list(data if isinstance(data, list) else [data]))
    
    elif args.action == 'bookings':
        if not args.user_id:
            print('错误: 需要 --user-id 参数')
            sys.exit(1)
        result = client.get_bookings(args.user_id)
        if 'error' not in result:
            data = result.get('data', result)
            print(format_bookings_list(data if isinstance(data, list) else [data]))
    
    elif args.action == 'book':
        # 支持快捷参数或完整 JSON
        if args.booking_data:
            try:
                booking_data = json.loads(args.booking_data)
            except json.JSONDecodeError:
                print('错误: --booking-data 必须是有效的 JSON')
                sys.exit(1)
        else:
            # 使用快捷参数构建
            if not args.user_id or not args.user_name:
                print('错误: 创建预约需要 --user-id 和 --user-name，或使用 --booking-data')
                sys.exit(1)
            if not args.hospital_id or not args.package_id:
                print('错误: 创建预约需要 --hospital-id 和 --package-id')
                sys.exit(1)
            if not args.exam_date:
                print('错误: 创建预约需要 --exam-date')
                sys.exit(1)
            
            booking_data = {
                'userId': args.user_id,
                'userName': args.user_name,
                'hospitalId': args.hospital_id,
                'packageId': args.package_id,
                'examinationDate': args.exam_date
            }
            if args.id_card:
                booking_data['idCardNo'] = args.id_card
            if args.phone:
                booking_data['phone'] = args.phone
            if args.exam_time:
                booking_data['examinationTime'] = args.exam_time
            if args.notes:
                booking_data['notes'] = args.notes
        
        result = client.create_booking(booking_data)
        if 'error' not in result:
            print('预约创建成功')
            print(format_booking_as_text(result.get('data', result)))
    
    elif args.action == 'update':
        if not args.booking_id:
            print('错误: 需要 --booking-id 参数')
            sys.exit(1)
        
        if args.booking_data:
            try:
                booking_data = json.loads(args.booking_data)
            except json.JSONDecodeError:
                print('错误: --booking-data 必须是有效的 JSON')
                sys.exit(1)
        else:
            booking_data = {}
            if args.exam_date:
                booking_data['examinationDate'] = args.exam_date
            if args.exam_time:
                booking_data['examinationTime'] = args.exam_time
            if args.notes:
                booking_data['notes'] = args.notes
            if args.phone:
                booking_data['phone'] = args.phone
        
        result = client.update_booking(args.booking_id, booking_data)
        if 'error' not in result:
            print('预约更新成功')
            print(format_booking_as_text(result.get('data', result)))
    
    elif args.action == 'cancel':
        if not args.booking_id:
            print('错误: 需要 --booking-id 参数')
            sys.exit(1)
        result = client.cancel_booking(args.booking_id)
        if 'error' not in result:
            print(f'预约 {args.booking_id} 已取消')
    
    # 输出错误信息
    if result and 'error' in result:
        print(f'请求失败: {result.get("message", "未知错误")}')
        if result.get('status'):
            print(f'HTTP 状态码: {result["status"]}')
        sys.exit(1)


if __name__ == '__main__':
    main()