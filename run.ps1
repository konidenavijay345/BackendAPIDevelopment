param(
    [Parameter(Position = 0)]
    [string] $Command,

    [Parameter(Position = 1)]
    [ValidateSet("staging", "production")]
    [string] $Mode = "staging"
)

if ($Command -ne "backend") {
    Write-Host "Usage:"
    Write-Host "  .\run backend staging"
    Write-Host "  .\run backend production"
    exit 1
}

$pathValue = [Environment]::GetEnvironmentVariable("Path", "Process")
if (-not $pathValue) {
    $pathValue = [Environment]::GetEnvironmentVariable("PATH", "Process")
}
if ($pathValue) {
    [Environment]::SetEnvironmentVariable("PATH", $null, "Process")
    [Environment]::SetEnvironmentVariable("Path", $pathValue, "Process")
}

$envFile = Join-Path $PSScriptRoot ".env"
if (Test-Path $envFile) {
    Get-Content $envFile | ForEach-Object {
        $line = $_.Trim()
        if (-not $line -or $line.StartsWith("#") -or -not $line.Contains("=")) {
            return
        }

        $name, $value = $line.Split("=", 2)
        Set-Item -Path "Env:$name" -Value $value
    }
}

if (-not $env:JWT_SECRET) {
    $env:JWT_SECRET = "dev-local-jwt-secret-change-me-32-chars"
}

$mavenCommand = if ($env:MVN_CMD) {
    $env:MVN_CMD
} elseif (Test-Path "D:\TestNgFramework\tools\apache-maven-3.9.9\bin\mvn.cmd") {
    "D:\TestNgFramework\tools\apache-maven-3.9.9\bin\mvn.cmd"
} elseif (Get-Command mvn -ErrorAction SilentlyContinue) {
    "mvn"
} else {
    "$PSScriptRoot\mvnw.cmd"
}

$backendPort = if ($env:SERVER_PORT) { [int] $env:SERVER_PORT } else { 8080 }
$localBaseUrl = "http://localhost:$backendPort"

if ($Mode -eq "staging") {
    Write-Host "Starting backend locally on $localBaseUrl"
    Write-Host "Logs will print in this console. Press Ctrl+C to stop."
    & $mavenCommand spring-boot:run
    exit $LASTEXITCODE
}

$backendLog = Join-Path $PSScriptRoot "backend-production.log"
$backendErrorLog = Join-Path $PSScriptRoot "backend-production.err.log"
$tunnelLog = Join-Path $PSScriptRoot "cloudflared-production.log"
$cloudflared = Join-Path $PSScriptRoot "tools\cloudflared.exe"

$listener = Get-NetTCPConnection -LocalPort $backendPort -State Listen -ErrorAction SilentlyContinue
if ($listener) {
    Write-Host "Backend already running on $localBaseUrl."
} else {
    Write-Host "Starting backend on $localBaseUrl..."
    Start-Process -FilePath $mavenCommand -ArgumentList "spring-boot:run" -WorkingDirectory $PSScriptRoot -RedirectStandardOutput $backendLog -RedirectStandardError $backendErrorLog -WindowStyle Hidden
}

Write-Host "Waiting for backend health check..."
$healthy = $false
for ($i = 0; $i -lt 45; $i++) {
    try {
        $response = Invoke-WebRequest -UseBasicParsing "$localBaseUrl/actuator/health" -TimeoutSec 2
        if ($response.StatusCode -eq 200) {
            $healthy = $true
            break
        }
    } catch {
        Start-Sleep -Seconds 1
    }
}

if (-not $healthy) {
    Write-Host "Backend did not become healthy. Check $backendLog."
    exit 1
}

if (-not (Test-Path $cloudflared)) {
    $command = Get-Command cloudflared -ErrorAction SilentlyContinue
    if (-not $command) {
        Write-Host "cloudflared was not found. Expected $cloudflared or cloudflared on PATH."
        exit 1
    }
    $cloudflared = $command.Source
}

Stop-Process -Name cloudflared -Force -ErrorAction SilentlyContinue
Remove-Item $tunnelLog -Force -ErrorAction SilentlyContinue

Write-Host "Starting public tunnel..."
Start-Process -FilePath $cloudflared -ArgumentList "tunnel --url $localBaseUrl --logfile `"$tunnelLog`"" -WorkingDirectory $PSScriptRoot -WindowStyle Hidden

for ($i = 0; $i -lt 45; $i++) {
    if (Test-Path $tunnelLog) {
        $match = Select-String -Path $tunnelLog -Pattern "https://[-a-z0-9]+\.trycloudflare\.com" -AllMatches -ErrorAction SilentlyContinue | Select-Object -Last 1
        if ($match) {
            $url = $match.Matches[0].Value
            Write-Host ""
            Write-Host "Production base URL:"
            Write-Host $url
            Write-Host ""
            Write-Host "Health check:"
            Write-Host "$url/actuator/health"
            exit 0
        }
    }
    Start-Sleep -Seconds 1
}

Write-Host "Tunnel started, but no public URL was found yet. Check $tunnelLog."
exit 1
