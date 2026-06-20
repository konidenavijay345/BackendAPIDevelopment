@echo off
setlocal

if /I "%~1"=="backend" goto backend
goto usage

:backend
set "BACKEND_MODE=%~2"
if "%BACKEND_MODE%"=="" set "BACKEND_MODE=staging"
if /I not "%BACKEND_MODE%"=="staging" if /I not "%BACKEND_MODE%"=="production" goto usage

if exist "%~dp0.env" (
  for /f "usebackq eol=# tokens=1,* delims==" %%A in ("%~dp0.env") do (
    if not "%%A"=="" set "%%A=%%B"
  )
)

if "%JWT_SECRET%"=="" (
  set "JWT_SECRET=dev-local-jwt-secret-change-me-32-chars"
)

if not "%MVN_CMD%"=="" (
  set "BACKEND_MVN=%MVN_CMD%"
) else if exist "D:\TestNgFramework\tools\apache-maven-3.9.9\bin\mvn.cmd" (
  set "BACKEND_MVN=D:\TestNgFramework\tools\apache-maven-3.9.9\bin\mvn.cmd"
) else (
  where mvn >nul 2>nul
  if "%ERRORLEVEL%"=="0" (
    set "BACKEND_MVN=mvn"
  ) else (
    set "BACKEND_MVN=%~dp0mvnw.cmd"
  )
)

set "BACKEND_PORT=8080"
if not "%SERVER_PORT%"=="" set "BACKEND_PORT=%SERVER_PORT%"

if /I "%BACKEND_MODE%"=="production" goto production

echo Starting backend locally on http://localhost:%BACKEND_PORT%
echo Logs will print in this console. Press Ctrl+C to stop.
call "%BACKEND_MVN%" spring-boot:run
exit /b %ERRORLEVEL%

:production
set "LOCAL_BASE_URL=http://localhost:%BACKEND_PORT%"
set "BACKEND_LOG=%~dp0backend-production.log"
set "TUNNEL_LOG=%~dp0cloudflared-production.log"
set "CLOUDFLARED=%~dp0tools\cloudflared.exe"

for /f "tokens=5" %%P in ('netstat -ano ^| findstr /R /C:":%BACKEND_PORT% .*LISTENING"') do (
  set "BACKEND_ALREADY_RUNNING=1"
)

if "%BACKEND_ALREADY_RUNNING%"=="1" (
  echo Backend already running on %LOCAL_BASE_URL%.
) else (
  echo Starting backend on %LOCAL_BASE_URL%...
  start "Backend API" /min cmd /c ""%BACKEND_MVN%" spring-boot:run > "%BACKEND_LOG%" 2>&1"
)

echo Waiting for backend health check...
for /L %%I in (1,1,45) do (
  powershell -NoProfile -Command "try { $r = Invoke-WebRequest -UseBasicParsing '%LOCAL_BASE_URL%/actuator/health' -TimeoutSec 2; if ($r.StatusCode -eq 200) { exit 0 } } catch { exit 1 }" >nul 2>nul
  if not errorlevel 1 goto backend_ready
  powershell -NoProfile -Command "Start-Sleep -Seconds 1" >nul 2>nul
)

echo Backend did not become healthy. Check "%BACKEND_LOG%".
exit /b 1

:backend_ready
if not exist "%CLOUDFLARED%" (
  where cloudflared >nul 2>nul
  if errorlevel 1 (
    echo cloudflared was not found. Expected "%CLOUDFLARED%" or cloudflared on PATH.
    exit /b 1
  )
  set "CLOUDFLARED=cloudflared"
)

powershell -NoProfile -Command "Stop-Process -Name cloudflared -Force -ErrorAction SilentlyContinue" >nul 2>nul
if exist "%TUNNEL_LOG%" del "%TUNNEL_LOG%" >nul 2>nul

echo Starting public tunnel...
start "Backend Public Tunnel" /min "%CLOUDFLARED%" tunnel --url "%LOCAL_BASE_URL%" --logfile "%TUNNEL_LOG%"

for /L %%I in (1,1,45) do (
  for /f "tokens=*" %%U in ('powershell -NoProfile -Command "if (Test-Path -LiteralPath '%TUNNEL_LOG%') { $text = Get-Content -Raw -LiteralPath '%TUNNEL_LOG%'; $m = [regex]::Match($text, 'https://[-a-z0-9]+\.trycloudflare\.com'); if ($m.Success) { $m.Value } }" 2^>nul') do (
    echo.
    echo Production base URL:
    echo %%U
    echo.
    echo Health check:
    echo %%U/actuator/health
    exit /b 0
  )
  powershell -NoProfile -Command "Start-Sleep -Seconds 1" >nul 2>nul
)

echo Tunnel started, but no public URL was found yet. Check "%TUNNEL_LOG%".
exit /b 1

:usage
echo Usage:
echo   run backend staging
echo   run backend production
exit /b 1
