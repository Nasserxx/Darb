param(
    [string]$BaseUrl = "http://localhost:8089",
    [switch]$ReadOnly
)

. (Join-Path $PSScriptRoot "..\lib.ps1")
Set-TestConfig -BaseUrl $BaseUrl

$script:Results = [System.Collections.Generic.List[object]]::new()
$n = 0

function Get-FirstId {
    param([string]$Json)
    try {
        $obj = $Json | ConvertFrom-Json
        if ($obj.data -and $obj.data.content -and $obj.data.content.Count -gt 0) {
            return $obj.data.content[0].id
        }
        if ($obj.data -and $obj.data.id) { return $obj.data.id }
    } catch {}
    return $null
}

Write-Case "Authentication"
$admin = Invoke-ApiLogin -Email "admin@darb.app" -Password "Admin123!"
$mosqueAdmin = Invoke-ApiLogin -Email "mosque.admin@darb.app" -Password "MosqueAdmin1!"
$teacher = Invoke-ApiLogin -Email "teacher@darb.app" -Password "Teacher123!"
$student = Invoke-ApiLogin -Email "student@darb.app" -Password "Student123!"
$parent = Invoke-ApiLogin -Email "parent@darb.app" -Password "Parent123!"

if (-not $admin) { Write-Host "Login failed for SUPER_ADMIN - aborting." -ForegroundColor Red; exit 1 }

$n++
Add-TestResult -Number $n -Case "Login as SUPER_ADMIN" -Method "POST" -Endpoint "/api/v1/auth/login" -Role "SUPER_ADMIN" -Input "admin@darb.app" -Expected 200 -Actual 200

$circles = Invoke-ApiRequest -Method "Get" -Path "/api/v1/circles" -Token $admin.accessToken
$circleId = Get-FirstId $circles.Body
$receiverId = $admin.userId
Write-Host "Circle id (for create): $circleId"

$msgId = $null
if (-not $ReadOnly -and $circleId) {
    $created = Invoke-ApiRequest -Method "Post" -Path "/api/v1/messages" -Token $student.accessToken -Body (Test-Body @{
        receiverId = $receiverId
        circleId = $circleId
        content = "Throwaway message from automated test"
    })
    if ($created.Status -eq 201) {
        $msgId = Get-FirstId $created.Body
        Write-Host "Throwaway message id: $msgId"
    }
}

Write-Case "Step 1 - Auth basics"
$n++
Add-TestResult -Number $n -Case "GET /mine without token" -Method "GET" -Endpoint "/api/v1/messages/mine" -Role "none" -Input "" -Expected 401 -Actual (Invoke-ApiRequest -Method "Get" -Path "/api/v1/messages/mine").Status
$n++
Add-TestResult -Number $n -Case "GET /mine with garbage token" -Method "GET" -Endpoint "/api/v1/messages/mine" -Role "garbage" -Input "Bearer xyz" -Expected 401 -Actual (Invoke-ApiRequest -Method "Get" -Path "/api/v1/messages/mine" -Token "xyz").Status

Write-Case "Step 2 - GET /mine (all roles)"
foreach ($roleEntry in @(
    @{ Role = "SUPER_ADMIN"; T = $admin },
    @{ Role = "MOSQUE_ADMIN"; T = $mosqueAdmin },
    @{ Role = "TEACHER"; T = $teacher },
    @{ Role = "STUDENT"; T = $student },
    @{ Role = "PARENT"; T = $parent }
)) {
    $n++
    Add-TestResult -Number $n -Case "GET /mine" -Method "GET" -Endpoint "/api/v1/messages/mine" -Role $roleEntry.Role -Input "" -Expected 200 -Actual (Invoke-ApiRequest -Method "Get" -Path "/api/v1/messages/mine" -Token $roleEntry.T.accessToken).Status
}

