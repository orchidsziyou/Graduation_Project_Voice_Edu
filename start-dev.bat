@echo off
echo ========================================
echo   语音转写系统 - 前后端分离启动脚本
echo ========================================
echo.

echo [1/3] 检查 Node.js 安装...
node --version >nul 2>&1
if %errorlevel% neq 0 (
    echo ❌ 错误：未检测到 Node.js，请先安装 Node.js v20.19.0 或更高版本
    echo 下载地址：https://nodejs.org/
    pause
    exit /b 1
)
echo ✅ Node.js 已安装

echo.
echo [2/3] 启动后端服务...
start "后端服务 - http://localhost:8080" cmd /k "mvnw.cmd spring-boot:run"
timeout /t 3 /nobreak >nul

echo.
echo [3/3] 启动前端服务...
cd vue-frontend
if not exist node_modules (
    echo 首次运行，正在安装依赖...
    call npm install
)
start "前端服务 - http://localhost:5173" cmd /k "npm run dev"

echo.
echo ========================================
echo   启动完成！
echo ========================================
echo.
echo 🔹 后端地址：http://localhost:8080
echo 🔹 前端地址：http://localhost:5173
echo.
echo 按任意键退出此窗口...
pause >nul
