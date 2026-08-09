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

function Ensure-User {
    param([string]$FullName, [string]$Email, [string]$Password, [string]$Role)
    $res = Invoke-ApiRequest -Method "Post" -Path "/api/v1/auth/register" -Body (Test-Body @{
        fullName = $FullName; email = $Email; password = $Password; role = $Role
    })
    if ($res.Status -eq 201 -or $res.Status -eq 409) {
        return Invoke-ApiLogin -Email $Email -Password $Password
    }
    return $null
}

$runSuffix = [DateTime]::Now.ToString("yyyyMMddHHmmss")
$mosqueId = $null
$adminInviteCode = $null
$twAdmin = $null
$twAdmin2 = $null
$twAdmin3 = $null
$twTeacher = $null
$twStudent = $null

if (-not $ReadOnly) {
    $twAdmin = Ensure-User "MA Throwaway Admin" "ma.admin$runSuffix@darb.app" "MaAdmin123!" "MOSQUE_ADMIN"
    $twAdmin2 = Ensure-User "MA Throwaway Admin2" "ma.admin2$runSuffix@darb.app" "MaAdmin123!" "MOSQUE_ADMIN"
    $twAdmin3 = Ensure-User "MA Throwaway Admin3" "ma.admin3$runSuffix@darb.app" "MaAdmin123!" "MOSQUE_ADMIN"
    $twTeacher = Ensure-User "MA Throwaway Teacher" "ma.teacher$runSuffix@darb.app" "MaTeacher123!" "TEACHER"
    $twStudent = Ensure-User "MA Throwaway Student" "ma.student$runSuffix@darb.app" "MaStudent123!" "STUDENT"

    if ($twAdmin) {
        $onboard = Invoke-ApiRequest -Method "Post" -Path "/api/v1/mosques/onboard" -Token $twAdmin.accessToken -Body (Test-Body @{
            name = "Throwaway Mosque $runSuffix"; city = "Riyadh"; timezone = "Asia/Riyadh"
        })
        $n++
        Add-TestResult -Number $n -Case "Onboard mosque (throwaway admin)" -Method "POST" -Endpoint "/api/v1/mosques/onboard" -Role "MOSQUE_ADMIN" -Input "name+city" -Expected 201 -Actual $onboard.Status
        if ($onboard.Status -eq 201) {
            $od = ($onboard.Body | ConvertFrom-Json).data
            $mosqueId = $od.mosque.id
            $adminInviteCode = $od.inviteCode
            Write-Host "Throwaway mosque: $mosqueId"
        } else {
            Write-Host "Onboard failed (status $($onboard.Status)) - dependent cases will be SKIPPED."
        }
    }
} else {
    $list = Invoke-ApiRequest -Method "Get" -Path "/api/v1/mosques" -Token $admin.accessToken
    if ($list.Status -eq 200) {
        $content = @(($list.Body | ConvertFrom-Json).data.content)
        foreach ($m in $content) {
            if ($m.isActive -eq $true) { $mosqueId = $m.id; break }
        }
        if (-not $mosqueId -and $content.Count -gt 0) { $mosqueId = $content[0].id }
        Write-Host "ReadOnly mode: using mosque $mosqueId"
    }
}

Write-Case "Step 1 - Auth basics"
$n++
Add-TestResult -Number $n -Case "GET /mosque-admins without token" -Method "GET" -Endpoint "/api/v1/mosque-admins" -Role "none" -Input "" -Expected 401 -Actual (Invoke-ApiRequest -Method "Get" -Path "/api/v1/mosque-admins").Status
$n++
Add-TestResult -Number $n -Case "GET /mosque-admins with garbage token" -Method "GET" -Endpoint "/api/v1/mosque-admins" -Role "garbage" -Input "Bearer xyz" -Expected 401 -Actual (Invoke-ApiRequest -Method "Get" -Path "/api/v1/mosque-admins" -Token "xyz").Status

