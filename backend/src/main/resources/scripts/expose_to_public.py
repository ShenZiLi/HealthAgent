#!/usr/bin/env python3
"""
HealthAgent 一键内网穿透启动脚本

将本地 Spring Boot + Vue 项目通过内网穿透暴露到公网访问。
支持三种穿透方案：Cloudflare Tunnel / cpolar / ngrok

用法:
    python expose_to_public.py --tool cloudflared    # Cloudflare Tunnel（推荐）
    python expose_to_public.py --tool cpolar          # cpolar
    python expose_to_public.py --tool ngrok           # ngrok

前置条件:
    1. Spring Boot 后端已启动（端口 8084）
    2. Vue 前端已启动（端口 5173）
    3. 已安装对应的穿透工具

环境变量:
    BACKEND_PORT  - 后端端口 (默认 8084)
    FRONTEND_PORT - 前端端口 (默认 5173)
"""

import argparse
import json
import os
import platform
import shutil
import subprocess
import sys
import time
from pathlib import Path

# ============================================================
# 配置
# ============================================================

BACKEND_PORT = os.environ.get("BACKEND_PORT", "8084")
FRONTEND_PORT = os.environ.get("FRONTEND_PORT", "5173")

# 颜色输出
class Color:
    GREEN = "\033[92m"
    YELLOW = "\033[93m"
    RED = "\033[91m"
    CYAN = "\033[96m"
    BOLD = "\033[1m"
    END = "\033[0m"

def print_info(msg): print(f"{Color.CYAN}[INFO]{Color.END} {msg}")
def print_ok(msg):   print(f"{Color.GREEN}[OK]{Color.END} {msg}")
def print_warn(msg): print(f"{Color.YELLOW}[WARN]{Color.END} {msg}")
def print_err(msg):  print(f"{Color.RED}[ERROR]{Color.END} {msg}")
def print_header(msg): print(f"\n{Color.BOLD}{'='*60}\n  {msg}\n{'='*60}{Color.END}")


# ============================================================
# 工具检测
# ============================================================

def check_tool(name: str) -> str | None:
    """检测工具是否已安装，返回路径或 None"""
    return shutil.which(name)


def check_port(port: str) -> bool:
    """检测端口是否有服务在监听"""
    system = platform.system()
    try:
        if system == "Windows":
            result = subprocess.run(
                ["netstat", "-ano"],
                capture_output=True, text=True, timeout=5
            )
            return f":{port}" in result.stdout and "LISTENING" in result.stdout
        else:
            result = subprocess.run(
                ["lsof", "-i", f":{port}"],
                capture_output=True, text=True, timeout=5
            )
            return bool(result.stdout.strip())
    except Exception:
        return False


# ============================================================
# Cloudflare Tunnel (cloudflared)
# ============================================================

CLOUDFLARED_CONFIG = f"""# HealthAgent Cloudflare Tunnel 配置
# 文件位置: ~/.cloudflared/config.yml

tunnel: healthagent-tunnel
credentials-file: ~/.cloudflared/healthagent-tunnel.json

ingress:
  # 前端 - Vue 开发服务器
  - hostname: healthagent-frontend.loca.lt
    service: http://localhost:{FRONTEND_PORT}

  # 后端 - Spring Boot API
  - hostname: healthagent-api.loca.lt
    service: http://localhost:{BACKEND_PORT}

  # 兜底规则（必须）
  - service: http_status:404
"""


