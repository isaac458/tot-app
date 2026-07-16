@echo off
set JAVA_HOME=D:\AndroidStudio_Install\jbr
set PATH=%JAVA_HOME%\bin;%PATH%

echo [1/5] Cleaning old builds...
call gradlew clean
if errorlevel 1 goto :error

echo [2/5] Building Fresh APK with YOUR local keys...
call gradlew assembleDebug
if errorlevel 1 goto :error

echo.
echo [3/5] Adding changes to Git...
git add .

echo [4/5] Committing and Tagging...
set /p msg="What changed in this version? "
set /p tag="Enter version (e.g. v4.1): "
git commit -m "%msg%"
git tag %tag%

echo [5/5] Pushing to GitHub (Code + Tag)...
git push origin main
git push origin %tag%

echo.
echo ======================================================
echo SUCCESS! Your local APK (with your keys) is uploaded.
echo Now download the APK from the Releases page on GitHub.
echo ======================================================
start https://github.com/isaac458/Toot-app/releases
pause
exit /b 0

:error
echo.
echo ******************************************************
echo  BUILD FAILED! The APK was NOT created.
echo  Please check the error messages above.
echo ******************************************************
pause
exit /b 1
