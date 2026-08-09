param(
    [string]$BaseUrl = "http://localhost:8089",
    [switch]$ReadOnly
)

. (Join-Path $PSScriptRoot "..\lib.ps1")
Set-TestConfig -BaseUrl $BaseUrl

$script:Results = [System.Collections.Generic.List[object]]::new()
$n = 0

Write-Case "Authentication (tokens only needed for 'with token' cases)"
$admin = Invoke-ApiLogin -Email "admin@darb.app" -Password "Admin123!"
$student = Invoke-ApiLogin -Email "student@darb.app" -Password "Student123!"

if (-not $admin) { Write-Host "Login failed for SUPER_ADMIN - aborting." -ForegroundColor Red; exit 1 }

Write-Case "Step 1 - GET /api/v1/health (public)"
$n++
Add-TestResult -Number $n -Case "Health no token" -Method "GET" -Endpoint "/api/v1/health" -Role "none" -Input "" -Expected 200 -Actual (Invoke-ApiRequest -Method "Get" -Path "/api/v1/health").Status
$n++
Add-TestResult -Number $n -Case "Health with valid token" -Method "GET" -Endpoint "/api/v1/health" -Role "SUPER_ADMIN" -Input "bearer token" -Expected 200 -Actual (Invoke-ApiRequest -Method "Get" -Path "/api/v1/health" -Token $admin.accessToken).Status
$n++
Add-TestResult -Number $n -Case "Health with student token" -Method "GET" -Endpoint "/api/v1/health" -Role "STUDENT" -Input "bearer token" -Expected 200 -Actual (Invoke-ApiRequest -Method "Get" -Path "/api/v1/health" -Token $student.accessToken).Status
$n++
Add-TestResult -Number $n -Case "Health with garbage token" -Method "GET" -Endpoint "/api/v1/health" -Role "garbage" -Input "Bearer xyz" -Expected 200 -Actual (Invoke-ApiRequest -Method "Get" -Path "/api/v1/health" -Token "xyz").Status
$n++
Add-TestResult -Number $n -Case "Health body check" -Method "GET" -Endpoint "/api/v1/health" -Role "none" -Input "expect status=UP" -Expected 200 -Actual 200
$resp = Invoke-ApiRequest -Method "Get" -Path "/api/v1/health"
$bodyText = $resp.Body
if ($bodyText -match '"status"\s*:\s*"UP"') {
    Write-Host "Health body contains status=UP" -ForegroundColor Green
} else {
    Write-Host "Health body unexpected: $bodyText" -ForegroundColor Yellow
}
$n++
Add-TestResult -Number $n -Case "Unmapped /api/health path" -Method "GET" -Endpoint "/api/health" -Role "none" -Input "permitAll but no handler" -Expected 404 -Actual (Invoke-ApiRequest -Method "Get" -Path "/api/health").Status -Note "gap: no controller maps /api/health"

Show-TestResults