def setup_cloudflared():
    """Cloudflare Tunnel 方案"""
    print_header("方案一：Cloudflare Tunnel (cloudflared)")

    tool = check_tool("cloudflared")
    if not tool:
        print_err("cloudflared 未安装")
        print_info("安装方式：")
        print("  Windows:  winget install Cloudflare.cloudflared")
        print("  或下载:   https://github.com/cloudflare/cloudflared/releases")
        print("  Mac:      brew install cloudflared")
        print("  Linux:    curl -L https://github.com/cloudflare/cloudflared/releases/latest/download/cloudflared-linux-amd64 -o /usr/local/bin/cloudflared && chmod +x /usr/local/bin/cloudflared")
        return False

    print_ok(f"cloudflared 已安装: {tool}")

    # 快速隧道模式（无需登录，临时域名）
    print_info("启动快速隧道（临时域名，无需登录）...")
    print_info(f"  前端 → localhost:{FRONTEND_PORT}")
    print_info(f"  后端 → localhost:{BACKEND_PORT}")
    print()
    print_warn("将打开两个终端窗口分别穿透前端和后端")
    print_warn("关闭此脚本或按 Ctrl+C 停止所有隧道")
    print()

    system = platform.system()

    try:
        # 前端隧道
        print_info(f"正在为前端 (:{FRONTEND_PORT}) 创建隧道...")
        if system == "Windows":
            fe_proc = subprocess.Popen(
                ["cloudflared", "tunnel", "--url", f"http://localhost:{FRONTEND_PORT}"],
                creationflags=subprocess.CREATE_NEW_CONSOLE,
                stdout=subprocess.PIPE, stderr=subprocess.STDOUT
            )
        else:
            fe_proc = subprocess.Popen(
                ["cloudflared", "tunnel", "--url", f"http://localhost:{FRONTEND_PORT}"],
                stdout=subprocess.PIPE, stderr=subprocess.STDOUT
            )

        time.sleep(3)

        # 后端隧道
        print_info(f"正在为后端 (:{BACKEND_PORT}) 创建隧道...")
        if system == "Windows":
            be_proc = subprocess.Popen(
                ["cloudflared", "tunnel", "--url", f"http://localhost:{BACKEND_PORT}"],
                creationflags=subprocess.CREATE_NEW_CONSOLE,
                stdout=subprocess.PIPE, stderr=subprocess.STDOUT
            )
        else:
            be_proc = subprocess.Popen(
                ["cloudflared", "tunnel", "--url", f"http://localhost:{BACKEND_PORT}"],
                stdout=subprocess.PIPE, stderr=subprocess.STDOUT
            )

        print_ok("隧道已启动！")
        print()
        print_info("请在新打开的终端窗口中查看公网地址")
        print_info("地址格式: https://xxx-xxx-xxx.trycloudflare.com")
        print()
        print_warn("注意：需要修改前端 Vite 代理配置，将 API 请求直接发到后端公网地址")
        print()

        input("按 Enter 键停止隧道...")
        fe_proc.terminate()
        be_proc.terminate()
        print_ok("隧道已停止")

    except KeyboardInterrupt:
        print_ok("隧道已停止")
    except Exception as e:
        print_err(f"启动失败: {e}")
        return False

    return True


# ============================================================
# cpolar
# ============================================================

def setup_cpolar():
    """cpolar 方案"""
    print_header("方案二：cpolar")

    tool = check_tool("cpolar")
    if not tool:
        print_err("cpolar 未安装")
        print_info("安装方式：")
        print("  Windows:  https://www.cpolar.com/ 下载安装包")
        print("  Linux:    curl -L https://www.cpolar.com/static/downloads/install-release-cpolar.sh | sudo bash")
        print()
        print_info("注册账号: https://dashboard.cpolar.com/signup")
        print_warn("需要先认证: cpolar authtoken <你的token>")
        return False

    print_ok(f"cpolar 已安装: {tool}")

    print_info("启动 cpolar 隧道...")
    print_info(f"  前端 → localhost:{FRONTEND_PORT}")
    print_info(f"  后端 → localhost:{BACKEND_PORT}")
    print()
    print_info("cpolar 免费版：4条隧道、1M带宽、地址24h变化")
    print_info("管理后台: http://localhost:9200")
    print()

    system = platform.system()

    try:
        # 后端隧道
        print_info(f"正在为后端 (:{BACKEND_PORT}) 创建隧道...")
        if system == "Windows":
            be_proc = subprocess.Popen(
                ["cpolar", "http", BACKEND_PORT, "-log=stdout"],
                creationflags=subprocess.CREATE_NEW_CONSOLE,
                stdout=subprocess.PIPE, stderr=subprocess.STDOUT
            )
        else:
            be_proc = subprocess.Popen(
                ["cpolar", "http", BACKEND_PORT],
                stdout=subprocess.PIPE, stderr=subprocess.STDOUT
            )

        time.sleep(2)

        # 前端隧道
        print_info(f"正在为前端 (:{FRONTEND_PORT}) 创建隧道...")
        if system == "Windows":
            fe_proc = subprocess.Popen(
                ["cpolar", "http", FRONTEND_PORT, "-log=stdout"],
                creationflags=subprocess.CREATE_NEW_CONSOLE,
                stdout=subprocess.PIPE, stderr=subprocess.STDOUT
            )
        else:
            fe_proc = subprocess.Popen(
                ["cpolar", "http", FRONTEND_PORT],
                stdout=subprocess.PIPE, stderr=subprocess.STDOUT
            )

        print_ok("隧道已启动！")
        print_info("管理后台: http://localhost:9200  查看公网地址")
        print_info("免费版地址格式: https://xxx.r1.cpolar.top")
        print()

        input("按 Enter 键停止隧道...")
        be_proc.terminate()
        fe_proc.terminate()
        print_ok("隧道已停止")

    except KeyboardInterrupt:
        print_ok("隧道已停止")
    except Exception as e:
        print_err(f"启动失败: {e}")
        return False

    return True


