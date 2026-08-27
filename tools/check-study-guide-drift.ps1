[CmdletBinding()]
param(
    [string]$ProjectRoot
)

$ErrorActionPreference = "Stop"

if ([string]::IsNullOrWhiteSpace($ProjectRoot)) {
    $ProjectRoot = Split-Path -Parent $PSScriptRoot
}

$failures = [System.Collections.Generic.List[string]]::new()
$checks = 0

function Read-Utf8([string]$Path) {
    if (-not (Test-Path -LiteralPath $Path)) {
        $script:failures.Add("Missing file: $Path")
        return ""
    }
    return Get-Content -LiteralPath $Path -Raw -Encoding UTF8
}

function Require-Contains([string]$Text, [string]$Expected, [string]$Label) {
    $script:checks++
    if (-not $Text.Contains($Expected)) {
        $script:failures.Add("$Label — missing: $Expected")
    }
}

function Forbid-Contains([string]$Text, [string]$Forbidden, [string]$Label) {
    $script:checks++
    if ($Text.Contains($Forbidden)) {
        $script:failures.Add("$Label — stale text still present: $Forbidden")
    }
}

function Normalize-NpmVersion([string]$Version) {
    return $Version.TrimStart('^', '~')
}

$guidePath = Join-Path $ProjectRoot "docs/study-and-demo-guide.md"
$pomPath = Join-Path $ProjectRoot "csa-official-backend/pom.xml"
$packagePath = Join-Path $ProjectRoot "csa-official-frontend/package.json"

$guide = Read-Utf8 $guidePath
$pomText = Read-Utf8 $pomPath
$packageText = Read-Utf8 $packagePath

if ($failures.Count -eq 0) {
    [xml]$pom = $pomText
    $ns = [System.Xml.XmlNamespaceManager]::new($pom.NameTable)
    $ns.AddNamespace("m", "http://maven.apache.org/POM/4.0.0")
    $package = $packageText | ConvertFrom-Json

    $versions = [ordered]@{
        "Spring Boot"          = $pom.SelectSingleNode("/m:project/m:parent/m:version", $ns).InnerText
        "Java"                 = $pom.SelectSingleNode("/m:project/m:properties/m:java.version", $ns).InnerText
        "MyBatis-Plus"         = $pom.SelectSingleNode("//m:dependency[m:artifactId='mybatis-plus-spring-boot3-starter']/m:version", $ns).InnerText
        "JJWT"                 = $pom.SelectSingleNode("//m:dependency[m:artifactId='jjwt-api']/m:version", $ns).InnerText
        "EasyExcel"            = $pom.SelectSingleNode("//m:dependency[m:artifactId='easyexcel']/m:version", $ns).InnerText
        "JGit"                 = $pom.SelectSingleNode("//m:dependency[m:artifactId='org.eclipse.jgit']/m:version", $ns).InnerText
        "Knife4j"              = $pom.SelectSingleNode("//m:dependency[m:artifactId='knife4j-openapi3-jakarta-spring-boot-starter']/m:version", $ns).InnerText
        "Next.js"              = Normalize-NpmVersion $package.dependencies.next
        "React"                = Normalize-NpmVersion $package.dependencies.react
        "axios"                = Normalize-NpmVersion $package.dependencies.axios
        "isomorphic-dompurify" = Normalize-NpmVersion $package.dependencies.'isomorphic-dompurify'
    }

    foreach ($entry in $versions.GetEnumerator()) {
        Require-Contains $guide "$($entry.Key) $($entry.Value)" "Version contract"
    }
}

$resourceController = Read-Utf8 (Join-Path $ProjectRoot "csa-official-backend/src/main/java/com/csa/official/modules/sys/controller/ResourceController.java")
$resourceService = Read-Utf8 (Join-Path $ProjectRoot "csa-official-backend/src/main/java/com/csa/official/modules/sys/service/ResourceService.java")
$pageUtils = Read-Utf8 (Join-Path $ProjectRoot "csa-official-backend/src/main/java/com/csa/official/common/util/PageUtils.java")
$axios = Read-Utf8 (Join-Path $ProjectRoot "csa-official-frontend/src/lib/axios.ts")
$resourceLibrary = Read-Utf8 (Join-Path $ProjectRoot "csa-official-frontend/src/components/business/resources/ResourceLibrary.tsx")
$globalHandler = Read-Utf8 (Join-Path $ProjectRoot "csa-official-backend/src/main/java/com/csa/official/common/exception/GlobalExceptionHandler.java")
$securityConfig = Read-Utf8 (Join-Path $ProjectRoot "csa-official-backend/src/main/java/com/csa/official/config/SecurityConfig.java")
$jwtFilter = Read-Utf8 (Join-Path $ProjectRoot "csa-official-backend/src/main/java/com/csa/official/config/JwtAuthenticationFilter.java")
$entryPoint = Read-Utf8 (Join-Path $ProjectRoot "csa-official-backend/src/main/java/com/csa/official/common/security/JwtAuthenticationEntryPoint.java")
$deniedHandler = Read-Utf8 (Join-Path $ProjectRoot "csa-official-backend/src/main/java/com/csa/official/common/security/JwtAccessDeniedHandler.java")

