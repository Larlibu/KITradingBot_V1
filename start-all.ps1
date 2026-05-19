param(
    [switch]$NoTrain,
    [switch]$PauseOnExit
)

Set-StrictMode -Version Latest
$ErrorActionPreference = "Continue"

$root = Split-Path -Parent $MyInvocation.MyCommand.Path
$runDir = Join-Path $root ".run"
if (-not (Test-Path $runDir)) { New-Item -ItemType Directory -Path $runDir | Out-Null }
$sessionLog = Join-Path $runDir "start-all.log"

function Write-Log {
    param([string]$Message)
    $line = "{0} {1}" -f (Get-Date -Format "yyyy-MM-dd HH:mm:ss"), $Message
    Write-Host $Message
    Add-Content -Path $sessionLog -Value $line -Encoding UTF8
}

function Test-PortInUse {
    param([int]$Port)
    return [bool](Get-NetTCPConnection -LocalPort $Port -State Listen -ErrorAction SilentlyContinue)
}

function Wait-ForPort {
    param(
        [int]$Port,
        [int]$TimeoutSeconds = 60
    )
    $deadline = (Get-Date).AddSeconds($TimeoutSeconds)
    while ((Get-Date) -lt $deadline) {
        if (Test-PortInUse -Port $Port) {
            return $true
        }
        Start-Sleep -Milliseconds 1000
    }
    return $false
}

Set-Content -Path $sessionLog -Value "" -Encoding UTF8
Write-Log "=== KITradingBot Startup ==="

# --- NEU: Zuerst alle alten Instanzen beenden ---
Write-Log "Stoppe eventuell laufende Dienste vor dem Neustart..."
& (Join-Path $root "stop-all.ps1")

try {
    # 1. ML Service (Python)
    $mlDir = Join-Path $root "ml-service"
    Write-Log "[ml-service] Starte Python API..."
    $mlStdout = Join-Path $runDir "ml-service.out.log"
    $mlStderr = Join-Path $runDir "ml-service.err.log"
    Start-Process -FilePath "python" -ArgumentList "-m", "uvicorn", "api:app", "--host", "0.0.0.0", "--port", "8000" `
        -WorkingDirectory $mlDir -WindowStyle Hidden -RedirectStandardOutput $mlStdout -RedirectStandardError $mlStderr

    if (Wait-ForPort -Port 8000) {
        Write-Log "[ml-service] ONLINE (Port 8000)"
    } else {
        Write-Log "[ml-service] FEHLER: Startet nicht innerhalb von 60s."
    }

    # 2. Backend (Java/Spring)
    $backendDir = Join-Path $root "backend"
    Write-Log "[backend] Starte Spring Boot..."
    $backStdout = Join-Path $runDir "backend.out.log"
    $backStderr = Join-Path $runDir "backend.err.log"
    Start-Process -FilePath "cmd.exe" -ArgumentList "/c", "mvn.cmd", "spring-boot:run" `
        -WorkingDirectory $backendDir -WindowStyle Hidden -RedirectStandardOutput $backStdout -RedirectStandardError $backStderr

    if (Wait-ForPort -Port 8080 -TimeoutSeconds 90) {
        Write-Log "[backend] ONLINE (Port 8080)"
    } else {
        Write-Log "[backend] FEHLER: Spring Boot startet nicht innerhalb von 90s."
    }

    # 3. Frontend (React/Vite)
    $frontendDir = Join-Path $root "frontend"
    Write-Log "[frontend] Starte React/Vite Dev Server (host: 0.0.0.0)..."
    $frontStdout = Join-Path $runDir "frontend.out.log"
    $frontStderr = Join-Path $runDir "frontend.err.log"
    Start-Process -FilePath "cmd.exe" -ArgumentList "/c", "npm.cmd", "run", "dev", "--", "--host", "0.0.0.0" `
        -WorkingDirectory $frontendDir -WindowStyle Hidden -RedirectStandardOutput $frontStdout -RedirectStandardError $frontStderr

    if (Wait-ForPort -Port 5173) {
        Write-Log "[frontend] ONLINE (Port 5173)"
    } else {
        Write-Log "[frontend] FEHLER: Frontend startet nicht innerhalb von 60s."
    }

    Write-Log "=== Startup abgeschlossen ==="
    Write-Log "Versuche diese URL: http://localhost:5173"
}
catch {
    Write-Log "KRITISCHER FEHLER: $($_.Exception.Message)"
}
finally {
    if ($PauseOnExit) {
        Read-Host "Zum Beenden Enter drücken"
    }
}