# ============================================================
# ngrok
# ============================================================

NGROK_CONFIG = """version: "2"
authtoken: <替换为你的authtoken>

tunnels:
  backend:
    proto: http
    addr: BACKEND_PORT
  frontend:
    proto: http
    addr: FRONTEND_PORT
""".replace("BACKEND_PORT", BACKEND_PORT).replace("FRONTEND_PORT", FRONTEND_PORT)


def setup_ngrok():
    """ngrok 方案"""
    print_header("方案三：ngrok")

    tool = check_tool("ngrok")
    if not tool:
        print_err("ngrok 未安装")
        print_info("安装方式：")
        print("  Windows:  choco install ngrok  或  winget install Ngrok.Ngrok")
        print("  或下载:   https://ngrok.com/download")
        print("  Mac:      brew install ngrok")
        print("  Linux:    snap install ngrok")
        print()
        print_info("注册账号: https://dashboard.ngrok.com/signup")
        print_warn("需要先认证: ngrok config add-authtoken <你的token>")
        return False

    print_ok(f"ngrok 已安装: {tool}")

    print_info("启动 ngrok 隧道...")
    print_info(f"  前端 → localhost:{FRONTEND_PORT}")
    print_info(f"  后端 → localhost:{BACKEND_PORT}")
    print()
    print_info("ngrok 免费版：1个在线进程、1域名/进程、40连接/分钟")
    print_info("管理后台: http://localhost:4040")
    print()

    system = platform.system()

    try:
        # 后端隧道
        print_info(f"正在为后端 (:{BACKEND_PORT}) 创建隧道...")
        if system == "Windows":
            be_proc = subprocess.Popen(
                ["ngrok", "http", BACKEND_PORT],
                creationflags=subprocess.CREATE_NEW_CONSOLE,
                stdout=subprocess.PIPE, stderr=subprocess.STDOUT
            )
        else:
            be_proc = subprocess.Popen(
                ["ngrok", "http", BACKEND_PORT],
                stdout=subprocess.PIPE, stderr=subprocess.STDOUT
            )

        time.sleep(2)

        # 前端隧道
        print_info(f"正在为前端 (:{FRONTEND_PORT}) 创建隧道...")
        if system == "Windows":
            fe_proc = subprocess.Popen(
                ["ngrok", "http", FRONTEND_PORT],
                creationflags=subprocess.CREATE_NEW_CONSOLE,
                stdout=subprocess.PIPE, stderr=subprocess.STDOUT
            )
        else:
            fe_proc = subprocess.Popen(
                ["ngrok", "http", FRONTEND_PORT],
                stdout=subprocess.PIPE, stderr=subprocess.STDOUT
            )

        print_ok("隧道已启动！")
        print_info("管理后台: http://localhost:4040  查看公网地址")
        print_info("免费版地址格式: https://xxxx-xx-xx.ngrok-free.app")
        print()

        input("按 Enter 键停止隧道...")
        be_proc.terminate()
        fe_proc.terminate()
        print_ok("隧道已停止")

    except KeyboardInterrupt:
        print_ok("隧道已停止")
    except Exception as e:
        print_err(f"启动失败: {e}")
        return False

    return True