Write-Case "Step 3 - GET /circle/{circleId}"
if ($circleId) {
    $n++
    Add-TestResult -Number $n -Case "Circle thread as SUPER_ADMIN" -Method "GET" -Endpoint "/api/v1/messages/circle/{circleId}" -Role "SUPER_ADMIN" -Input "circle=$circleId" -Expected 200 -Actual (Invoke-ApiRequest -Method "Get" -Path "/api/v1/messages/circle/$circleId" -Token $admin.accessToken).Status
    $n++
    Add-TestResult -Number $n -Case "Circle thread as non-participant STUDENT" -Method "GET" -Endpoint "/api/v1/messages/circle/{circleId}" -Role "STUDENT" -Input "no mosque" -Expected 403 -Actual (Invoke-ApiRequest -Method "Get" -Path "/api/v1/messages/circle/$circleId" -Token $student.accessToken).Status
    $n++
    Add-TestResult -Number $n -Case "Circle thread as non-participant PARENT" -Method "GET" -Endpoint "/api/v1/messages/circle/{circleId}" -Role "PARENT" -Input "no mosque" -Expected 403 -Actual (Invoke-ApiRequest -Method "Get" -Path "/api/v1/messages/circle/$circleId" -Token $parent.accessToken).Status
} else {
    $n++
    Add-TestResult -Number $n -Case "Circle thread as SUPER_ADMIN" -Method "GET" -Endpoint "/api/v1/messages/circle/{circleId}" -Role "SUPER_ADMIN" -Input "no circle" -Expected 200 -Actual 0 -Note "SKIPPED"
    $n++
    Add-TestResult -Number $n -Case "Circle thread as non-participant STUDENT" -Method "GET" -Endpoint "/api/v1/messages/circle/{circleId}" -Role "STUDENT" -Input "no circle" -Expected 403 -Actual 0 -Note "SKIPPED"
    $n++
    Add-TestResult -Number $n -Case "Circle thread as non-participant PARENT" -Method "GET" -Endpoint "/api/v1/messages/circle/{circleId}" -Role "PARENT" -Input "no circle" -Expected 403 -Actual 0 -Note "SKIPPED"
}
$n++
Add-TestResult -Number $n -Case "Circle thread nonexistent" -Method "GET" -Endpoint "/api/v1/messages/circle/{circleId}" -Role "SUPER_ADMIN" -Input "random uuid" -Expected 404 -Actual (Invoke-ApiRequest -Method "Get" -Path "/api/v1/messages/circle/$([guid]::NewGuid())" -Token $admin.accessToken).Status
$n++
Add-TestResult -Number $n -Case "Circle thread invalid UUID" -Method "GET" -Endpoint "/api/v1/messages/circle/{circleId}" -Role "SUPER_ADMIN" -Input "not-a-uuid" -Expected 400 -Actual (Invoke-ApiRequest -Method "Get" -Path "/api/v1/messages/circle/not-a-uuid" -Token $admin.accessToken).Status
$n++
Add-TestResult -Number $n -Case "Circle thread without token" -Method "GET" -Endpoint "/api/v1/messages/circle/{circleId}" -Role "none" -Input "random uuid" -Expected 401 -Actual (Invoke-ApiRequest -Method "Get" -Path "/api/v1/messages/circle/$([guid]::NewGuid())").Status

Write-Case "Step 4 - POST /messages (any authenticated)"
if ($circleId) {
    $n++
    Add-TestResult -Number $n -Case "Send as STUDENT" -Method "POST" -Endpoint "/api/v1/messages" -Role "STUDENT" -Input "receiver=admin" -Expected 201 -Actual (Invoke-ApiRequest -Method "Post" -Path "/api/v1/messages" -Token $student.accessToken -Body (Test-Body @{ receiverId = $receiverId; circleId = $circleId; content = "Test message from student" })).Status
    $n++
    Add-TestResult -Number $n -Case "Send as TEACHER" -Method "POST" -Endpoint "/api/v1/messages" -Role "TEACHER" -Input "receiver=admin" -Expected 201 -Actual (Invoke-ApiRequest -Method "Post" -Path "/api/v1/messages" -Token $teacher.accessToken -Body (Test-Body @{ receiverId = $receiverId; circleId = $circleId; content = "Test message from teacher" })).Status
    $n++
    Add-TestResult -Number $n -Case "Send as PARENT" -Method "POST" -Endpoint "/api/v1/messages" -Role "PARENT" -Input "receiver=admin" -Expected 201 -Actual (Invoke-ApiRequest -Method "Post" -Path "/api/v1/messages" -Token $parent.accessToken -Body (Test-Body @{ receiverId = $receiverId; circleId = $circleId; content = "Test message from parent" })).Status
} else {
    $n++
    Add-TestResult -Number $n -Case "Send as STUDENT" -Method "POST" -Endpoint "/api/v1/messages" -Role "STUDENT" -Input "no circle" -Expected 201 -Actual 0 -Note "SKIPPED"
    $n++
    Add-TestResult -Number $n -Case "Send as TEACHER" -Method "POST" -Endpoint "/api/v1/messages" -Role "TEACHER" -Input "no circle" -Expected 201 -Actual 0 -Note "SKIPPED"
    $n++
    Add-TestResult -Number $n -Case "Send as PARENT" -Method "POST" -Endpoint "/api/v1/messages" -Role "PARENT" -Input "no circle" -Expected 201 -Actual 0 -Note "SKIPPED"
}
$n++
Add-TestResult -Number $n -Case "Send empty body" -Method "POST" -Endpoint "/api/v1/messages" -Role "STUDENT" -Input "{}" -Expected 400 -Actual (Invoke-ApiRequest -Method "Post" -Path "/api/v1/messages" -Token $student.accessToken -Body "{}").Status
$n++
Add-TestResult -Number $n -Case "Send blank content" -Method "POST" -Endpoint "/api/v1/messages" -Role "STUDENT" -Input "content=" -Expected 400 -Actual (Invoke-ApiRequest -Method "Post" -Path "/api/v1/messages" -Token $student.accessToken -Body (Test-Body @{ receiverId = $receiverId; circleId = $circleId; content = "" })).Status
$n++
Add-TestResult -Number $n -Case "Send nonexistent circle" -Method "POST" -Endpoint "/api/v1/messages" -Role "STUDENT" -Input "random circle uuid" -Expected 404 -Actual (Invoke-ApiRequest -Method "Post" -Path "/api/v1/messages" -Token $student.accessToken -Body (Test-Body @{ receiverId = $receiverId; circleId = ([guid]::NewGuid().ToString()); content = "x" })).Status
$n++
Add-TestResult -Number $n -Case "Send nonexistent receiver" -Method "POST" -Endpoint "/api/v1/messages" -Role "STUDENT" -Input "random user uuid" -Expected 404 -Actual (Invoke-ApiRequest -Method "Post" -Path "/api/v1/messages" -Token $student.accessToken -Body (Test-Body @{ receiverId = ([guid]::NewGuid().ToString()); circleId = $(if ($circleId) { $circleId } else { ([guid]::NewGuid().ToString()) }); content = "x" })).Status

