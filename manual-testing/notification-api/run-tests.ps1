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

$mosques = Invoke-ApiRequest -Method "Get" -Path "/api/v1/mosques" -Token $admin.accessToken
$mosqueId = Get-FirstId $mosques.Body
$recipientId = $student.userId
Write-Host "Mosque id (for create): $mosqueId"

$notifId = $null
if (-not $ReadOnly -and $mosqueId) {
    $created = Invoke-ApiRequest -Method "Post" -Path "/api/v1/notifications" -Token $admin.accessToken -Body (Test-Body @{
        mosqueId = $mosqueId
        recipientUserId = $recipientId
        title = "Throwaway Notification"
        body = "Automated test notification body"
        channel = "IN_APP"
        status = "PENDING"
    })
    if ($created.Status -eq 201) {
        $notifId = Get-FirstId $created.Body
        Write-Host "Throwaway notification id: $notifId"
    }
}

Write-Case "Step 1 - Auth basics"
$n++
Add-TestResult -Number $n -Case "GET /mine without token" -Method "GET" -Endpoint "/api/v1/notifications/mine" -Role "none" -Input "" -Expected 401 -Actual (Invoke-ApiRequest -Method "Get" -Path "/api/v1/notifications/mine").Status
$n++
Add-TestResult -Number $n -Case "GET /mine with garbage token" -Method "GET" -Endpoint "/api/v1/notifications/mine" -Role "garbage" -Input "Bearer xyz" -Expected 401 -Actual (Invoke-ApiRequest -Method "Get" -Path "/api/v1/notifications/mine" -Token "xyz").Status

Write-Case "Step 2 - GET /mine (all roles)"
foreach ($roleEntry in @(
    @{ Role = "SUPER_ADMIN"; T = $admin },
    @{ Role = "MOSQUE_ADMIN"; T = $mosqueAdmin },
    @{ Role = "TEACHER"; T = $teacher },
    @{ Role = "STUDENT"; T = $student },
    @{ Role = "PARENT"; T = $parent }
)) {
    $n++
    Add-TestResult -Number $n -Case "GET /mine" -Method "GET" -Endpoint "/api/v1/notifications/mine" -Role $roleEntry.Role -Input "" -Expected 200 -Actual (Invoke-ApiRequest -Method "Get" -Path "/api/v1/notifications/mine" -Token $roleEntry.T.accessToken).Status
}

Write-Case "Step 3 - POST /notifications (SUPER_ADMIN, MOSQUE_ADMIN)"
if ($mosqueId) {
    $n++
    Add-TestResult -Number $n -Case "Create as SUPER_ADMIN" -Method "POST" -Endpoint "/api/v1/notifications" -Role "SUPER_ADMIN" -Input "recipient=student" -Expected 201 -Actual (Invoke-ApiRequest -Method "Post" -Path "/api/v1/notifications" -Token $admin.accessToken -Body (Test-Body @{ mosqueId = $mosqueId; recipientUserId = $recipientId; title = "Test Notification A"; body = "Test body A"; channel = "IN_APP"; status = "PENDING" })).Status
    $n++
    Add-TestResult -Number $n -Case "Create as MOSQUE_ADMIN" -Method "POST" -Endpoint "/api/v1/notifications" -Role "MOSQUE_ADMIN" -Input "recipient=student" -Expected 201 -Actual (Invoke-ApiRequest -Method "Post" -Path "/api/v1/notifications" -Token $mosqueAdmin.accessToken -Body (Test-Body @{ mosqueId = $mosqueId; recipientUserId = $recipientId; title = "Test Notification B"; body = "Test body B"; channel = "EMAIL"; status = "SENT" })).Status
} else {
    $n++
    Add-TestResult -Number $n -Case "Create as SUPER_ADMIN" -Method "POST" -Endpoint "/api/v1/notifications" -Role "SUPER_ADMIN" -Input "no mosque" -Expected 201 -Actual 0 -Note "SKIPPED"
    $n++
    Add-TestResult -Number $n -Case "Create as MOSQUE_ADMIN" -Method "POST" -Endpoint "/api/v1/notifications" -Role "MOSQUE_ADMIN" -Input "no mosque" -Expected 201 -Actual 0 -Note "SKIPPED"
}
$n++
Add-TestResult -Number $n -Case "Create as TEACHER" -Method "POST" -Endpoint "/api/v1/notifications" -Role "TEACHER" -Input "valid body" -Expected 403 -Actual (Invoke-ApiRequest -Method "Post" -Path "/api/v1/notifications" -Token $teacher.accessToken -Body (Test-Body @{ mosqueId = $mosqueId; recipientUserId = $recipientId; title = "hax"; body = "hax"; channel = "IN_APP" })).Status
$n++
Add-TestResult -Number $n -Case "Create as STUDENT" -Method "POST" -Endpoint "/api/v1/notifications" -Role "STUDENT" -Input "valid body" -Expected 403 -Actual (Invoke-ApiRequest -Method "Post" -Path "/api/v1/notifications" -Token $student.accessToken -Body (Test-Body @{ mosqueId = $mosqueId; recipientUserId = $recipientId; title = "hax"; body = "hax"; channel = "IN_APP" })).Status
$n++
Add-TestResult -Number $n -Case "Create as PARENT" -Method "POST" -Endpoint "/api/v1/notifications" -Role "PARENT" -Input "valid body" -Expected 403 -Actual (Invoke-ApiRequest -Method "Post" -Path "/api/v1/notifications" -Token $parent.accessToken -Body (Test-Body @{ mosqueId = $mosqueId; recipientUserId = $recipientId; title = "hax"; body = "hax"; channel = "IN_APP" })).Status
$n++
Add-TestResult -Number $n -Case "Create empty body" -Method "POST" -Endpoint "/api/v1/notifications" -Role "SUPER_ADMIN" -Input "{}" -Expected 400 -Actual (Invoke-ApiRequest -Method "Post" -Path "/api/v1/notifications" -Token $admin.accessToken -Body "{}").Status
$n++
Add-TestResult -Number $n -Case "Create bad channel enum" -Method "POST" -Endpoint "/api/v1/notifications" -Role "SUPER_ADMIN" -Input "channel=WHATSAPP" -Expected 400 -Actual (Invoke-ApiRequest -Method "Post" -Path "/api/v1/notifications" -Token $admin.accessToken -Body (Test-Body @{ mosqueId = $mosqueId; recipientUserId = $recipientId; title = "x"; body = "y"; channel = "WHATSAPP" })).Status
$n++
Add-TestResult -Number $n -Case "Create title >255 chars" -Method "POST" -Endpoint "/api/v1/notifications" -Role "SUPER_ADMIN" -Input "title=300 chars" -Expected 400 -Actual (Invoke-ApiRequest -Method "Post" -Path "/api/v1/notifications" -Token $admin.accessToken -Body (Test-Body @{ mosqueId = $mosqueId; recipientUserId = $recipientId; title = ("x" * 256); body = "y"; channel = "IN_APP" })).Status
$n++
Add-TestResult -Number $n -Case "Create nonexistent mosque" -Method "POST" -Endpoint "/api/v1/notifications" -Role "SUPER_ADMIN" -Input "random mosque uuid" -Expected 404 -Actual (Invoke-ApiRequest -Method "Post" -Path "/api/v1/notifications" -Token $admin.accessToken -Body (Test-Body @{ mosqueId = ([guid]::NewGuid().ToString()); recipientUserId = $recipientId; title = "x"; body = "y"; channel = "IN_APP" })).Status
$n++
Add-TestResult -Number $n -Case "Create nonexistent recipient" -Method "POST" -Endpoint "/api/v1/notifications" -Role "SUPER_ADMIN" -Input "random user uuid" -Expected 404 -Actual (Invoke-ApiRequest -Method "Post" -Path "/api/v1/notifications" -Token $admin.accessToken -Body (Test-Body @{ mosqueId = $(if ($mosqueId) { $mosqueId } else { ([guid]::NewGuid().ToString()) }); recipientUserId = ([guid]::NewGuid().ToString()); title = "x"; body = "y"; channel = "IN_APP" })).Status