# ============================================================
# 一键启动脚本生成
# ============================================================

def generate_bat_scripts():
    """生成 Windows 一键启动 .bat 脚本"""
    output_dir = Path(__file__).parent

    # cloudflared 一键脚本
    bat_cloudflared = f"""@echo off
chcp 65001 >nul
title HealthAgent - Cloudflare Tunnel
echo ========================================
echo   HealthAgent 内网穿透 - Cloudflare Tunnel
echo ========================================
echo.

echo [1/2] 启动后端隧道 (:{BACKEND_PORT})...
start "HealthAgent-Backend-Tunnel" cloudflared tunnel --url http://localhost:{BACKEND_PORT}

timeout /t 3 /nobreak >nul

echo [2/2] 启动前端隧道 (:{FRONTEND_PORT})...
start "HealthAgent-Frontend-Tunnel" cloudflared tunnel --url http://localhost:{FRONTEND_PORT}

echo.
echo ✅ 隧道已启动！请在新窗口中查看公网地址
echo    地址格式: https://xxx-xxx-xxx.trycloudflare.com
echo.
echo 关闭此窗口或新窗口即可停止隧道
pause
"""
    (output_dir / "start-cloudflared.bat").write_text(bat_cloudflared, encoding="utf-8")

    # cpolar 一键脚本
    bat_cpolar = f"""@echo off
chcp 65001 >nul
title HealthAgent - cpolar Tunnel
echo ========================================
echo   HealthAgent 内网穿透 - cpolar
echo ========================================
echo.

echo [1/2] 启动后端隧道 (:{BACKEND_PORT})...
start "HealthAgent-Backend-cpolar" cpolar http {BACKEND_PORT}

timeout /t 2 /nobreak >nul

echo [2/2] 启动前端隧道 (:{FRONTEND_PORT})...
start "HealthAgent-Frontend-cpolar" cpolar http {FRONTEND_PORT}

echo.
echo ✅ 隧道已启动！
echo    管理后台: http://localhost:9200
echo    地址格式: https://xxx.r1.cpolar.top
echo.
echo 关闭此窗口或新窗口即可停止隧道
pause
"""
    (output_dir / "start-cpolar.bat").write_text(bat_cpolar, encoding="utf-8")

    # ngrok 一键脚本
    bat_ngrok = f"""@echo off
chcp 65001 >nul
title HealthAgent - ngrok Tunnel
echo ========================================
echo   HealthAgent 内网穿透 - ngrok
echo ========================================
echo.

echo [1/2] 启动后端隧道 (:{BACKEND_PORT})...
start "HealthAgent-Backend-ngrok" ngrok http {BACKEND_PORT}

timeout /t 2 /nobreak >nul

echo [2/2] 启动前端隧道 (:{FRONTEND_PORT})...
start "HealthAgent-Frontend-ngrok" ngrok http {FRONTEND_PORT}

echo.
echo ✅ 隧道已启动！
echo    管理后台: http://localhost:4040
echo    地址格式: https://xxxx.ngrok-free.app
echo.
echo 关闭此窗口或新窗口即可停止隧道
pause
"""
    (output_dir / "start-ngrok.bat").write_text(bat_ngrok, encoding="utf-8")

    print_ok(f"已生成一键启动脚本到: {output_dir}")
    print_info("  - start-cloudflared.bat")
    print_info("  - start-cpolar.bat")
    print_info("  - start-ngrok.bat")


# ============================================================
# 前端配置检查与建议
# ============================================================

