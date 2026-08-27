[CmdletBinding()]
param(
    [Parameter(Mandatory = $true)]
    [string]$BackupDirectory,
    [string]$ComposeFile = (Join-Path $PSScriptRoot "..\compose.production.yml"),
    [string]$EnvFile,
    [string]$ProjectName,
    [string]$Database,
    [switch]$SkipUploads,
    [switch]$NoRestart,
    [Parameter(Mandatory = $true)]
    [switch]$ConfirmDestructiveRestore
)

$ErrorActionPreference = "Stop"

function Invoke-Docker {
    param([string[]]$Arguments)

    & docker @Arguments
    if ($LASTEXITCODE -ne 0) {
        throw "docker command failed with exit code $LASTEXITCODE"
    }
}

function Get-DockerOutput {
    param([string[]]$Arguments)

    $output = & docker @Arguments
    if ($LASTEXITCODE -ne 0) {
        throw "docker command failed with exit code $LASTEXITCODE"
    }
    return $output
}

if (-not $ConfirmDestructiveRestore) {
    throw "Pass -ConfirmDestructiveRestore to acknowledge that the target database and uploads will be replaced."
}

$backupPath = [IO.Path]::GetFullPath($BackupDirectory)
$composePath = [IO.Path]::GetFullPath($ComposeFile)
if (-not (Test-Path -LiteralPath $backupPath -PathType Container)) {
    throw "Backup directory does not exist: $backupPath"
}
if (-not (Test-Path -LiteralPath $composePath -PathType Leaf)) {
    throw "Compose file does not exist: $composePath"
}

$composeArgs = @("compose")
if ($ProjectName) {
    $composeArgs += @("--project-name", $ProjectName)
}
if ($EnvFile) {
    $envPath = [IO.Path]::GetFullPath($EnvFile)
    if (-not (Test-Path -LiteralPath $envPath -PathType Leaf)) {
        throw "Environment file does not exist: $envPath"
    }
    $composeArgs += @("--env-file", $envPath)
}
$composeArgs += @("-f", $composePath)

$manifestPath = Join-Path $backupPath "SHA256SUMS"
if (-not (Test-Path -LiteralPath $manifestPath -PathType Leaf)) {
    throw "Missing checksum manifest: $manifestPath"
}

foreach ($line in Get-Content -LiteralPath $manifestPath) {
    if ($line -notmatch '^([0-9a-fA-F]{64})  ([A-Za-z0-9._-]+)$') {
        throw "Invalid checksum line: $line"
    }
    $expected = $Matches[1].ToLowerInvariant()
    $name = $Matches[2]
    $file = Join-Path $backupPath $name
    if (-not (Test-Path -LiteralPath $file -PathType Leaf)) {
        throw "Backup artifact is missing: $name"
    }
    $actual = (Get-FileHash -LiteralPath $file -Algorithm SHA256).Hash.ToLowerInvariant()
    if ($actual -ne $expected) {
        throw "Checksum mismatch: $name"
    }
}

$databaseArchive = Join-Path $backupPath "database.sql.gz"
$uploadsArchive = Join-Path $backupPath "uploads.tar.gz"
if (-not (Test-Path -LiteralPath $databaseArchive -PathType Leaf)) {
    throw "Missing database archive."
}
if (-not $SkipUploads -and -not (Test-Path -LiteralPath $uploadsArchive -PathType Leaf)) {
    throw "Missing uploads archive."
}

$runningServices = @(Get-DockerOutput ($composeArgs + @("ps", "--status", "running", "--services")))
if ($runningServices -notcontains "mysql") {
    throw "The mysql service must be running before restore."
}

if (-not $Database) {
    $Database = (Get-DockerOutput ($composeArgs + @(
        "exec", "-T", "mysql", "sh", "-ec", 'printf %s "$MYSQL_DATABASE"'
    )) | Out-String).Trim()
}
if ($Database -notmatch '^[A-Za-z_][A-Za-z0-9_]*$') {
    throw "Database must start with a letter or underscore and contain only letters, digits, and underscores."
}

$configuredServices = @(Get-DockerOutput ($composeArgs + @("config", "--services")))
$writeServices = @("caddy", "frontend", "backend") | Where-Object { $configuredServices -contains $_ }
$runningWriteServices = @($writeServices | Where-Object { $runningServices -contains $_ })
if ($runningWriteServices.Count -gt 0) {
    Invoke-Docker ($composeArgs + @("stop") + $runningWriteServices)
}

$nonce = [Guid]::NewGuid().ToString("N")
$databaseTemp = "/tmp/csa-restore-$nonce.sql.gz"
$databaseScriptTemp = "/tmp/csa-db-restore-$nonce.sh"
$uploadHelper = "csa-upload-restore-$nonce"

try {
    Invoke-Docker ($composeArgs + @("cp", $databaseArchive, "mysql:$databaseTemp"))
    $databaseScript = Join-Path $PSScriptRoot "container\mysql-restore.sh"
    Invoke-Docker ($composeArgs + @("cp", $databaseScript, "mysql:$databaseScriptTemp"))
    Invoke-Docker ($composeArgs + @(
        "exec", "-T", "mysql", "sh", $databaseScriptTemp, $Database, $databaseTemp
    ))

    if (-not $SkipUploads) {
        $backendId = (Get-DockerOutput ($composeArgs + @("ps", "-aq", "backend")) | Select-Object -First 1)
        if (-not $backendId) {
            throw "Cannot find the backend container needed to resolve the uploads volume."
        }
        $inspect = (Get-DockerOutput @("inspect", $backendId) | Out-String | ConvertFrom-Json)[0]
        $uploadMount = $inspect.Mounts | Where-Object { $_.Destination -eq "/app/uploads" } | Select-Object -First 1
        if (-not $uploadMount -or -not $uploadMount.Name) {
            throw "Cannot resolve the named uploads volume."
        }

        Invoke-Docker @(
            "create", "--name", $uploadHelper,
            "--mount", "type=volume,src=$($uploadMount.Name),dst=/restore",
            "alpine:3.20", "sh", "/tmp/uploads-restore.sh"
        )
        $uploadsScript = Join-Path $PSScriptRoot "container\uploads-restore.sh"
        Invoke-Docker @("cp", $uploadsScript, "${uploadHelper}:/tmp/uploads-restore.sh")
        Invoke-Docker @("cp", $uploadsArchive, "${uploadHelper}:/tmp/uploads.tar.gz")
        Invoke-Docker @("start", "-a", $uploadHelper)
    }
}
catch {
    Write-Error "Restore failed. Application services remain stopped; recover from the last verified backup before restarting. $_"
    throw
}
finally {
    & docker @($composeArgs + @("exec", "-T", "mysql", "rm", "-f", $databaseTemp, $databaseScriptTemp)) 2>$null | Out-Null
    $helperId = & docker ps -aq --filter "name=^/$uploadHelper$"
    if ($helperId) {
        & docker rm -f $helperId | Out-Null
    }
}

if (-not $NoRestart -and $runningWriteServices.Count -gt 0) {
    Invoke-Docker ($composeArgs + @("up", "-d") + $runningWriteServices)
}

Write-Output "Restore completed and checksums verified: database=$Database uploads=$(-not $SkipUploads)"