Write-Case "Step 2 - GET /mosque-admins/join-requests (MOSQUE_ADMIN)"
if ($mosqueId -and $twAdmin) {
    $n++
    Add-TestResult -Number $n -Case "List join requests (assigned admin)" -Method "GET" -Endpoint "/api/v1/mosque-admins/join-requests" -Role "MOSQUE_ADMIN" -Input "tw admin" -Expected 200 -Actual (Invoke-ApiRequest -Method "Get" -Path "/api/v1/mosque-admins/join-requests" -Token $twAdmin.accessToken).Status
} else {
    $n++
    Add-TestResult -Number $n -Case "List join requests (assigned admin)" -Method "GET" -Endpoint "/api/v1/mosque-admins/join-requests" -Role "MOSQUE_ADMIN" -Input "no mosque" -Expected 200 -Actual 0 -Note "SKIPPED"
}
$n++
Add-TestResult -Number $n -Case "List join requests no assignment" -Method "GET" -Endpoint "/api/v1/mosque-admins/join-requests" -Role "MOSQUE_ADMIN" -Input "seed admin" -Expected 403 -Actual (Invoke-ApiRequest -Method "Get" -Path "/api/v1/mosque-admins/join-requests" -Token $mosqueAdmin.accessToken).Status
$n++
Add-TestResult -Number $n -Case "List join requests as TEACHER" -Method "GET" -Endpoint "/api/v1/mosque-admins/join-requests" -Role "TEACHER" -Input "" -Expected 403 -Actual (Invoke-ApiRequest -Method "Get" -Path "/api/v1/mosque-admins/join-requests" -Token $teacher.accessToken).Status
$n++
Add-TestResult -Number $n -Case "List join requests without token" -Method "GET" -Endpoint "/api/v1/mosque-admins/join-requests" -Role "none" -Input "" -Expected 401 -Actual (Invoke-ApiRequest -Method "Get" -Path "/api/v1/mosque-admins/join-requests").Status

Write-Case "Step 3 - Approve join request flow"
$approveId = $null
if (-not $ReadOnly -and $mosqueId -and $twTeacher -and $twAdmin) {
    $jr = Invoke-ApiRequest -Method "Post" -Path "/api/v1/mosques/join-requests" -Token $twTeacher.accessToken -Body (Test-Body @{ mosqueId = $mosqueId })
    $n++
    Add-TestResult -Number $n -Case "Create pending teacher join request" -Method "POST" -Endpoint "/api/v1/mosques/join-requests" -Role "TEACHER" -Input "mosque=$mosqueId" -Expected 201 -Actual $jr.Status
    if ($jr.Status -eq 201) {
        $list = Invoke-ApiRequest -Method "Get" -Path "/api/v1/mosque-admins/join-requests" -Token $twAdmin.accessToken
        if ($list.Status -eq 200) {
            $content = @(($list.Body | ConvertFrom-Json).data)
            if ($content.Count -gt 0) { $approveId = $content[0].id }
            Write-Host "Join request id: $approveId"
        }
    }
} else {
    $n++
    Add-TestResult -Number $n -Case "Create pending teacher join request" -Method "POST" -Endpoint "/api/v1/mosques/join-requests" -Role "TEACHER" -Input "no mosque" -Expected 201 -Actual 0 -Note "SKIPPED"
}
if ($approveId) {
    $n++
    Add-TestResult -Number $n -Case "Approve pending request" -Method "POST" -Endpoint "/api/v1/mosque-admins/join-requests/{id}/approve" -Role "MOSQUE_ADMIN" -Input "req=$approveId" -Expected 200 -Actual (Invoke-ApiRequest -Method "Post" -Path "/api/v1/mosque-admins/join-requests/$approveId/approve" -Token $twAdmin.accessToken).Status
    $n++
    Add-TestResult -Number $n -Case "Approve again (not pending)" -Method "POST" -Endpoint "/api/v1/mosque-admins/join-requests/{id}/approve" -Role "MOSQUE_ADMIN" -Input "req=$approveId" -Expected 400 -Actual (Invoke-ApiRequest -Method "Post" -Path "/api/v1/mosque-admins/join-requests/$approveId/approve" -Token $twAdmin.accessToken).Status
} else {
    $n++
    Add-TestResult -Number $n -Case "Approve pending request" -Method "POST" -Endpoint "/api/v1/mosque-admins/join-requests/{id}/approve" -Role "MOSQUE_ADMIN" -Input "no req" -Expected 200 -Actual 0 -Note "SKIPPED"
    $n++
    Add-TestResult -Number $n -Case "Approve again (not pending)" -Method "POST" -Endpoint "/api/v1/mosque-admins/join-requests/{id}/approve" -Role "MOSQUE_ADMIN" -Input "no req" -Expected 400 -Actual 0 -Note "SKIPPED"
}
if ($mosqueId -and $twAdmin) {
    $n++
    Add-TestResult -Number $n -Case "Approve random UUID" -Method "POST" -Endpoint "/api/v1/mosque-admins/join-requests/{id}/approve" -Role "MOSQUE_ADMIN" -Input "random uuid" -Expected 404 -Actual (Invoke-ApiRequest -Method "Post" -Path "/api/v1/mosque-admins/join-requests/$([guid]::NewGuid())/approve" -Token $twAdmin.accessToken).Status
} else {
    $n++
    Add-TestResult -Number $n -Case "Approve random UUID" -Method "POST" -Endpoint "/api/v1/mosque-admins/join-requests/{id}/approve" -Role "MOSQUE_ADMIN" -Input "no mosque" -Expected 404 -Actual 0 -Note "SKIPPED"
}
$n++
Add-TestResult -Number $n -Case "Approve invalid UUID" -Method "POST" -Endpoint "/api/v1/mosque-admins/join-requests/{id}/approve" -Role "MOSQUE_ADMIN" -Input "not-a-uuid" -Expected 400 -Actual (Invoke-ApiRequest -Method "Post" -Path "/api/v1/mosque-admins/join-requests/not-a-uuid/approve" -Token $twAdmin.accessToken).Status
$n++
Add-TestResult -Number $n -Case "Approve as seed admin (no assignment)" -Method "POST" -Endpoint "/api/v1/mosque-admins/join-requests/{id}/approve" -Role "MOSQUE_ADMIN" -Input "seed admin" -Expected 403 -Actual (Invoke-ApiRequest -Method "Post" -Path "/api/v1/mosque-admins/join-requests/$([guid]::NewGuid())/approve" -Token $mosqueAdmin.accessToken).Status
$n++
Add-TestResult -Number $n -Case "Approve as TEACHER" -Method "POST" -Endpoint "/api/v1/mosque-admins/join-requests/{id}/approve" -Role "TEACHER" -Input "" -Expected 403 -Actual (Invoke-ApiRequest -Method "Post" -Path "/api/v1/mosque-admins/join-requests/$([guid]::NewGuid())/approve" -Token $teacher.accessToken).Status

