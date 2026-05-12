@echo off
chcp 65001 >nul
title HealthAgent - cpolar 内网穿透
echo ========================================
echo   HealthAgent 内网穿透 (国内方案)
echo   工具: cpolar 3.3.12
echo   后端: localhost:8084
echo   前端: localhost:5173
echo ========================================
echo.

set CPOLAR=C:\Tools\cpolar\cpolar.exe

REM 检查 cpolar 是否已安装
if not exist "%CPOLAR%" (
    echo [错误] cpolar 未安装！
    echo 请先运行安装: 访问 https://www.cpolar.com/download 下载
    pause
    exit /b 1
)

REM 检查是否已认证 authtoken
echo [1/3] 检查 authtoken...
"%CPOLAR%" authtoken >nul 2>&1
if errorlevel 1 (
    echo [!] 未认证 authtoken，请先注册并认证:
    echo.
    echo   1. 打开 https://dashboard.cpolar.com/signup 注册账号
    echo   2. 登录后点击「验证」复制 authtoken
    echo   3. 执行: %CPOLAR% authtoken 你的token
    echo.
    set /p TOKEN=请输入你的 authtoken: 
    "%CPOLAR%" authtoken %TOKEN%
    if errorlevel 1 (
        echo [错误] authtoken 认证失败！
        pause
        exit /b 1
    )
    echo [OK] authtoken 认证成功！
) else (
    echo [OK] authtoken 已认证
)

echo.
echo [2/3] 启动后端隧道 (localhost:8084)...
start "HealthAgent-Backend-cpolar" "%CPOLAR%" http 8084 -log=stdout

timeout /t 3 /nobreak >nul

echo [3/3] 启动前端隧道 (localhost:5173)...
start "HealthAgent-Frontend-cpolar" "%CPOLAR%" http 5173 -log=stdout

echo.
echo ========================================
echo   隧道已启动！
echo ========================================
echo.
echo   查看公网地址:
echo     管理后台: http://localhost:9200
echo     官网后台: https://dashboard.cpolar.com/status
echo.
echo   地址格式: https://xxx.r1.cpolar.top
echo.
echo   [!] 重要提醒:
echo     1. 需修改前端 VITE_API_URL 指向后端公网地址
echo     2. 需修改后端 CORS allowedOrigins 加入前端公网域名
echo.
echo   关闭此窗口或新窗口即可停止隧道
echo ========================================
pause
