param(
    [string]$BaseUrl = "http://localhost:8089",
    [switch]$ReadOnly
)

. (Join-Path $PSScriptRoot "..\lib.ps1")
Set-TestConfig -BaseUrl $BaseUrl

$script:Results = [System.Collections.Generic.List[object]]::new()
$n = 0

Write-Case "Authentication"
$admin = Invoke-ApiLogin -Email "admin@darb.app" -Password "Admin123!"
$mosqueAdmin = Invoke-ApiLogin -Email "mosque.admin@darb.app" -Password "MosqueAdmin1!"
$teacher = Invoke-ApiLogin -Email "teacher@darb.app" -Password "Teacher123!"
$student = Invoke-ApiLogin -Email "student@darb.app" -Password "Student123!"
$parent = Invoke-ApiLogin -Email "parent@darb.app" -Password "Parent123!"

if (-not $admin) { Write-Host "Login failed for SUPER_ADMIN - aborting." -ForegroundColor Red; exit 1 }

$n++
Add-TestResult -Number $n -Case "Login as SUPER_ADMIN" -Method "POST" -Endpoint "/api/v1/auth/login" -Role "SUPER_ADMIN" -Input "admin@darb.app" -Expected 200 -Actual 200

# ---------------------------------------------------------------------------
# Throwaway resources (not ReadOnly): throwaway mosque + throwaway reports.
# reportId / reportMaId are needed for the 200/403 read cases below.
# ---------------------------------------------------------------------------
$suffix = ([guid]::NewGuid().ToString("N")).Substring(0, 8)
$mosqueId = $null
$reportId = $null
$reportMaId = $null

if (-not $ReadOnly) {
    $mc = Invoke-ApiRequest -Method "Post" -Path "/api/v1/mosques" -Token $admin.accessToken -Body (Test-Body @{
        name = "Report Test Mosque $suffix"; city = "Test City"
    })
    if ($mc.Status -eq 201) {
        $mosqueId = ($mc.Body | ConvertFrom-Json).data.id
        Write-Host "Throwaway mosque id: $mosqueId"
    } else {
        Write-Host "Could not create throwaway mosque (status $($mc.Status))."
    }
}

if (-not $ReadOnly -and $mosqueId) {
    $rc = Invoke-ApiRequest -Method "Post" -Path "/api/v1/reports" -Token $admin.accessToken -Body (Test-Body @{
        mosqueId = $mosqueId; type = "ATTENDANCE_SUMMARY"; title = "Report Test $suffix"
    })
    if ($rc.Status -eq 201) {
        $reportId = ($rc.Body | ConvertFrom-Json).data.id
        Write-Host "Throwaway report id: $reportId"
    } else {
        Write-Host "Could not create throwaway report (status $($rc.Status))."
    }
}

if (-not $ReadOnly -and $mosqueId) {
    $rm = Invoke-ApiRequest -Method "Post" -Path "/api/v1/reports" -Token $mosqueAdmin.accessToken -Body (Test-Body @{
        mosqueId = $mosqueId; type = "ENROLLMENT"; title = "Report By Mosque Admin $suffix"
    })
    if ($rm.Status -eq 201) {
        $reportMaId = ($rm.Body | ConvertFrom-Json).data.id
        Write-Host "Mosque-admin report id: $reportMaId"
    } else {
        Write-Host "Could not create report as MOSQUE_ADMIN (status $($rm.Status))."
    }
}

Write-Case "Step 1 - Auth basics"
$n++
Add-TestResult -Number $n -Case "GET /reports/{id} without token" -Method "GET" -Endpoint "/api/v1/reports/{id}" -Role "none" -Input "" -Expected 401 -Actual (Invoke-ApiRequest -Method "Get" -Path "/api/v1/reports/$([guid]::NewGuid())").Status
$n++
Add-TestResult -Number $n -Case "GET /reports/mosque/{id} without token" -Method "GET" -Endpoint "/api/v1/reports/mosque/{mosqueId}" -Role "none" -Input "" -Expected 401 -Actual (Invoke-ApiRequest -Method "Get" -Path "/api/v1/reports/mosque/$([guid]::NewGuid())").Status
$n++
Add-TestResult -Number $n -Case "POST /reports without token" -Method "POST" -Endpoint "/api/v1/reports" -Role "none" -Input "" -Expected 401 -Actual (Invoke-ApiRequest -Method "Post" -Path "/api/v1/reports" -Body (Test-Body @{})).Status