Write-Case "Step 4 - Reject join request flow"
$rejectId = $null
if (-not $ReadOnly -and $mosqueId -and $twStudent -and $twAdmin) {
    $jr = Invoke-ApiRequest -Method "Post" -Path "/api/v1/mosques/join-requests" -Token $twStudent.accessToken -Body (Test-Body @{ mosqueId = $mosqueId })
    $n++
    Add-TestResult -Number $n -Case "Create pending student join request" -Method "POST" -Endpoint "/api/v1/mosques/join-requests" -Role "STUDENT" -Input "mosque=$mosqueId" -Expected 201 -Actual $jr.Status
    if ($jr.Status -eq 201) {
        $list = Invoke-ApiRequest -Method "Get" -Path "/api/v1/mosque-admins/join-requests" -Token $twAdmin.accessToken
        if ($list.Status -eq 200) {
            $content = @(($list.Body | ConvertFrom-Json).data)
            if ($content.Count -gt 0) { $rejectId = $content[0].id }
            Write-Host "Reject-target join request id: $rejectId"
        }
    }
} else {
    $n++
    Add-TestResult -Number $n -Case "Create pending student join request" -Method "POST" -Endpoint "/api/v1/mosques/join-requests" -Role "STUDENT" -Input "no mosque" -Expected 201 -Actual 0 -Note "SKIPPED"
}
if ($rejectId) {
    $n++
    Add-TestResult -Number $n -Case "Reject pending request" -Method "POST" -Endpoint "/api/v1/mosque-admins/join-requests/{id}/reject" -Role "MOSQUE_ADMIN" -Input "req=$rejectId" -Expected 200 -Actual (Invoke-ApiRequest -Method "Post" -Path "/api/v1/mosque-admins/join-requests/$rejectId/reject" -Token $twAdmin.accessToken).Status
    $n++
    Add-TestResult -Number $n -Case "Reject again (not pending)" -Method "POST" -Endpoint "/api/v1/mosque-admins/join-requests/{id}/reject" -Role "MOSQUE_ADMIN" -Input "req=$rejectId" -Expected 400 -Actual (Invoke-ApiRequest -Method "Post" -Path "/api/v1/mosque-admins/join-requests/$rejectId/reject" -Token $twAdmin.accessToken).Status
} else {
    $n++
    Add-TestResult -Number $n -Case "Reject pending request" -Method "POST" -Endpoint "/api/v1/mosque-admins/join-requests/{id}/reject" -Role "MOSQUE_ADMIN" -Input "no req" -Expected 200 -Actual 0 -Note "SKIPPED"
    $n++
    Add-TestResult -Number $n -Case "Reject again (not pending)" -Method "POST" -Endpoint "/api/v1/mosque-admins/join-requests/{id}/reject" -Role "MOSQUE_ADMIN" -Input "no req" -Expected 400 -Actual 0 -Note "SKIPPED"
}
if ($mosqueId -and $twAdmin) {
    $n++
    Add-TestResult -Number $n -Case "Reject random UUID" -Method "POST" -Endpoint "/api/v1/mosque-admins/join-requests/{id}/reject" -Role "MOSQUE_ADMIN" -Input "random uuid" -Expected 404 -Actual (Invoke-ApiRequest -Method "Post" -Path "/api/v1/mosque-admins/join-requests/$([guid]::NewGuid())/reject" -Token $twAdmin.accessToken).Status
} else {
    $n++
    Add-TestResult -Number $n -Case "Reject random UUID" -Method "POST" -Endpoint "/api/v1/mosque-admins/join-requests/{id}/reject" -Role "MOSQUE_ADMIN" -Input "no mosque" -Expected 404 -Actual 0 -Note "SKIPPED"
}
$n++
Add-TestResult -Number $n -Case "Reject as seed admin (no assignment)" -Method "POST" -Endpoint "/api/v1/mosque-admins/join-requests/{id}/reject" -Role "MOSQUE_ADMIN" -Input "seed admin" -Expected 403 -Actual (Invoke-ApiRequest -Method "Post" -Path "/api/v1/mosque-admins/join-requests/$([guid]::NewGuid())/reject" -Token $mosqueAdmin.accessToken).Status
$n++
Add-TestResult -Number $n -Case "Reject as TEACHER" -Method "POST" -Endpoint "/api/v1/mosque-admins/join-requests/{id}/reject" -Role "TEACHER" -Input "" -Expected 403 -Actual (Invoke-ApiRequest -Method "Post" -Path "/api/v1/mosque-admins/join-requests/$([guid]::NewGuid())/reject" -Token $teacher.accessToken).Status

