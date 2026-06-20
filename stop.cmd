@echo off
setlocal

if /I "%~1"=="backend" goto backend
goto usage

:backend
set "BACKEND_PORT=8080"

if exist "%~dp0.env" (
  for /f "usebackq eol=# tokens=1,* delims==" %%A in ("%~dp0.env") do (
    if /I "%%A"=="SERVER_PORT" set "BACKEND_PORT=%%B"
  )
)

powershell -NoProfile -Command "Stop-Process -Name cloudflared -Force -ErrorAction Stop" >nul 2>nul
if errorlevel 1 (
  echo Public tunnel is not running.
) else (
  echo Public tunnel stopped.
)

for /f "tokens=5" %%P in ('netstat -ano ^| findstr /R /C:":%BACKEND_PORT% .*LISTENING"') do (
  echo Stopping backend on port %BACKEND_PORT% ^(PID %%P^)...
  powershell -NoProfile -Command "Stop-Process -Id %%P -Force -ErrorAction Stop" >nul 2>nul
  if errorlevel 1 (
    echo Failed to stop backend. Try running this command from an Administrator terminal.
    exit /b 1
  )
  echo Backend stopped.
  exit /b 0
)

echo Backend is not running on port %BACKEND_PORT%.
exit /b 0

:usage
echo Usage:
echo   stop backend
exit /b 1
