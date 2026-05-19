@echo off
setlocal
cd /d "%~dp0"
powershell -ExecutionPolicy Bypass -NoProfile -File ".\stop-all.ps1" -PauseOnExit
♦