Write-Case "Step 5 - GET /mosque-admins (pagination)"
$n++
Add-TestResult -Number $n -Case "List admins as SUPER_ADMIN" -Method "GET" -Endpoint "/api/v1/mosque-admins" -Role "SUPER_ADMIN" -Input "default page" -Expected 200 -Actual (Invoke-ApiRequest -Method "Get" -Path "/api/v1/mosque-admins" -Token $admin.accessToken).Status
$n++
Add-TestResult -Number $n -Case "List admins paginated" -Method "GET" -Endpoint "/api/v1/mosque-admins?page=0&size=1" -Role "SUPER_ADMIN" -Input "page=0&size=1" -Expected 200 -Actual (Invoke-ApiRequest -Method "Get" -Path "/api/v1/mosque-admins" -Token $admin.accessToken -Query @{ page = 0; size = 1 }).Status
if ($mosqueId -and $twAdmin) {
    $n++
    Add-TestResult -Number $n -Case "List admins as MOSQUE_ADMIN (scoped)" -Method "GET" -Endpoint "/api/v1/mosque-admins" -Role "MOSQUE_ADMIN" -Input "tw admin" -Expected 200 -Actual (Invoke-ApiRequest -Method "Get" -Path "/api/v1/mosque-admins" -Token $twAdmin.accessToken).Status
} else {
    $n++
    Add-TestResult -Number $n -Case "List admins as MOSQUE_ADMIN (scoped)" -Method "GET" -Endpoint "/api/v1/mosque-admins" -Role "MOSQUE_ADMIN" -Input "no mosque" -Expected 200 -Actual 0 -Note "SKIPPED"
}
$n++
Add-TestResult -Number $n -Case "List admins seed admin (empty page)" -Method "GET" -Endpoint "/api/v1/mosque-admins" -Role "MOSQUE_ADMIN" -Input "seed admin" -Expected 200 -Actual (Invoke-ApiRequest -Method "Get" -Path "/api/v1/mosque-admins" -Token $mosqueAdmin.accessToken).Status
$n++
Add-TestResult -Number $n -Case "List admins as TEACHER" -Method "GET" -Endpoint "/api/v1/mosque-admins" -Role "TEACHER" -Input "" -Expected 403 -Actual (Invoke-ApiRequest -Method "Get" -Path "/api/v1/mosque-admins" -Token $teacher.accessToken).Status
$n++
Add-TestResult -Number $n -Case "List admins as STUDENT" -Method "GET" -Endpoint "/api/v1/mosque-admins" -Role "STUDENT" -Input "" -Expected 403 -Actual (Invoke-ApiRequest -Method "Get" -Path "/api/v1/mosque-admins" -Token $student.accessToken).Status
$n++
Add-TestResult -Number $n -Case "List admins as PARENT" -Method "GET" -Endpoint "/api/v1/mosque-admins" -Role "PARENT" -Input "" -Expected 403 -Actual (Invoke-ApiRequest -Method "Get" -Path "/api/v1/mosque-admins" -Token $parent.accessToken).Status

