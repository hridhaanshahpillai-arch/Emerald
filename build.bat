@echo off
rem Double-click to build Emerald (close Emerald first: Windows won't let the build replace it while it runs)
cd /d "%~dp0"
powershell -NoProfile -ExecutionPolicy Bypass -File "%~dp0build.ps1"
echo.
pause
