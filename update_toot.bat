@echo off
color 0B
echo.
echo   ##########################################
echo   #       🦜 TOOT APP - CLEAN PUSH v4.2     #
echo   #       WORKING FROM DRIVE C ONLY        #
echo   ##########################################
echo.

rem Force go to C: drive project path
cd /d "C:\Users\PC\AndroidStudioProjects\MyApplication"

echo [+] Removing any old tags locally...
git tag -d v4.2 2>nul

echo [+] Adding all clean files from C...
git add .

set /p msg="[?] Enter Update Message: "
if "%msg%"=="" set msg="Clean Release v4.2 from Drive C"

echo [+] Committing...
git commit -m "%msg%"

echo [+] Tagging as v4.2...
git tag v4.2

echo [+] Pushing to GitHub (Forcing C drive version)...
git push origin master:main --tags --force

echo.
echo [OK] Everything is now on Drive C and GitHub is updated!
pause