Write-Case "Step 6 - GET /mosque-admins/{id}"
$adminRowId = $null
if ($mosqueId -and $twAdmin) {
    $me = Invoke-ApiRequest -Method "Get" -Path "/api/v1/mosque-admins" -Token $twAdmin.accessToken
    if ($me.Status -eq 200) {
        $content = @(($me.Body | ConvertFrom-Json).data.content)
        foreach ($row in $content) {
            if ($row.user.id -eq $twAdmin.userId) { $adminRowId = $row.id; break }
        }
        if (-not $adminRowId -and $content.Count -gt 0) { $adminRowId = $content[0].id }
        Write-Host "Admin row id: $adminRowId"
    }
}
if ($adminRowId) {
    $n++
    Add-TestResult -Number $n -Case "Get admin row as SUPER_ADMIN" -Method "GET" -Endpoint "/api/v1/mosque-admins/{id}" -Role "SUPER_ADMIN" -Input "row=$adminRowId" -Expected 200 -Actual (Invoke-ApiRequest -Method "Get" -Path "/api/v1/mosque-admins/$adminRowId" -Token $admin.accessToken).Status
    $n++
    Add-TestResult -Number $n -Case "Get admin row as MOSQUE_ADMIN (same mosque)" -Method "GET" -Endpoint "/api/v1/mosque-admins/{id}" -Role "MOSQUE_ADMIN" -Input "row=$adminRowId" -Expected 200 -Actual (Invoke-ApiRequest -Method "Get" -Path "/api/v1/mosque-admins/$adminRowId" -Token $twAdmin.accessToken).Status
    $n++
    Add-TestResult -Number $n -Case "Get admin row as seed MOSQUE_ADMIN (no assignment)" -Method "GET" -Endpoint "/api/v1/mosque-admins/{id}" -Role "MOSQUE_ADMIN" -Input "seed admin" -Expected 403 -Actual (Invoke-ApiRequest -Method "Get" -Path "/api/v1/mosque-admins/$adminRowId" -Token $mosqueAdmin.accessToken).Status
    $n++
    Add-TestResult -Number $n -Case "Get admin row as TEACHER" -Method "GET" -Endpoint "/api/v1/mosque-admins/{id}" -Role "TEACHER" -Input "" -Expected 403 -Actual (Invoke-ApiRequest -Method "Get" -Path "/api/v1/mosque-admins/$adminRowId" -Token $teacher.accessToken).Status
} else {
    $n++
    Add-TestResult -Number $n -Case "Get admin row as SUPER_ADMIN" -Method "GET" -Endpoint "/api/v1/mosque-admins/{id}" -Role "SUPER_ADMIN" -Input "no row" -Expected 200 -Actual 0 -Note "SKIPPED"
    $n++
    Add-TestResult -Number $n -Case "Get admin row as MOSQUE_ADMIN (same mosque)" -Method "GET" -Endpoint "/api/v1/mosque-admins/{id}" -Role "MOSQUE_ADMIN" -Input "no row" -Expected 200 -Actual 0 -Note "SKIPPED"
    $n++
    Add-TestResult -Number $n -Case "Get admin row as seed MOSQUE_ADMIN (no assignment)" -Method "GET" -Endpoint "/api/v1/mosque-admins/{id}" -Role "MOSQUE_ADMIN" -Input "no row" -Expected 403 -Actual 0 -Note "SKIPPED"
    $n++
    Add-TestResult -Number $n -Case "Get admin row as TEACHER" -Method "GET" -Endpoint "/api/v1/mosque-admins/{id}" -Role "TEACHER" -Input "" -Expected 403 -Actual 0 -Note "SKIPPED"
}
$n++
Add-TestResult -Number $n -Case "Get admin row invalid UUID" -Method "GET" -Endpoint "/api/v1/mosque-admins/{id}" -Role "SUPER_ADMIN" -Input "not-a-uuid" -Expected 400 -Actual (Invoke-ApiRequest -Method "Get" -Path "/api/v1/mosque-admins/not-a-uuid" -Token $admin.accessToken).Status
$n++
Add-TestResult -Number $n -Case "Get admin row random UUID" -Method "GET" -Endpoint "/api/v1/mosque-admins/{id}" -Role "SUPER_ADMIN" -Input "random uuid" -Expected 404 -Actual (Invoke-ApiRequest -Method "Get" -Path "/api/v1/mosque-admins/$([guid]::NewGuid())" -Token $admin.accessToken).Status
$n++
Add-TestResult -Number $n -Case "Get admin row without token" -Method "GET" -Endpoint "/api/v1/mosque-admins/{id}" -Role "none" -Input "" -Expected 401 -Actual (Invoke-ApiRequest -Method "Get" -Path "/api/v1/mosque-admins/$([guid]::NewGuid())").Status

