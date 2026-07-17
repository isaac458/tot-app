@echo off
setlocal enabledelayedexpansion
color 0B
echo.
echo   ##########################################
echo   #       🦜 TOOT APP - AUTO RELEASE       #
echo   #       LOCATION: DRIVE D (FIXED PATH)   #
echo   ##########################################
echo.

cd /d "%~dp0"

set "GH_BIN=bin\gh.exe"

echo [+] Checking local.properties...
if not exist local.properties (
    echo sdk.dir=C\:\\Users\\PC\\AppData\\Local\\Android\\Sdk > local.properties
)
findstr "AI_API_KEY" local.properties >nul
if %ERRORLEVEL% NEQ 0 (
    echo [?] AI_API_KEY is missing.
    set /p "user_key=Please enter your Groq API Key: "
    set "P1=AI_API"
    set "P2=KEY"
    echo !P1!_!P2!=!user_key! >> local.properties
)

echo [+] Stopping Gradle...
call gradlew.bat --stop >nul 2>&1

echo [+] Accepting Licenses (Standard Path)...
cmd /c "(echo y & echo y & echo y & echo y & echo y & echo y) | C:\Users\PC\AppData\Local\Android\Sdk\cmdline-tools\latest\bin\sdkmanager.bat --licenses" >nul 2>&1

set /p raw_ver="[1/2] Enter New Version (e.g., 4.4): "
set /p msg="[2/2] Enter Release Notes: "
set ver=!raw_ver!
if "!ver:~0,1!"=="v" set ver=!ver:~1!

echo [+] Updating Version to v!ver!...
set "v_code=!ver:.=!"
powershell -Command "$content = gc app/build.gradle.kts; $content = $content -replace 'versionName = \".*\"', 'versionName = \"v!ver!\"'; $content = $content -replace 'versionCode = \d+', 'versionCode = !v_code!'; $content | Out-File -encoding UTF8 app/build.gradle.kts"

echo [+] 🛠 BUILDING APK...
echo [!] This is a fresh build, please wait...
call gradlew.bat clean assembleDebug --no-daemon

if %ERRORLEVEL% NEQ 0 (
    echo [!] Build failed. Please check if the space in SDK path is gone.
    pause
    exit /b %ERRORLEVEL%
)

echo [+] Saving Source Code to GitHub...
git add .
git commit -m "Release v!ver!: !msg!"
git tag -d v!ver! 2>nul
git tag v!ver!
git push origin main --tags --force

echo [+] 🚀 UPLOADING APK TO GITHUB...
if exist "%GH_BIN%" (
    "%GH_BIN%" release delete v!ver! --yes 2>nul
    "%GH_BIN%" release create v!ver! "app\build\outputs\apk\debug\app-debug.apk" --title "Toot App v!ver!" --notes "!msg!"
    echo [OK] v!ver! Uploaded Successfully!
)

echo.
echo [DONE] Visit: https://github.com/isaac458/tot-app/releases
pause
