$ErrorActionPreference = "Stop"

$email = [Environment]::GetEnvironmentVariable("UPSTASH_EMAIL", "User")
if ([string]::IsNullOrWhiteSpace($email)) {
    $email = [Environment]::GetEnvironmentVariable("UPSTASH_EMAIL", "Process")
}

$apiKey = [Environment]::GetEnvironmentVariable("UPSTASH_API_KEY", "User")
if ([string]::IsNullOrWhiteSpace($apiKey)) {
    $apiKey = [Environment]::GetEnvironmentVariable("UPSTASH_API_KEY", "Process")
}

if ([string]::IsNullOrWhiteSpace($email)) {
    throw "UPSTASH_EMAIL is not configured. Set it as a user or process environment variable."
}

if ([string]::IsNullOrWhiteSpace($apiKey)) {
    throw "UPSTASH_API_KEY is not configured. Set it as a user or process environment variable."
}

& npx.cmd -y @upstash/mcp-server@latest --email $email --api-key $apiKey
exit $LASTEXITCODE