Write-Case "Step 5 - PUT /{id}/read (any authenticated)"
if ($msgId) {
    $n++
    Add-TestResult -Number $n -Case "Mark read as receiver (SUPER_ADMIN)" -Method "PUT" -Endpoint "/api/v1/messages/{id}/read" -Role "SUPER_ADMIN" -Input "msg=$msgId" -Expected 200 -Actual (Invoke-ApiRequest -Method "Put" -Path "/api/v1/messages/$msgId/read" -Token $admin.accessToken).Status
    $n++
    Add-TestResult -Number $n -Case "Mark read as sender (STUDENT)" -Method "PUT" -Endpoint "/api/v1/messages/{id}/read" -Role "STUDENT" -Input "msg=$msgId" -Expected 200 -Actual (Invoke-ApiRequest -Method "Put" -Path "/api/v1/messages/$msgId/read" -Token $student.accessToken).Status -Note "no ownership check - verify"
} else {
    $n++
    Add-TestResult -Number $n -Case "Mark read as receiver (SUPER_ADMIN)" -Method "PUT" -Endpoint "/api/v1/messages/{id}/read" -Role "SUPER_ADMIN" -Input "no msg" -Expected 200 -Actual 0 -Note "SKIPPED"
    $n++
    Add-TestResult -Number $n -Case "Mark read as sender (STUDENT)" -Method "PUT" -Endpoint "/api/v1/messages/{id}/read" -Role "STUDENT" -Input "no msg" -Expected 200 -Actual 0 -Note "SKIPPED"
}
$n++
Add-TestResult -Number $n -Case "Mark read nonexistent" -Method "PUT" -Endpoint "/api/v1/messages/{id}/read" -Role "STUDENT" -Input "random uuid" -Expected 404 -Actual (Invoke-ApiRequest -Method "Put" -Path "/api/v1/messages/$([guid]::NewGuid())/read" -Token $student.accessToken).Status
$n++
Add-TestResult -Number $n -Case "Mark read invalid UUID" -Method "PUT" -Endpoint "/api/v1/messages/{id}/read" -Role "STUDENT" -Input "not-a-uuid" -Expected 400 -Actual (Invoke-ApiRequest -Method "Put" -Path "/api/v1/messages/not-a-uuid/read" -Token $student.accessToken).Status
$n++
Add-TestResult -Number $n -Case "Mark read without token" -Method "PUT" -Endpoint "/api/v1/messages/{id}/read" -Role "none" -Input "random uuid" -Expected 401 -Actual (Invoke-ApiRequest -Method "Put" -Path "/api/v1/messages/$([guid]::NewGuid())/read").Status

Show-TestResults