Write-Case "Step 2 - GET /reports/{id}"
if ($reportId) {
    $n++
    Add-TestResult -Number $n -Case "Get report as SUPER_ADMIN" -Method "GET" -Endpoint "/api/v1/reports/{id}" -Role "SUPER_ADMIN" -Input "reportId" -Expected 200 -Actual (Invoke-ApiRequest -Method "Get" -Path "/api/v1/reports/$reportId" -Token $admin.accessToken).Status
    $n++
    Add-TestResult -Number $n -Case "Get report as MOSQUE_ADMIN (no assignment)" -Method "GET" -Endpoint "/api/v1/reports/{id}" -Role "MOSQUE_ADMIN" -Input "reportId" -Expected 403 -Actual (Invoke-ApiRequest -Method "Get" -Path "/api/v1/reports/$reportId" -Token $mosqueAdmin.accessToken).Status -Note "tenant check blocks unassigned admin"
} else {
    $n++
    Add-TestResult -Number $n -Case "Get report as SUPER_ADMIN" -Method "GET" -Endpoint "/api/v1/reports/{id}" -Role "SUPER_ADMIN" -Input "reportId" -Expected 200 -Actual 0 -Note "SKIPPED"
    $n++
    Add-TestResult -Number $n -Case "Get report as MOSQUE_ADMIN (no assignment)" -Method "GET" -Endpoint "/api/v1/reports/{id}" -Role "MOSQUE_ADMIN" -Input "reportId" -Expected 403 -Actual 0 -Note "SKIPPED"
}
$n++
Add-TestResult -Number $n -Case "Get report as TEACHER" -Method "GET" -Endpoint "/api/v1/reports/{id}" -Role "TEACHER" -Input "random uuid" -Expected 403 -Actual (Invoke-ApiRequest -Method "Get" -Path "/api/v1/reports/$([guid]::NewGuid())" -Token $teacher.accessToken).Status
$n++
Add-TestResult -Number $n -Case "Get report as STUDENT" -Method "GET" -Endpoint "/api/v1/reports/{id}" -Role "STUDENT" -Input "random uuid" -Expected 403 -Actual (Invoke-ApiRequest -Method "Get" -Path "/api/v1/reports/$([guid]::NewGuid())" -Token $student.accessToken).Status
$n++
Add-TestResult -Number $n -Case "Get report as PARENT" -Method "GET" -Endpoint "/api/v1/reports/{id}" -Role "PARENT" -Input "random uuid" -Expected 403 -Actual (Invoke-ApiRequest -Method "Get" -Path "/api/v1/reports/$([guid]::NewGuid())" -Token $parent.accessToken).Status
$n++
Add-TestResult -Number $n -Case "Get report invalid UUID" -Method "GET" -Endpoint "/api/v1/reports/{id}" -Role "SUPER_ADMIN" -Input "not-a-uuid" -Expected 400 -Actual (Invoke-ApiRequest -Method "Get" -Path "/api/v1/reports/not-a-uuid" -Token $admin.accessToken).Status
$n++
Add-TestResult -Number $n -Case "Get report random UUID" -Method "GET" -Endpoint "/api/v1/reports/{id}" -Role "SUPER_ADMIN" -Input "random uuid" -Expected 404 -Actual (Invoke-ApiRequest -Method "Get" -Path "/api/v1/reports/$([guid]::NewGuid())" -Token $admin.accessToken).Status