Write-Case "Step 7 - POST /mosque-admins (SUPER_ADMIN, audit reason required)"
$crudRowId = $null
if (-not $ReadOnly -and $mosqueId -and $twAdmin3) {
    $auditOk = Invoke-ApiRequest -Method "Post" -Path "/api/v1/mosque-admins" -Token $admin.accessToken -Headers @{ "X-Audit-Reason" = "Assigning test admin" } -Body (Test-Body @{
        userId = $twAdmin3.userId; mosqueId = $mosqueId; permission = "MANAGE_STUDENTS"; isPrimaryAdmin = $false
    })
    $n++
    Add-TestResult -Number $n -Case "Create admin with audit header" -Method "POST" -Endpoint "/api/v1/mosque-admins" -Role "SUPER_ADMIN" -Input "user+mosque+header" -Expected 201 -Actual $auditOk.Status
    if ($auditOk.Status -eq 201) { $crudRowId = ($auditOk.Body | ConvertFrom-Json).data.id }
} else {
    $n++
    Add-TestResult -Number $n -Case "Create admin with audit header" -Method "POST" -Endpoint "/api/v1/mosque-admins" -Role "SUPER_ADMIN" -Input "no mosque" -Expected 201 -Actual 0 -Note "SKIPPED"
}
if (-not $ReadOnly -and $mosqueId -and $twAdmin3) {
    $n++
    Add-TestResult -Number $n -Case "Create admin without audit reason" -Method "POST" -Endpoint "/api/v1/mosque-admins" -Role "SUPER_ADMIN" -Input "user+mosque" -Expected 400 -Actual (Invoke-ApiRequest -Method "Post" -Path "/api/v1/mosque-admins" -Token $admin.accessToken -Body (Test-Body @{ userId = $twAdmin3.userId; mosqueId = $mosqueId; permission = "MANAGE_STUDENTS" })).Status
    $n++
    Add-TestResult -Number $n -Case "Create admin with short audit reason" -Method "POST" -Endpoint "/api/v1/mosque-admins" -Role "SUPER_ADMIN" -Input "reason=short" -Expected 400 -Actual (Invoke-ApiRequest -Method "Post" -Path "/api/v1/mosque-admins" -Token $admin.accessToken -Headers @{ "X-Audit-Reason" = "short" } -Body (Test-Body @{ userId = $twAdmin3.userId; mosqueId = $mosqueId; permission = "MANAGE_STUDENTS" })).Status
    $n++
    Add-TestResult -Number $n -Case "Create admin as MOSQUE_ADMIN" -Method "POST" -Endpoint "/api/v1/mosque-admins" -Role "MOSQUE_ADMIN" -Input "tw admin" -Expected 403 -Actual (Invoke-ApiRequest -Method "Post" -Path "/api/v1/mosque-admins" -Token $twAdmin.accessToken -Body (Test-Body @{ userId = $twAdmin3.userId; mosqueId = $mosqueId; permission = "MANAGE_STUDENTS" })).Status
    $n++
    Add-TestResult -Number $n -Case "Create admin unknown user" -Method "POST" -Endpoint "/api/v1/mosque-admins" -Role "SUPER_ADMIN" -Input "random user uuid" -Expected 404 -Actual (Invoke-ApiRequest -Method "Post" -Path "/api/v1/mosque-admins" -Token $admin.accessToken -Headers @{ "X-Audit-Reason" = "Assigning test admin" } -Body (Test-Body @{ userId = [guid]::NewGuid(); mosqueId = $mosqueId; permission = "MANAGE_STUDENTS" })).Status
    $n++
    Add-TestResult -Number $n -Case "Create admin duplicate (user,mosque)" -Method "POST" -Endpoint "/api/v1/mosque-admins" -Role "SUPER_ADMIN" -Input "tw admin3 again" -Expected 400 -Actual (Invoke-ApiRequest -Method "Post" -Path "/api/v1/mosque-admins" -Token $admin.accessToken -Headers @{ "X-Audit-Reason" = "Assigning test admin again" } -Body (Test-Body @{ userId = $twAdmin3.userId; mosqueId = $mosqueId; permission = "MANAGE_STUDENTS" })).Status
} else {
    $n++
    Add-TestResult -Number $n -Case "Create admin without audit reason" -Method "POST" -Endpoint "/api/v1/mosque-admins" -Role "SUPER_ADMIN" -Input "no mosque" -Expected 400 -Actual 0 -Note "SKIPPED"
    $n++
    Add-TestResult -Number $n -Case "Create admin with short audit reason" -Method "POST" -Endpoint "/api/v1/mosque-admins" -Role "SUPER_ADMIN" -Input "no mosque" -Expected 400 -Actual 0 -Note "SKIPPED"
    $n++
    Add-TestResult -Number $n -Case "Create admin as MOSQUE_ADMIN" -Method "POST" -Endpoint "/api/v1/mosque-admins" -Role "MOSQUE_ADMIN" -Input "no mosque" -Expected 403 -Actual 0 -Note "SKIPPED"
    $n++
    Add-TestResult -Number $n -Case "Create admin unknown user" -Method "POST" -Endpoint "/api/v1/mosque-admins" -Role "SUPER_ADMIN" -Input "no mosque" -Expected 404 -Actual 0 -Note "SKIPPED"
    $n++
    Add-TestResult -Number $n -Case "Create admin duplicate (user,mosque)" -Method "POST" -Endpoint "/api/v1/mosque-admins" -Role "SUPER_ADMIN" -Input "no mosque" -Expected 400 -Actual 0 -Note "SKIPPED"
}
$n++
Add-TestResult -Number $n -Case "Create admin without token" -Method "POST" -Endpoint "/api/v1/mosque-admins" -Role "none" -Input "" -Expected 401 -Actual (Invoke-ApiRequest -Method "Post" -Path "/api/v1/mosque-admins" -Body (Test-Body @{ userId = [guid]::NewGuid(); mosqueId = [guid]::NewGuid() })).Status

