@echo off
setlocal
cd /d "%~dp0"
powershell -ExecutionPolicy Bypass -NoProfile -File ".\start-all.ps1" -PauseOnExit
echo.
pause