Write-Case "Step 3 - GET /reports/mosque/{mosqueId}"
if ($mosqueId) {
    $n++
    Add-TestResult -Number $n -Case "List by mosque as SUPER_ADMIN" -Method "GET" -Endpoint "/api/v1/reports/mosque/{mosqueId}" -Role "SUPER_ADMIN" -Input "mosqueId" -Expected 200 -Actual (Invoke-ApiRequest -Method "Get" -Path "/api/v1/reports/mosque/$mosqueId" -Token $admin.accessToken).Status
    $n++
    Add-TestResult -Number $n -Case "List by mosque paginated" -Method "GET" -Endpoint "/api/v1/reports/mosque/{mosqueId}?size=1" -Role "SUPER_ADMIN" -Input "size=1" -Expected 200 -Actual (Invoke-ApiRequest -Method "Get" -Path "/api/v1/reports/mosque/$mosqueId" -Token $admin.accessToken -Query @{ size = 1 }).Status
    $n++
    Add-TestResult -Number $n -Case "List by mosque as MOSQUE_ADMIN" -Method "GET" -Endpoint "/api/v1/reports/mosque/{mosqueId}" -Role "MOSQUE_ADMIN" -Input "mosqueId" -Expected 200 -Actual (Invoke-ApiRequest -Method "Get" -Path "/api/v1/reports/mosque/$mosqueId" -Token $mosqueAdmin.accessToken).Status -Note "gap: no tenant check on this endpoint"
} else {
    $n++
    Add-TestResult -Number $n -Case "List by mosque as SUPER_ADMIN" -Method "GET" -Endpoint "/api/v1/reports/mosque/{mosqueId}" -Role "SUPER_ADMIN" -Input "mosqueId" -Expected 200 -Actual 0 -Note "SKIPPED"
    $n++
    Add-TestResult -Number $n -Case "List by mosque paginated" -Method "GET" -Endpoint "/api/v1/reports/mosque/{mosqueId}?size=1" -Role "SUPER_ADMIN" -Input "size=1" -Expected 200 -Actual 0 -Note "SKIPPED"
    $n++
    Add-TestResult -Number $n -Case "List by mosque as MOSQUE_ADMIN" -Method "GET" -Endpoint "/api/v1/reports/mosque/{mosqueId}" -Role "MOSQUE_ADMIN" -Input "mosqueId" -Expected 200 -Actual 0 -Note "SKIPPED"
}
$n++
Add-TestResult -Number $n -Case "List by mosque as TEACHER" -Method "GET" -Endpoint "/api/v1/reports/mosque/{mosqueId}" -Role "TEACHER" -Input "random uuid" -Expected 403 -Actual (Invoke-ApiRequest -Method "Get" -Path "/api/v1/reports/mosque/$([guid]::NewGuid())" -Token $teacher.accessToken).Status
$n++
Add-TestResult -Number $n -Case "List by mosque as STUDENT" -Method "GET" -Endpoint "/api/v1/reports/mosque/{mosqueId}" -Role "STUDENT" -Input "random uuid" -Expected 403 -Actual (Invoke-ApiRequest -Method "Get" -Path "/api/v1/reports/mosque/$([guid]::NewGuid())" -Token $student.accessToken).Status
$n++
Add-TestResult -Number $n -Case "List by mosque as PARENT" -Method "GET" -Endpoint "/api/v1/reports/mosque/{mosqueId}" -Role "PARENT" -Input "random uuid" -Expected 403 -Actual (Invoke-ApiRequest -Method "Get" -Path "/api/v1/reports/mosque/$([guid]::NewGuid())" -Token $parent.accessToken).Status
$n++
Add-TestResult -Number $n -Case "List by mosque invalid UUID" -Method "GET" -Endpoint "/api/v1/reports/mosque/{mosqueId}" -Role "SUPER_ADMIN" -Input "not-a-uuid" -Expected 400 -Actual (Invoke-ApiRequest -Method "Get" -Path "/api/v1/reports/mosque/not-a-uuid" -Token $admin.accessToken).Status
$n++
Add-TestResult -Number $n -Case "List by mosque random UUID (no mosque check)" -Method "GET" -Endpoint "/api/v1/reports/mosque/{mosqueId}" -Role "SUPER_ADMIN" -Input "random uuid" -Expected 200 -Actual (Invoke-ApiRequest -Method "Get" -Path "/api/v1/reports/mosque/$([guid]::NewGuid())" -Token $admin.accessToken).Status -Note "gap: empty page, 404 documented but not enforced"

