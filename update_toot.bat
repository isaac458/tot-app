@echo off
setlocal enabledelayedexpansion
color 0B
echo.
echo   ##########################################
echo   #       🦜 TOOT APP - AUTO RELEASE       #
echo   #       Build, Push \u0026 Upload APK         #
echo   ##########################################
echo.

cd /d "%~dp0"

rem 1. طلب رقم الإصدار
set /p raw_ver="[1/2] Enter New Version (e.g., 4.3): "
set /p msg="[2/2] Enter Release Notes (Brief): "

set ver=!raw_ver!
if "!ver:~0,1!"=="v" set ver=!ver:~1!

echo [+] Target Version: v!ver!
echo [+] Updating Version in Gradle...
powershell -Command "(gc app/build.gradle.kts) -replace 'versionName = \".*\"', 'versionName = \"!ver!\"' | Out-File -encoding UTF8 app/build.gradle.kts"

echo [+] 🛠 BUILDING APK LOCAL...
call gradlew.bat assembleDebug

if %ERRORLEVEL% NEQ 0 (
    echo [!] Build failed!
    pause
    exit /b %ERRORLEVEL%
)

echo [+] Saving Source Code to GitHub...
git add .
git commit -m "Release v!ver!: !msg!"
git tag -d v!ver! 2>nul
git tag v!ver!
git push origin main --tags --force

echo [+] 🚀 UPLOADING APK TO GITHUB RELEASES...
where gh >nul 2>nul
if %ERRORLEVEL% EQU 0 (
    echo [+] Creating GitHub Release...
    gh release create v!ver! "app\build\outputs\apk\debug\app-debug.apk" --title "Toot App v!ver!" --notes "!msg!"
    echo [OK] APK Uploaded Successfully!
) else (
    echo.
    echo [!] GitHub CLI (gh) not found.
    echo [!] Please install it from: https://cli.github.com/
    echo [!] For now, upload the APK manually to: https://github.com/isaac458/tot-app/releases
)

echo.
echo [DONE] Version v!ver! is live!
echo [!] Opening Releases page...
start https://github.com/isaac458/tot-app/releases
pause
