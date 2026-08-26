$ErrorActionPreference = "Stop"

$email = Read-Host "Upstash account email"
if ([string]::IsNullOrWhiteSpace($email)) {
    throw "The email cannot be empty."
}

$secureKey = Read-Host "New Upstash API key (input hidden)" -AsSecureString
$ptr = [Runtime.InteropServices.Marshal]::SecureStringToBSTR($secureKey)
try {
    $plainKey = [Runtime.InteropServices.Marshal]::PtrToStringBSTR($ptr)
    if ([string]::IsNullOrWhiteSpace($plainKey)) {
        throw "The API key cannot be empty."
    }

    [Environment]::SetEnvironmentVariable("UPSTASH_EMAIL", $email, "User")
    [Environment]::SetEnvironmentVariable("UPSTASH_API_KEY", $plainKey, "User")
}
finally {
    if ($ptr -ne [IntPtr]::Zero) {
        [Runtime.InteropServices.Marshal]::ZeroFreeBSTR($ptr)
    }
}

Write-Output "Saved UPSTASH_EMAIL and UPSTASH_API_KEY to the current Windows user environment."
Write-Output "Restart Codex so the MCP process can read the new values."
