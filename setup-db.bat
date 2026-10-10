@echo off
rem Double-click or run from a terminal. Bypasses the PowerShell script-execution policy for this one script.
powershell -NoProfile -ExecutionPolicy Bypass -File "%~dp0setup-db.ps1" %*
pause
