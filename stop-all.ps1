param(
    [switch]$PauseOnExit
)

Set-StrictMode -Version Latest
$ErrorActionPreference = "Continue" # Geändert auf Continue für robustere Ausführung

$root = Split-Path -Parent $MyInvocation.MyCommand.Path
$runDir = Join-Path $root ".run"
if (-not (Test-Path $runDir)) { New-Item -ItemType Directory -Path $runDir | Out-Null }
$sessionLog = Join-Path $runDir "stop-all.log"

function Write-Log {
    param([string]$Message)
    $line = "{0} {1}" -f (Get-Date -Format "yyyy-MM-dd HH:mm:ss"), $Message
    Write-Host $Message
    Add-Content -Path $sessionLog -Value $line -Encoding UTF8
}

Set-Content -Path $sessionLog -Value "" -Encoding UTF8
Write-Log "=== KITradingBot Shutdown ==="

$targetPorts = @(8000, 8080, 5173)
$stoppedAny = $false

foreach ($port in $targetPorts) {
    Write-Log "Überprüfe Port $port..."

    # Alle Prozesse finden, die diesen Port belegen (Listening)
    $connections = Get-NetTCPConnection -LocalPort $port -State Listen -ErrorAction SilentlyContinue

    if ($connections) {
        $pids = $connections | Select-Object -ExpandProperty OwningProcess -Unique

        foreach ($currentPid in $pids) { # Variable von $pid zu $currentPid geändert
            try {
                $proc = Get-Process -Id $currentPid -ErrorAction SilentlyContinue
                if ($proc) {
                    $procName = $proc.ProcessName
                    Write-Log "Beende Prozess: $procName (PID: $currentPid) auf Port $port"

                    # Beende den Prozess und alle seine Kind-Prozesse (wichtig für Maven/npm)
                    Stop-Process -Id $currentPid -Force
                    $stoppedAny = $true
                }
            }
            catch {
                Write-Log "Konnte PID $currentPid nicht beenden: $($_.Exception.Message)"
            }
        }
    } else {
        Write-Log "Port $port ist bereits frei."
    }
}

# Zusätzliche Sicherheit: Suche nach verwaisten Java oder Node Prozessen, die mit dem Bot zusammenhängen könnten
# (Optional, falls die Port-Suche fehlschlägt)

Write-Log "Warte kurz auf Freigabe der Ports..."
Start-Sleep -Seconds 2

# Finale Prüfung
$stillOpen = @()
foreach ($port in $targetPorts) {
    if (Get-NetTCPConnection -LocalPort $port -State Listen -ErrorAction SilentlyContinue) {
        $stillOpen += $port
    }
}

if ($stillOpen.Count -eq 0) {
    Write-Log "ERFOLG: Alle Ports (8000, 8080, 5173) sind frei."
} else {
    Write-Log "WARNUNG: Folgende Ports sind noch belegt: $($stillOpen -join ', ')"
}

Write-Log "=== Shutdown abgeschlossen ==="

if ($PauseOnExit) {
    Read-Host "Zum Beenden Enter drücken"
}