Write-Case "Step 8 - PUT /mosque-admins/{id} (SUPER_ADMIN, audit reason required)"
if ($crudRowId) {
    $n++
    Add-TestResult -Number $n -Case "Update admin with audit header" -Method "PUT" -Endpoint "/api/v1/mosque-admins/{id}" -Role "SUPER_ADMIN" -Input "row=$crudRowId+header" -Expected 200 -Actual (Invoke-ApiRequest -Method "Put" -Path "/api/v1/mosque-admins/$crudRowId" -Token $admin.accessToken -Headers @{ "X-Audit-Reason" = "Updating permission" } -Body (Test-Body @{ permission = "MANAGE_CIRCLES" })).Status
    $n++
    Add-TestResult -Number $n -Case "Update admin without audit reason" -Method "PUT" -Endpoint "/api/v1/mosque-admins/{id}" -Role "SUPER_ADMIN" -Input "row=$crudRowId" -Expected 400 -Actual (Invoke-ApiRequest -Method "Put" -Path "/api/v1/mosque-admins/$crudRowId" -Token $admin.accessToken -Body (Test-Body @{ permission = "MANAGE_CIRCLES" })).Status
    $n++
    Add-TestResult -Number $n -Case "Update admin as MOSQUE_ADMIN" -Method "PUT" -Endpoint "/api/v1/mosque-admins/{id}" -Role "MOSQUE_ADMIN" -Input "row=$crudRowId" -Expected 403 -Actual (Invoke-ApiRequest -Method "Put" -Path "/api/v1/mosque-admins/$crudRowId" -Token $twAdmin.accessToken -Body (Test-Body @{ permission = "MANAGE_CIRCLES" })).Status
} else {
    $n++
    Add-TestResult -Number $n -Case "Update admin with audit header" -Method "PUT" -Endpoint "/api/v1/mosque-admins/{id}" -Role "SUPER_ADMIN" -Input "no row" -Expected 200 -Actual 0 -Note "SKIPPED"
    $n++
    Add-TestResult -Number $n -Case "Update admin without audit reason" -Method "PUT" -Endpoint "/api/v1/mosque-admins/{id}" -Role "SUPER_ADMIN" -Input "no row" -Expected 400 -Actual 0 -Note "SKIPPED"
    $n++
    Add-TestResult -Number $n -Case "Update admin as MOSQUE_ADMIN" -Method "PUT" -Endpoint "/api/v1/mosque-admins/{id}" -Role "MOSQUE_ADMIN" -Input "no row" -Expected 403 -Actual 0 -Note "SKIPPED"
}
$n++
Add-TestResult -Number $n -Case "Update admin random UUID" -Method "PUT" -Endpoint "/api/v1/mosque-admins/{id}" -Role "SUPER_ADMIN" -Input "random uuid" -Expected 404 -Actual (Invoke-ApiRequest -Method "Put" -Path "/api/v1/mosque-admins/$([guid]::NewGuid())" -Token $admin.accessToken -Headers @{ "X-Audit-Reason" = "Updating permission" } -Body (Test-Body @{ permission = "MANAGE_CIRCLES" })).Status
$n++
Add-TestResult -Number $n -Case "Update admin without token" -Method "PUT" -Endpoint "/api/v1/mosque-admins/{id}" -Role "none" -Input "" -Expected 401 -Actual (Invoke-ApiRequest -Method "Put" -Path "/api/v1/mosque-admins/$([guid]::NewGuid())" -Body (Test-Body @{ permission = "MANAGE_CIRCLES" })).Status

