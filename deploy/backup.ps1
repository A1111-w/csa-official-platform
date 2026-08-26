[CmdletBinding()]
param(
    [string]$ComposeFile = (Join-Path $PSScriptRoot "..\compose.production.yml"),
    [string]$EnvFile,
    [string]$ProjectName,
    [string]$OutputRoot = (Join-Path $PSScriptRoot "..\backups"),
    [ValidateRange(1, 3650)]
    [int]$RetentionDays = 30
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

$composePath = [IO.Path]::GetFullPath($ComposeFile)
if (-not (Test-Path -LiteralPath $composePath -PathType Leaf)) {
    throw "Compose file does not exist: $composePath"
}

$backupRoot = [IO.Path]::GetFullPath($OutputRoot)
if ($backupRoot -eq [IO.Path]::GetPathRoot($backupRoot)) {
    throw "OutputRoot must not be a filesystem root."
}
New-Item -ItemType Directory -Force -Path $backupRoot | Out-Null

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

$runningServices = @(Get-DockerOutput ($composeArgs + @("ps", "--status", "running", "--services")))
foreach ($requiredService in @("mysql", "backend")) {
    if ($runningServices -notcontains $requiredService) {
        throw "Required service is not running: $requiredService"
    }
}

$timestamp = (Get-Date).ToUniversalTime().ToString("yyyyMMddTHHmmssZ")
$destination = Join-Path $backupRoot $timestamp
New-Item -ItemType Directory -Path $destination | Out-Null

$nonce = [Guid]::NewGuid().ToString("N")
$databaseTemp = "/tmp/csa-db-$nonce.sql.gz"
$databaseScriptTemp = "/tmp/csa-db-backup-$nonce.sh"
$uploadsTempDirectory = "/app/uploads/.csa-backup-tmp"
$uploadsTemp = "$uploadsTempDirectory/csa-uploads-$nonce.tar.gz"
$databaseFile = Join-Path $destination "database.sql.gz"
$uploadsFile = Join-Path $destination "uploads.tar.gz"

try {
    $databaseScript = Join-Path $PSScriptRoot "container\mysql-backup.sh"
    Invoke-Docker ($composeArgs + @("cp", $databaseScript, "mysql:$databaseScriptTemp"))
    Invoke-Docker ($composeArgs + @("exec", "-T", "mysql", "sh", $databaseScriptTemp, $databaseTemp))
    Invoke-Docker ($composeArgs + @("cp", "mysql:$databaseTemp", $databaseFile))

    Invoke-Docker ($composeArgs + @("exec", "-T", "backend", "mkdir", "-p", $uploadsTempDirectory))
    Invoke-Docker ($composeArgs + @(
        "exec", "-T", "backend", "tar", "-C", "/app/uploads",
        "--exclude=./.csa-backup-tmp", "-czf", $uploadsTemp, "."
    ))
    Invoke-Docker ($composeArgs + @("cp", "backend:$uploadsTemp", $uploadsFile))
}
finally {
    & docker @($composeArgs + @("exec", "-T", "mysql", "rm", "-f", $databaseTemp, $databaseScriptTemp)) 2>$null | Out-Null
    & docker @($composeArgs + @("exec", "-T", "backend", "rm", "-f", $uploadsTemp)) 2>$null | Out-Null
    & docker @($composeArgs + @("exec", "-T", "backend", "rmdir", $uploadsTempDirectory)) 2>$null | Out-Null
}

$databaseName = (Get-DockerOutput ($composeArgs + @(
    "exec", "-T", "mysql", "sh", "-ec", 'printf %s "$MYSQL_DATABASE"'
)) | Out-String).Trim()

$metadata = [ordered]@{
    formatVersion = 1
    createdAtUtc = (Get-Date).ToUniversalTime().ToString("o")
    database = $databaseName
    databaseArchive = "database.sql.gz"
    uploadsArchive = "uploads.tar.gz"
    consistency = "mysqldump --single-transaction plus an independent uploads snapshot"
}
$metadata | ConvertTo-Json | Set-Content -LiteralPath (Join-Path $destination "metadata.json") -Encoding UTF8

$checksumFiles = @("database.sql.gz", "uploads.tar.gz", "metadata.json")
$checksumLines = foreach ($name in $checksumFiles) {
    $hash = (Get-FileHash -LiteralPath (Join-Path $destination $name) -Algorithm SHA256).Hash.ToLowerInvariant()
    "$hash  $name"
}
$checksumLines | Set-Content -LiteralPath (Join-Path $destination "SHA256SUMS") -Encoding ASCII

$cutoff = (Get-Date).ToUniversalTime().AddDays(-$RetentionDays)
Get-ChildItem -LiteralPath $backupRoot -Directory |
    Where-Object {
        $_.Name -match '^\d{8}T\d{6}Z$' -and
        $_.LastWriteTimeUtc -lt $cutoff -and
        $_.FullName -ne $destination
    } |
    ForEach-Object { Remove-Item -LiteralPath $_.FullName -Recurse -Force }

Write-Output "Backup completed: $destination"
Write-Output "Checksum manifest: $(Join-Path $destination 'SHA256SUMS')"