Write-Case "Step 4 - POST /reports"
if (-not $ReadOnly -and $mosqueId) {
    $n++
    Add-TestResult -Number $n -Case "Create report as SUPER_ADMIN" -Method "POST" -Endpoint "/api/v1/reports" -Role "SUPER_ADMIN" -Input "valid body" -Expected 201 -Actual (Invoke-ApiRequest -Method "Post" -Path "/api/v1/reports" -Token $admin.accessToken -Body (Test-Body @{ mosqueId = $mosqueId; type = "FINANCIAL"; title = "Finance $suffix" })).Status
    $n++
    Add-TestResult -Number $n -Case "Create report as MOSQUE_ADMIN (no assignment)" -Method "POST" -Endpoint "/api/v1/reports" -Role "MOSQUE_ADMIN" -Input "any mosqueId" -Expected 201 -Actual (Invoke-ApiRequest -Method "Post" -Path "/api/v1/reports" -Token $mosqueAdmin.accessToken -Body (Test-Body @{ mosqueId = $mosqueId; type = "STUDENT_PROGRESS"; title = "Progress $suffix" })).Status -Note "gap: no tenant check on create"
} else {
    $n++
    Add-TestResult -Number $n -Case "Create report as SUPER_ADMIN" -Method "POST" -Endpoint "/api/v1/reports" -Role "SUPER_ADMIN" -Input "valid body" -Expected 201 -Actual 0 -Note "SKIPPED"
    $n++
    Add-TestResult -Number $n -Case "Create report as MOSQUE_ADMIN (no assignment)" -Method "POST" -Endpoint "/api/v1/reports" -Role "MOSQUE_ADMIN" -Input "any mosqueId" -Expected 201 -Actual 0 -Note "SKIPPED"
}
$n++
Add-TestResult -Number $n -Case "Create report as TEACHER" -Method "POST" -Endpoint "/api/v1/reports" -Role "TEACHER" -Input "valid body" -Expected 403 -Actual (Invoke-ApiRequest -Method "Post" -Path "/api/v1/reports" -Token $teacher.accessToken -Body (Test-Body @{ mosqueId = $([guid]::NewGuid()); type = "ENROLLMENT"; title = "x" })).Status
$n++
Add-TestResult -Number $n -Case "Create report as STUDENT" -Method "POST" -Endpoint "/api/v1/reports" -Role "STUDENT" -Input "valid body" -Expected 403 -Actual (Invoke-ApiRequest -Method "Post" -Path "/api/v1/reports" -Token $student.accessToken -Body (Test-Body @{ mosqueId = $([guid]::NewGuid()); type = "ENROLLMENT"; title = "x" })).Status
$n++
Add-TestResult -Number $n -Case "Create report as PARENT" -Method "POST" -Endpoint "/api/v1/reports" -Role "PARENT" -Input "valid body" -Expected 403 -Actual (Invoke-ApiRequest -Method "Post" -Path "/api/v1/reports" -Token $parent.accessToken -Body (Test-Body @{ mosqueId = $([guid]::NewGuid()); type = "ENROLLMENT"; title = "x" })).Status
$n++
Add-TestResult -Number $n -Case "Create report missing title" -Method "POST" -Endpoint "/api/v1/reports" -Role "SUPER_ADMIN" -Input "no title" -Expected 400 -Actual (Invoke-ApiRequest -Method "Post" -Path "/api/v1/reports" -Token $admin.accessToken -Body (Test-Body @{ mosqueId = $([guid]::NewGuid()); type = "ENROLLMENT" })).Status
$n++
Add-TestResult -Number $n -Case "Create report bad type enum" -Method "POST" -Endpoint "/api/v1/reports" -Role "SUPER_ADMIN" -Input "type=BOGUS" -Expected 400 -Actual (Invoke-ApiRequest -Method "Post" -Path "/api/v1/reports" -Token $admin.accessToken -Body (Test-Body @{ mosqueId = $([guid]::NewGuid()); type = "BOGUS"; title = "x" })).Status
$n++
Add-TestResult -Number $n -Case "Create report nonexistent mosque" -Method "POST" -Endpoint "/api/v1/reports" -Role "SUPER_ADMIN" -Input "random mosqueId" -Expected 404 -Actual (Invoke-ApiRequest -Method "Post" -Path "/api/v1/reports" -Token $admin.accessToken -Body (Test-Body @{ mosqueId = $([guid]::NewGuid()); type = "ENROLLMENT"; title = "x" })).Status

Show-TestResults
