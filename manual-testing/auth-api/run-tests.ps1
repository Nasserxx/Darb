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

# Throwaway user for password-change tests (unique email per run keeps runs idempotent)
$suffix = ([guid]::NewGuid().ToString("N")).Substring(0, 8)
$throwEmail = "auth.test.$suffix@darb.app"
$throwPass = "Auth12345!"
$throwNewPass = "Auth67890!"
$throw = $null
$reg = $null

if (-not $ReadOnly) {
    $reg = Invoke-ApiRequest -Method "Post" -Path "/api/v1/auth/register" -Body (Test-Body @{
        fullName = "Throwaway Auth User"; email = $throwEmail; password = $throwPass; role = "STUDENT"
    })
    if ($reg.Status -eq 201 -or $reg.Status -eq 409) {
        $throw = Invoke-ApiLogin -Email $throwEmail -Password $throwPass
        if ($throw) {
            Write-Host "Throwaway user ready: $throwEmail"
        } else {
            Write-Host "Could not log in throwaway user - password change cases will be SKIPPED."
        }
    } else {
        Write-Host "Could not register throwaway user (status $($reg.Status)) - password change cases will be SKIPPED."
    }
}

Write-Case "Step 1 - POST /auth/register (public)"
$n++
Add-TestResult -Number $n -Case "Register missing fullName" -Method "POST" -Endpoint "/api/v1/auth/register" -Role "none" -Input "no fullName" -Expected 400 -Actual (Invoke-ApiRequest -Method "Post" -Path "/api/v1/auth/register" -Body (Test-Body @{ email = "nofn@example.com"; password = "password123" })).Status
$n++
Add-TestResult -Number $n -Case "Register short password" -Method "POST" -Endpoint "/api/v1/auth/register" -Role "none" -Input "password=abc" -Expected 400 -Actual (Invoke-ApiRequest -Method "Post" -Path "/api/v1/auth/register" -Body (Test-Body @{ fullName = "X User"; email = "shortpw@example.com"; password = "abc" })).Status
$n++
Add-TestResult -Number $n -Case "Register disallowed role SUPER_ADMIN" -Method "POST" -Endpoint "/api/v1/auth/register" -Role "none" -Input "role=SUPER_ADMIN" -Expected 400 -Actual (Invoke-ApiRequest -Method "Post" -Path "/api/v1/auth/register" -Body (Test-Body @{ fullName = "X User"; email = "role@example.com"; password = "password123"; role = "SUPER_ADMIN" })).Status
$n++
Add-TestResult -Number $n -Case "Register duplicate email" -Method "POST" -Endpoint "/api/v1/auth/register" -Role "none" -Input "admin@darb.app" -Expected 409 -Actual (Invoke-ApiRequest -Method "Post" -Path "/api/v1/auth/register" -Body (Test-Body @{ fullName = "X User"; email = "admin@darb.app"; password = "password123" })).Status
if (-not $ReadOnly) {
    $n++
    Add-TestResult -Number $n -Case "Register throwaway user" -Method "POST" -Endpoint "/api/v1/auth/register" -Role "none" -Input $throwEmail -Expected 201 -Actual $reg.Status
} else {
    $n++
    Add-TestResult -Number $n -Case "Register throwaway user" -Method "POST" -Endpoint "/api/v1/auth/register" -Role "none" -Input "throwaway" -Expected 201 -Actual 0 -Note "SKIPPED"
}

Write-Case "Step 2 - POST /auth/login (public)"
$n++
Add-TestResult -Number $n -Case "Login wrong password" -Method "POST" -Endpoint "/api/v1/auth/login" -Role "none" -Input "student@darb.app / wrong" -Expected 401 -Actual (Invoke-ApiRequest -Method "Post" -Path "/api/v1/auth/login" -Body (Test-Body @{ email = "student@darb.app"; password = "wrongpass" })).Status
$n++
Add-TestResult -Number $n -Case "Login empty body" -Method "POST" -Endpoint "/api/v1/auth/login" -Role "none" -Input "{}" -Expected 400 -Actual (Invoke-ApiRequest -Method "Post" -Path "/api/v1/auth/login" -Body (Test-Body @{})).Status
$n++
Add-TestResult -Number $n -Case "Login bad email format" -Method "POST" -Endpoint "/api/v1/auth/login" -Role "none" -Input "not-an-email" -Expected 400 -Actual (Invoke-ApiRequest -Method "Post" -Path "/api/v1/auth/login" -Body (Test-Body @{ email = "not-an-email"; password = "whatever" })).Status