Require-Contains $resourceController "public R<Page<ResourceVO>> list(" "Resource response type"
Require-Contains $resourceController "resourceService.listResources(page, size, category)" "Controller-to-Service call"
Require-Contains $resourceController "@RequestBody @Valid SaveResourceDto dto" "Validation trigger"
Require-Contains $resourceController '@NotBlank(message = "' "Title validation rule"

Require-Contains $resourceService "PageUtils.of(page, size)" "Page normalization"
Require-Contains $resourceService "query.eq(Resource::getCategory, category.trim())" "Category filter"
Require-Contains $resourceService "query.orderByDesc(Resource::getCreateTime)" "Resource ordering"
Require-Contains $resourceService "resourceMapper.selectPage(pageParam, query)" "Mapper pagination call"
Require-Contains $resourceService "map(ResourceVO::from)" "Entity-to-VO conversion"

Require-Contains $pageUtils "DEFAULT_PAGE_SIZE = 10" "Default page size"
Require-Contains $pageUtils "MAX_PAGE_SIZE = 100" "Maximum page size"
Require-Contains $resourceLibrary "const pageSize = 8" "Frontend page size"
Require-Contains $resourceLibrary "setItems(response.records)" "React records state"
Require-Contains $resourceLibrary "setPages(Math.max(response.pages || 1, 1))" "React page state"
Require-Contains $resourceLibrary "setTotal(response.total || 0)" "React total state"
Require-Contains $axios "const payload = response.data as unknown" "Axios transport envelope extraction"
Require-Contains $axios "return payload.data" "Axios business payload extraction"

$exceptionContracts = [ordered]@{
    "BindException.class"                          = "VALIDATION_FAILED"
    "MissingServletRequestParameterException.class" = "MISSING_PARAMETER"
    "MethodArgumentTypeMismatchException.class"     = "TYPE_MISMATCH"
    "HttpMessageNotReadableException.class"          = "MALFORMED_REQUEST"
    "CsaException.class"                             = "handleCsaException"
}
foreach ($entry in $exceptionContracts.GetEnumerator()) {
    Require-Contains $globalHandler $entry.Key "Exception handler declaration"
    Require-Contains $globalHandler $entry.Value "Exception response contract"
}

Require-Contains $securityConfig ".authenticationEntryPoint(unauthorizedHandler)" "401 handler wiring"
Require-Contains $securityConfig ".accessDeniedHandler(accessDeniedHandler)" "403 handler wiring"
Require-Contains $jwtFilter "SecurityContextHolder.getContext().setAuthentication(authToken)" "SecurityContext authentication write"
Require-Contains $entryPoint "ApiErrorCode.AUTHENTICATION_REQUIRED" "401 error code"
Require-Contains $deniedHandler "ApiErrorCode.ACCESS_DENIED" "403 error code"

Require-Contains $guide "### 0.1" "Compression recovery section"
Require-Contains $guide "### 0.3" "Historical drift ledger"
Require-Contains $guide "D:\CSA-Project" "Source priority"
Require-Contains $guide "MethodArgumentNotValidException" "Validation exception terminology"
Require-Contains $guide "JwtAuthenticationEntryPoint" "401 terminology"
Require-Contains $guide "JwtAccessDeniedHandler" "403 terminology"

Forbid-Contains $guide "没有，直接调用了R.ok" "Pre-Service answer"
Forbid-Contains $guide "没有执行操作，在BaseMapper" "Pre-Mapper answer"
Forbid-Contains $guide "Axios将两层的data" "Imprecise Axios wording"

if ($failures.Count -gt 0) {
    Write-Host "Study-guide drift check failed ($($failures.Count) issue(s), $checks checks):" -ForegroundColor Red
    foreach ($failure in $failures) {
        Write-Host " - $failure" -ForegroundColor Red
    }
    exit 1
}

Write-Host "Study-guide drift check passed: $checks checks." -ForegroundColor Green