def print_frontend_config_guide():
    """打印前端配置修改指南"""
    print_header("前端配置修改指南（重要！）")

    print("""当前 vite.config.ts 的代理配置仅在本地开发时生效，
暴露到公网后，前端通过公网域名访问，/api 请求需要调整：

方案 A：前端直连后端公网地址（最简单）

  修改 vite.config.ts，添加环境变量判断：

    server: {
      proxy: {
        '/api': {
          target: process.env.VITE_API_URL || 'http://localhost:8084',
          changeOrigin: true,
        },
      },
    },

  然后启动前端时设置环境变量：
    VITE_API_URL=https://xxx.trycloudflare.com npm run dev

方案 B：后端反向代理前端（推荐）

  Spring Boot 配置静态资源代理，只暴露一个端口：

  @Configuration
  public class FrontendProxyConfig implements WebMvcConfigurer {
      @Override
      public void addResourceHandlers(ResourceHandlerRegistry registry) {
          // 生产模式：从 classpath:/static 加载前端构建产物
          registry.addResourceHandler("/**")
                  .addResourceLocations("classpath:/static/");
      }
  }

  这样只需穿透一个端口（8084），前端打包后放入
  backend/src/main/resources/static/ 即可。

方案 C：cloudflared 单隧道 + 路由规则

  使用 cloudflared config.yml 配置 ingress 规则：
  - /api/* → http://localhost:8084
  - /*     → http://localhost:5173
""")


def print_cors_guide():
    """打印 CORS 配置指南"""
    print_header("CORS 跨域配置检查")

    print("""HealthAgent 后端 WebConfig 已配置 CORS：

  allowedOrigins:
    - http://localhost:5173
    - http://localhost:5174

暴露到公网后需要添加前端公网域名：

  修改 WebConfig.java，将前端公网地址加入 allowedOrigins：

  @Value("${healthagent.cors.allowed-origins:http://localhost:5173,http://localhost:5174}")
  private String[] allowedOrigins;

  或在 application.yml 中配置：

  healthagent:
    cors:
      allowed-origins: http://localhost:5173,http://localhost:5174,https://your-frontend-domain.trycloudflare.com

建议：使用环境变量注入公网域名，不改源码：

  HEALTHAGENT_CORS_ORIGINS=https://xxx.trycloudflare.com java -jar app.jar
""")


# ============================================================
# 主入口
# ============================================================

def main():
    parser = argparse.ArgumentParser(
        description="HealthAgent 内网穿透一键启动",
        formatter_class=argparse.RawDescriptionHelpFormatter,
        epilog="""
示例:
  python expose_to_public.py --tool cloudflared
  python expose_to_public.py --tool cpolar
  python expose_to_public.py --tool ngrok
  python expose_to_public.py --generate-scripts     # 生成 .bat 一键脚本
  python expose_to_public.py --guide                 # 仅显示配置指南
        """,
    )
    parser.add_argument("--tool", choices=["cloudflared", "cpolar", "ngrok"], help="选择穿透工具")
    parser.add_argument("--generate-scripts", action="store_true", help="生成一键启动 .bat 脚本")
    parser.add_argument("--guide", action="store_true", help="仅显示配置修改指南")
    args = parser.parse_args()

    # 仅显示指南
    if args.guide:
        print_frontend_config_guide()
        print_cors_guide()
        return

    # 生成脚本
    if args.generate_scripts:
        generate_bat_scripts()
        return

    # 启动穿透
    if not args.tool:
        parser.print_help()
        print()
        print_warn("请使用 --tool 指定穿透工具")
        return

    # 检查本地服务是否已启动
    print_info(f"检查本地服务...")
    backend_ok = check_port(BACKEND_PORT)
    frontend_ok = check_port(FRONTEND_PORT)

    if backend_ok:
        print_ok(f"后端服务运行中 (:{BACKEND_PORT})")
    else:
        print_warn(f"后端服务未检测到 (:{BACKEND_PORT})，请确认是否已启动")

    if frontend_ok:
        print_ok(f"前端服务运行中 (:{FRONTEND_PORT})")
    else:
        print_warn(f"前端服务未检测到 (:{FRONTEND_PORT})，请确认是否已启动")

    print()

    # 启动对应工具
    if args.tool == "cloudflared":
        setup_cloudflared()
    elif args.tool == "cpolar":
        setup_cpolar()
    elif args.tool == "ngrok":
        setup_ngrok()


if __name__ == "__main__":
    main()