Write-Case "Step 4 - PUT /{id}/read (any authenticated)"
if ($notifId) {
    $n++
    Add-TestResult -Number $n -Case "Mark read as recipient (STUDENT)" -Method "PUT" -Endpoint "/api/v1/notifications/{id}/read" -Role "STUDENT" -Input "notif=$notifId" -Expected 200 -Actual (Invoke-ApiRequest -Method "Put" -Path "/api/v1/notifications/$notifId/read" -Token $student.accessToken).Status
    $n++
    Add-TestResult -Number $n -Case "Mark read cross-user (PARENT)" -Method "PUT" -Endpoint "/api/v1/notifications/{id}/read" -Role "PARENT" -Input "notif=$notifId" -Expected 200 -Actual (Invoke-ApiRequest -Method "Put" -Path "/api/v1/notifications/$notifId/read" -Token $parent.accessToken).Status -Note "no ownership check - verify"
} else {
    $n++
    Add-TestResult -Number $n -Case "Mark read as recipient (STUDENT)" -Method "PUT" -Endpoint "/api/v1/notifications/{id}/read" -Role "STUDENT" -Input "no notif" -Expected 200 -Actual 0 -Note "SKIPPED"
    $n++
    Add-TestResult -Number $n -Case "Mark read cross-user (PARENT)" -Method "PUT" -Endpoint "/api/v1/notifications/{id}/read" -Role "PARENT" -Input "no notif" -Expected 200 -Actual 0 -Note "SKIPPED"
}
$n++
Add-TestResult -Number $n -Case "Mark read nonexistent" -Method "PUT" -Endpoint "/api/v1/notifications/{id}/read" -Role "SUPER_ADMIN" -Input "random uuid" -Expected 404 -Actual (Invoke-ApiRequest -Method "Put" -Path "/api/v1/notifications/$([guid]::NewGuid())/read" -Token $admin.accessToken).Status
$n++
Add-TestResult -Number $n -Case "Mark read invalid UUID" -Method "PUT" -Endpoint "/api/v1/notifications/{id}/read" -Role "SUPER_ADMIN" -Input "not-a-uuid" -Expected 400 -Actual (Invoke-ApiRequest -Method "Put" -Path "/api/v1/notifications/not-a-uuid/read" -Token $admin.accessToken).Status
$n++
Add-TestResult -Number $n -Case "Mark read without token" -Method "PUT" -Endpoint "/api/v1/notifications/{id}/read" -Role "none" -Input "random uuid" -Expected 401 -Actual (Invoke-ApiRequest -Method "Put" -Path "/api/v1/notifications/$([guid]::NewGuid())/read").Status

Show-TestResults
