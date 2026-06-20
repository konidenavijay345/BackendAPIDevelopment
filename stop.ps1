param(
    [Parameter(Position = 0)]
    [string] $Command
)

if ($Command -ne "backend") {
    Write-Host "Usage:"
    Write-Host "  .\stop backend"
    exit 1
}

$backendPort = 8080
$envFile = Join-Path $PSScriptRoot ".env"
if (Test-Path $envFile) {
    Get-Content $envFile | ForEach-Object {
        $line = $_.Trim()
        if (-not $line -or $line.StartsWith("#") -or -not $line.Contains("=")) {
            return
        }

        $name, $value = $line.Split("=", 2)
        if ($name -eq "SERVER_PORT") {
            $backendPort = [int] $value
        }
    }
}

try {
    Stop-Process -Name cloudflared -Force -ErrorAction Stop
    Write-Host "Public tunnel stopped."
} catch {
    Write-Host "Public tunnel is not running."
}

$listeners = Get-NetTCPConnection -LocalPort $backendPort -State Listen -ErrorAction SilentlyContinue
if (-not $listeners) {
    Write-Host "Backend is not running on port $backendPort."
    exit 0
}

$processIds = $listeners | Select-Object -ExpandProperty OwningProcess -Unique
foreach ($processId in $processIds) {
    Write-Host "Stopping backend on port $backendPort (PID $processId)..."
    Stop-Process -Id $processId -Force
}

Write-Host "Backend stopped."
