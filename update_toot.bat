@echo off
setlocal enabledelayedexpansion
color 0B
echo.
echo   ##########################################
echo   #       🦜 TOOT APP - BUILD \u0026 UPDATE      #
echo   #       Path: Drive D (LATEST)           #
echo   ##########################################
echo.

cd /d "%~dp0"

rem 1. طلب رقم الإصدار
set /p raw_ver="[1/2] Enter New Version (e.g., 4.3): "
set /p msg="[2/2] Enter Update Message: "

set ver=!raw_ver!
if "!ver:~0,1!"=="v" set ver=!ver:~1!

echo [+] Target Version: !ver!
echo [+] Updating Version in Code...
powershell -Command "(gc app/build.gradle.kts) -replace 'versionName = \".*\"', 'versionName = \"!ver!\"' | Out-File -encoding UTF8 app/build.gradle.kts"

echo [+] 🛠 BUILDING APK LOCAL...
call gradlew.bat assembleDebug

if %ERRORLEVEL% NEQ 0 (
    echo [!] Build failed. Checking local.properties...
    if not exist local.properties (
        echo [!] local.properties missing! Please create it with sdk.dir and AI_API_KEY.
    )
    pause
    exit /b %ERRORLEVEL%
)

echo [+] Saving Source Code to GitHub...
git add .
git commit -m "v!ver!: !msg!"

echo [+] Syncing Tags...
git tag -d v!ver! 2>nul
git tag v!ver!

echo [+] Pushing to GitHub...
git push origin main --tags --force

echo.
echo [OK] Done! Version v!ver! is now live.
echo [!] Opening APK folder...
explorer "app\build\outputs\apk\debug"
pause
