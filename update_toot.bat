@echo off
color 0B
echo.
echo   ##########################################
echo   #       🦜 TOOT APP - BUILD \u0026 UPDATE      #
echo   #    (Fixing API \u0026 Registration Issues)    #
echo   ##########################################
echo.

cd /d "C:\Users\PC\AndroidStudioProjects\MyApplication"

set /p ver="[1/2] Enter Version Number (e.g., 4.2): "
set /p msg="[2/2] What did you change? "

echo [+] Updating Version in Gradle...
powershell -Command "(gc app/build.gradle.kts) -replace 'versionName = \".*\"', 'versionName = \"%ver%\"' | Out-File -encoding UTF8 app/build.gradle.kts"

echo [+] 🛠 BUILDING APK (This ensures your API Key is included)...
call gradlew assembleDebug

echo [+] Saving Source Code to GitHub...
git add .
git commit -m "v%ver%: %msg%"
git tag v%ver%
git push origin main --tags --force

echo.
echo [OK] Everything is updated!
echo [!] NOW: Copy 'app-debug.apk' from the opened folder and upload it to GitHub.
explorer "app\build\outputs\apk\debug"
pause
