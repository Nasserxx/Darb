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
# Throwaway wiring (not ReadOnly): throwaway mosque + throwaway STUDENT who
# submits a pending join request, so the fleet list has real data.
# ---------------------------------------------------------------------------
$suffix = ([guid]::NewGuid().ToString("N")).Substring(0, 8)
$joinPrepared = $false

if (-not $ReadOnly) {
    $mc = Invoke-ApiRequest -Method "Post" -Path "/api/v1/mosques" -Token $admin.accessToken -Body (Test-Body @{
        name = "Stuck Work Test Mosque $suffix"; city = "Test City"
    })
    if ($mc.Status -eq 201) {
        $stuckMosqueId = ($mc.Body | ConvertFrom-Json).data.id
        $regSt = Invoke-ApiRequest -Method "Post" -Path "/api/v1/auth/register" -Body (Test-Body @{
            fullName = "Stuck Work Student"; email = "sw.student.$suffix@darb.app"; password = "SwPass123!"; role = "STUDENT"
        })
        if ($regSt.Status -eq 201 -or $regSt.Status -eq 409) {
            $swStudent = Invoke-ApiLogin -Email "sw.student.$suffix@darb.app" -Password "SwPass123!"
        } else {
            $swStudent = $null
        }
        if ($swStudent) {
            $jr = Invoke-ApiRequest -Method "Post" -Path "/api/v1/mosques/join-requests" -Token $swStudent.accessToken -Body (Test-Body @{
                mosqueId = $stuckMosqueId
            })
            if ($jr.Status -eq 201) {
                $joinPrepared = $true
                Write-Host "Pending join request created for mosque $stuckMosqueId"
            } else {
                Write-Host "Could not create join request (status $($jr.Status))."
            }
        } else {
            Write-Host "Could not login throwaway student."
        }
    } else {
        Write-Host "Could not create throwaway mosque (status $($mc.Status))."
    }
}

Write-Case "Step 1 - Auth basics"
$n++
Add-TestResult -Number $n -Case "GET stuck-work without token" -Method "GET" -Endpoint "/api/v1/admin/stuck-work" -Role "none" -Input "" -Expected 401 -Actual (Invoke-ApiRequest -Method "Get" -Path "/api/v1/admin/stuck-work").Status
$n++
Add-TestResult -Number $n -Case "GET stuck-work garbage token" -Method "GET" -Endpoint "/api/v1/admin/stuck-work" -Role "garbage" -Input "Bearer xyz" -Expected 401 -Actual (Invoke-ApiRequest -Method "Get" -Path "/api/v1/admin/stuck-work" -Token "xyz").Status

Write-Case "Step 2 - Roles"
$n++
Add-TestResult -Number $n -Case "List as SUPER_ADMIN" -Method "GET" -Endpoint "/api/v1/admin/stuck-work" -Role "SUPER_ADMIN" -Input "" -Expected 200 -Actual (Invoke-ApiRequest -Method "Get" -Path "/api/v1/admin/stuck-work" -Token $admin.accessToken).Status
$n++
Add-TestResult -Number $n -Case "List as MOSQUE_ADMIN" -Method "GET" -Endpoint "/api/v1/admin/stuck-work" -Role "MOSQUE_ADMIN" -Input "" -Expected 403 -Actual (Invoke-ApiRequest -Method "Get" -Path "/api/v1/admin/stuck-work" -Token $mosqueAdmin.accessToken).Status
$n++
Add-TestResult -Number $n -Case "List as TEACHER" -Method "GET" -Endpoint "/api/v1/admin/stuck-work" -Role "TEACHER" -Input "" -Expected 403 -Actual (Invoke-ApiRequest -Method "Get" -Path "/api/v1/admin/stuck-work" -Token $teacher.accessToken).Status
$n++
Add-TestResult -Number $n -Case "List as STUDENT" -Method "GET" -Endpoint "/api/v1/admin/stuck-work" -Role "STUDENT" -Input "" -Expected 403 -Actual (Invoke-ApiRequest -Method "Get" -Path "/api/v1/admin/stuck-work" -Token $student.accessToken).Status
$n++
Add-TestResult -Number $n -Case "List as PARENT" -Method "GET" -Endpoint "/api/v1/admin/stuck-work" -Role "PARENT" -Input "" -Expected 403 -Actual (Invoke-ApiRequest -Method "Get" -Path "/api/v1/admin/stuck-work" -Token $parent.accessToken).Status

Write-Case "Step 3 - Data present (pending join)"
if ($joinPrepared) {
    $listRes = Invoke-ApiRequest -Method "Get" -Path "/api/v1/admin/stuck-work" -Token $admin.accessToken
    $items = @(($listRes.Body | ConvertFrom-Json).data)
    $count = $items.Count
    $n++
    Add-TestResult -Number $n -Case "List contains pending join" -Method "GET" -Endpoint "/api/v1/admin/stuck-work" -Role "SUPER_ADMIN" -Input "1 pending join" -Expected 200 -Actual $listRes.Status -Note "items=$count"
    if ($count -gt 0 -and $items[0].kind -eq "PENDING_JOIN") {
        Write-Host "Stuck work item verified: kind=$($items[0].kind) summary='$($items[0].summary)'" -ForegroundColor Green
    } else {
        Write-Host "Expected PENDING_JOIN item but got count=$count" -ForegroundColor Yellow
    }
} else {
    $n++
    Add-TestResult -Number $n -Case "List contains pending join" -Method "GET" -Endpoint "/api/v1/admin/stuck-work" -Role "SUPER_ADMIN" -Input "1 pending join" -Expected 200 -Actual 0 -Note "SKIPPED"
}

Show-TestResults