Write-Case "Step 9 - DELETE /mosque-admins/{id} (SUPER_ADMIN, audit header required)"
if ($crudRowId) {
    $n++
    Add-TestResult -Number $n -Case "Delete admin with audit header" -Method "DELETE" -Endpoint "/api/v1/mosque-admins/{id}" -Role "SUPER_ADMIN" -Input "row=$crudRowId+header" -Expected 200 -Actual (Invoke-ApiRequest -Method "Delete" -Path "/api/v1/mosque-admins/$crudRowId" -Token $admin.accessToken -Headers @{ "X-Audit-Reason" = "Removing test admin" }).Status
    $n++
    Add-TestResult -Number $n -Case "Delete admin without audit header" -Method "DELETE" -Endpoint "/api/v1/mosque-admins/{id}" -Role "SUPER_ADMIN" -Input "row=$crudRowId" -Expected 400 -Actual (Invoke-ApiRequest -Method "Delete" -Path "/api/v1/mosque-admins/$crudRowId" -Token $admin.accessToken).Status
    $n++
    Add-TestResult -Number $n -Case "Delete admin again (already removed)" -Method "DELETE" -Endpoint "/api/v1/mosque-admins/{id}" -Role "SUPER_ADMIN" -Input "row=$crudRowId" -Expected 404 -Actual (Invoke-ApiRequest -Method "Delete" -Path "/api/v1/mosque-admins/$crudRowId" -Token $admin.accessToken -Headers @{ "X-Audit-Reason" = "Removing test admin again" }).Status
} else {
    $n++
    Add-TestResult -Number $n -Case "Delete admin with audit header" -Method "DELETE" -Endpoint "/api/v1/mosque-admins/{id}" -Role "SUPER_ADMIN" -Input "no row" -Expected 200 -Actual 0 -Note "SKIPPED"
    $n++
    Add-TestResult -Number $n -Case "Delete admin without audit header" -Method "DELETE" -Endpoint "/api/v1/mosque-admins/{id}" -Role "SUPER_ADMIN" -Input "no row" -Expected 400 -Actual 0 -Note "SKIPPED"
    $n++
    Add-TestResult -Number $n -Case "Delete admin again (already removed)" -Method "DELETE" -Endpoint "/api/v1/mosque-admins/{id}" -Role "SUPER_ADMIN" -Input "no row" -Expected 404 -Actual 0 -Note "SKIPPED"
}
$n++
Add-TestResult -Number $n -Case "Delete admin as MOSQUE_ADMIN" -Method "DELETE" -Endpoint "/api/v1/mosque-admins/{id}" -Role "MOSQUE_ADMIN" -Input "seed admin" -Expected 403 -Actual (Invoke-ApiRequest -Method "Delete" -Path "/api/v1/mosque-admins/$([guid]::NewGuid())" -Token $mosqueAdmin.accessToken -Headers @{ "X-Audit-Reason" = "Removing test admin" }).Status
$n++
Add-TestResult -Number $n -Case "Delete admin random UUID" -Method "DELETE" -Endpoint "/api/v1/mosque-admins/{id}" -Role "SUPER_ADMIN" -Input "random uuid" -Expected 404 -Actual (Invoke-ApiRequest -Method "Delete" -Path "/api/v1/mosque-admins/$([guid]::NewGuid())" -Token $admin.accessToken -Headers @{ "X-Audit-Reason" = "Removing test admin" }).Status
$n++
Add-TestResult -Number $n -Case "Delete admin without token" -Method "DELETE" -Endpoint "/api/v1/mosque-admins/{id}" -Role "none" -Input "" -Expected 401 -Actual (Invoke-ApiRequest -Method "Delete" -Path "/api/v1/mosque-admins/$([guid]::NewGuid())").Status

Write-Case "Step 10 - Cleanup"
if (-not $ReadOnly -and $mosqueId) {
    $del = Invoke-ApiRequest -Method "Delete" -Path "/api/v1/mosques/$mosqueId" -Token $admin.accessToken
    $n++
    Add-TestResult -Number $n -Case "Cleanup: deactivate throwaway mosque" -Method "DELETE" -Endpoint "/api/v1/mosques/{id}" -Role "SUPER_ADMIN" -Input "mosque=$mosqueId" -Expected 200 -Actual $del.Status
} else {
    $n++
    Add-TestResult -Number $n -Case "Cleanup: deactivate throwaway mosque" -Method "DELETE" -Endpoint "/api/v1/mosques/{id}" -Role "SUPER_ADMIN" -Input "no mosque" -Expected 200 -Actual 0 -Note "SKIPPED"
}

Show-TestResults