Write-Case "Step 3 - POST /auth/refresh (public)"
$n++
Add-TestResult -Number $n -Case "Refresh with garbage token" -Method "POST" -Endpoint "/api/v1/auth/refresh" -Role "none" -Input "garbage" -Expected 401 -Actual (Invoke-ApiRequest -Method "Post" -Path "/api/v1/auth/refresh" -Body (Test-Body @{ refreshToken = "garbage.token.value" })).Status
$n++
Add-TestResult -Number $n -Case "Refresh with access token" -Method "POST" -Endpoint "/api/v1/auth/refresh" -Role "STUDENT" -Input "access token" -Expected 401 -Actual (Invoke-ApiRequest -Method "Post" -Path "/api/v1/auth/refresh" -Body (Test-Body @{ refreshToken = $student.accessToken })).Status
$n++
Add-TestResult -Number $n -Case "Refresh empty body" -Method "POST" -Endpoint "/api/v1/auth/refresh" -Role "none" -Input "{}" -Expected 400 -Actual (Invoke-ApiRequest -Method "Post" -Path "/api/v1/auth/refresh" -Body (Test-Body @{})).Status
if (-not $ReadOnly) {
    $n++
    Add-TestResult -Number $n -Case "Refresh valid token" -Method "POST" -Endpoint "/api/v1/auth/refresh" -Role "STUDENT" -Input "student refresh" -Expected 200 -Actual (Invoke-ApiRequest -Method "Post" -Path "/api/v1/auth/refresh" -Body (Test-Body @{ refreshToken = $student.refreshToken })).Status
} else {
    $n++
    Add-TestResult -Number $n -Case "Refresh valid token" -Method "POST" -Endpoint "/api/v1/auth/refresh" -Role "STUDENT" -Input "student refresh" -Expected 200 -Actual 0 -Note "SKIPPED"
}

Write-Case "Step 4 - POST /auth/change-password (authenticated)"
$n++
Add-TestResult -Number $n -Case "Change password no token" -Method "POST" -Endpoint "/api/v1/auth/change-password" -Role "none" -Input "no auth" -Expected 500 -Actual (Invoke-ApiRequest -Method "Post" -Path "/api/v1/auth/change-password" -Body (Test-Body @{ currentPassword = "x"; newPassword = "yyyyyyyy" })).Status -Note "gap: route is permitAll, anonymous reaches handler"
$n++
Add-TestResult -Number $n -Case "Change password wrong current" -Method "POST" -Endpoint "/api/v1/auth/change-password" -Role "STUDENT" -Input "wrong current" -Expected 400 -Actual (Invoke-ApiRequest -Method "Post" -Path "/api/v1/auth/change-password" -Token $student.accessToken -Body (Test-Body @{ currentPassword = "WrongPass1"; newPassword = "NewPass123!" })).Status
$n++
Add-TestResult -Number $n -Case "Change password short new" -Method "POST" -Endpoint "/api/v1/auth/change-password" -Role "STUDENT" -Input "newPassword=abc" -Expected 400 -Actual (Invoke-ApiRequest -Method "Post" -Path "/api/v1/auth/change-password" -Token $student.accessToken -Body (Test-Body @{ currentPassword = "Student123!"; newPassword = "abc" })).Status
$n++
Add-TestResult -Number $n -Case "Change password empty body" -Method "POST" -Endpoint "/api/v1/auth/change-password" -Role "STUDENT" -Input "{}" -Expected 400 -Actual (Invoke-ApiRequest -Method "Post" -Path "/api/v1/auth/change-password" -Token $student.accessToken -Body (Test-Body @{})).Status

if (-not $ReadOnly -and $throw) {
    $n++
    Add-TestResult -Number $n -Case "Change password success" -Method "POST" -Endpoint "/api/v1/auth/change-password" -Role "STUDENT" -Input "throwaway" -Expected 200 -Actual (Invoke-ApiRequest -Method "Post" -Path "/api/v1/auth/change-password" -Token $throw.accessToken -Body (Test-Body @{ currentPassword = $throwPass; newPassword = $throwNewPass })).Status
    $n++
    Add-TestResult -Number $n -Case "Old refresh token invalid after change" -Method "POST" -Endpoint "/api/v1/auth/refresh" -Role "STUDENT" -Input "old refresh" -Expected 401 -Actual (Invoke-ApiRequest -Method "Post" -Path "/api/v1/auth/refresh" -Body (Test-Body @{ refreshToken = $throw.refreshToken })).Status
    $n++
    Add-TestResult -Number $n -Case "Login with new password" -Method "POST" -Endpoint "/api/v1/auth/login" -Role "STUDENT" -Input "throwaway new pw" -Expected 200 -Actual (Invoke-ApiRequest -Method "Post" -Path "/api/v1/auth/login" -Body (Test-Body @{ email = $throwEmail; password = $throwNewPass })).Status
    $n++
    Add-TestResult -Number $n -Case "Login with old password" -Method "POST" -Endpoint "/api/v1/auth/login" -Role "STUDENT" -Input "throwaway old pw" -Expected 401 -Actual (Invoke-ApiRequest -Method "Post" -Path "/api/v1/auth/login" -Body (Test-Body @{ email = $throwEmail; password = $throwPass })).Status
} else {
    $n++
    Add-TestResult -Number $n -Case "Change password success" -Method "POST" -Endpoint "/api/v1/auth/change-password" -Role "STUDENT" -Input "throwaway" -Expected 200 -Actual 0 -Note "SKIPPED"
}

Write-Case "Step 5 - Logout (gap check)"
$n++
Add-TestResult -Number $n -Case "Logout endpoint not implemented" -Method "POST" -Endpoint "/api/v1/auth/logout" -Role "SUPER_ADMIN" -Input "with bearer token" -Expected 404 -Actual (Invoke-ApiRequest -Method "Post" -Path "/api/v1/auth/logout" -Token $admin.accessToken).Status -Note "gap: AuthService.logout exists but no controller mapping"

Show-TestResults
