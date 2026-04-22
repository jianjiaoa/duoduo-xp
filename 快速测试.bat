@echo off
title 检测 JD 类 - 快速测试
color 0A
echo.
echo ========================================
echo   检测 com.zx.flg.JD 类 - 快速测试
echo ========================================
echo.

echo [1/6] 编译新版本...
call gradlew assembleRelease
if errorlevel 1 (
    echo ❌ 编译失败！
    pause
    exit
)
echo ✅ 编译成功
echo.

echo [2/6] 安装到手机...
adb install -r app\release\duoduo-xp.apk
if errorlevel 1 (
    echo ⚠️ 安装失败，尝试卸载后重装...
    adb uninstall com.jianjiao.duoduo
    adb install app\release\duoduo-xp.apk
)
echo ✅ 安装完成
echo.

echo [3/6] 停止京东app...
adb shell am force-stop com.jingdong.app.mall
timeout /t 2 >nul
echo ✅ 已停止
echo.

echo [4/6] 清除旧日志...
adb logcat -c
echo ✅ 日志已清除
echo.

echo [5/6] 启动京东app...
adb shell am start -n com.jingdong.app.mall/.MainActivity
timeout /t 3 >nul
echo ✅ 已启动
echo.

echo [6/6] 开始监控日志...
echo.
echo ========================================
echo   实时日志输出 (按Ctrl+C停止)
echo ========================================
echo.

adb logcat | findstr /C:"FinalDetector" /C:"🔔" /C:"🎯" /C:"📤" /C:"✅ JD"

