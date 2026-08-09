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
$teacherInviteCode = $null
$studentInviteCode = $null
$twAdmin = $null
$twAdmin2 = $null
$twAdmin3 = $null
$twTeacher = $null
$twStudent = $null

if (-not $ReadOnly) {
    $twAdmin = Ensure-User "MQ Throwaway Admin" "mq.admin$runSuffix@darb.app" "MqAdmin123!" "MOSQUE_ADMIN"
    $twAdmin2 = Ensure-User "MQ Throwaway Admin2" "mq.admin2$runSuffix@darb.app" "MqAdmin123!" "MOSQUE_ADMIN"
    $twAdmin3 = Ensure-User "MQ Throwaway Admin3" "mq.admin3$runSuffix@darb.app" "MqAdmin123!" "MOSQUE_ADMIN"
    $twTeacher = Ensure-User "MQ Throwaway Teacher" "mq.teacher$runSuffix@darb.app" "MqTeacher123!" "TEACHER"
    $twStudent = Ensure-User "MQ Throwaway Student" "mq.student$runSuffix@darb.app" "MqStudent123!" "STUDENT"

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
            $teacherInviteCode = $od.teacherInviteCode
            $studentInviteCode = $od.studentInviteCode
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
Add-TestResult -Number $n -Case "GET /mosques without token" -Method "GET" -Endpoint "/api/v1/mosques" -Role "none" -Input "" -Expected 401 -Actual (Invoke-ApiRequest -Method "Get" -Path "/api/v1/mosques").Status
$n++
Add-TestResult -Number $n -Case "GET /mosques with garbage token" -Method "GET" -Endpoint "/api/v1/mosques" -Role "garbage" -Input "Bearer xyz" -Expected 401 -Actual (Invoke-ApiRequest -Method "Get" -Path "/api/v1/mosques" -Token "xyz").Status

Write-Case "Step 2 - GET /mosques (list, any authenticated)"
$n++
Add-TestResult -Number $n -Case "List mosques" -Method "GET" -Endpoint "/api/v1/mosques" -Role "SUPER_ADMIN" -Input "default page" -Expected 200 -Actual (Invoke-ApiRequest -Method "Get" -Path "/api/v1/mosques" -Token $admin.accessToken).Status
$n++
Add-TestResult -Number $n -Case "List mosques paginated" -Method "GET" -Endpoint "/api/v1/mosques?page=0&size=2" -Role "SUPER_ADMIN" -Input "page=0&size=2" -Expected 200 -Actual (Invoke-ApiRequest -Method "Get" -Path "/api/v1/mosques" -Token $admin.accessToken -Query @{ page = 0; size = 2 }).Status
$n++
Add-TestResult -Number $n -Case "List mosques as MOSQUE_ADMIN" -Method "GET" -Endpoint "/api/v1/mosques" -Role "MOSQUE_ADMIN" -Input "" -Expected 200 -Actual (Invoke-ApiRequest -Method "Get" -Path "/api/v1/mosques" -Token $mosqueAdmin.accessToken).Status
$n++
Add-TestResult -Number $n -Case "List mosques as STUDENT" -Method "GET" -Endpoint "/api/v1/mosques" -Role "STUDENT" -Input "" -Expected 200 -Actual (Invoke-ApiRequest -Method "Get" -Path "/api/v1/mosques" -Token $student.accessToken).Status
$n++
Add-TestResult -Number $n -Case "List mosques as PARENT" -Method "GET" -Endpoint "/api/v1/mosques" -Role "PARENT" -Input "" -Expected 200 -Actual (Invoke-ApiRequest -Method "Get" -Path "/api/v1/mosques" -Token $parent.accessToken).Status

Write-Case "Step 3 - GET /mosques/{id} (any authenticated, scoped)"
$n++
Add-TestResult -Number $n -Case "Get by invalid UUID" -Method "GET" -Endpoint "/api/v1/mosques/{id}" -Role "SUPER_ADMIN" -Input "not-a-uuid" -Expected 400 -Actual (Invoke-ApiRequest -Method "Get" -Path "/api/v1/mosques/not-a-uuid" -Token $admin.accessToken).Status
$n++
Add-TestResult -Number $n -Case "Get by random UUID" -Method "GET" -Endpoint "/api/v1/mosques/{id}" -Role "SUPER_ADMIN" -Input "random uuid" -Expected 404 -Actual (Invoke-ApiRequest -Method "Get" -Path "/api/v1/mosques/$([guid]::NewGuid())" -Token $admin.accessToken).Status
$n++
Add-TestResult -Number $n -Case "Get mosque without token" -Method "GET" -Endpoint "/api/v1/mosques/{id}" -Role "none" -Input "" -Expected 401 -Actual (Invoke-ApiRequest -Method "Get" -Path "/api/v1/mosques/$([guid]::NewGuid())").Status
if ($mosqueId) {
    $n++
    Add-TestResult -Number $n -Case "Get by id as SUPER_ADMIN" -Method "GET" -Endpoint "/api/v1/mosques/{id}" -Role "SUPER_ADMIN" -Input "mosque=$mosqueId" -Expected 200 -Actual (Invoke-ApiRequest -Method "Get" -Path "/api/v1/mosques/$mosqueId" -Token $admin.accessToken).Status
    $n++
    Add-TestResult -Number $n -Case "Get by id as MOSQUE_ADMIN (no assignment)" -Method "GET" -Endpoint "/api/v1/mosques/{id}" -Role "MOSQUE_ADMIN" -Input "mosque=$mosqueId" -Expected 403 -Actual (Invoke-ApiRequest -Method "Get" -Path "/api/v1/mosques/$mosqueId" -Token $mosqueAdmin.accessToken).Status
    $n++
    Add-TestResult -Number $n -Case "Get by id as TEACHER (no assignment)" -Method "GET" -Endpoint "/api/v1/mosques/{id}" -Role "TEACHER" -Input "mosque=$mosqueId" -Expected 403 -Actual (Invoke-ApiRequest -Method "Get" -Path "/api/v1/mosques/$mosqueId" -Token $teacher.accessToken).Status
    $n++
    Add-TestResult -Number $n -Case "Get by id as STUDENT (no assignment)" -Method "GET" -Endpoint "/api/v1/mosques/{id}" -Role "STUDENT" -Input "mosque=$mosqueId" -Expected 403 -Actual (Invoke-ApiRequest -Method "Get" -Path "/api/v1/mosques/$mosqueId" -Token $student.accessToken).Status
} else {
    $n++
    Add-TestResult -Number $n -Case "Get by id as SUPER_ADMIN" -Method "GET" -Endpoint "/api/v1/mosques/{id}" -Role "SUPER_ADMIN" -Input "no mosque" -Expected 200 -Actual 0 -Note "SKIPPED"
    $n++
    Add-TestResult -Number $n -Case "Get by id as MOSQUE_ADMIN (no assignment)" -Method "GET" -Endpoint "/api/v1/mosques/{id}" -Role "MOSQUE_ADMIN" -Input "no mosque" -Expected 403 -Actual 0 -Note "SKIPPED"
    $n++
    Add-TestResult -Number $n -Case "Get by id as TEACHER (no assignment)" -Method "GET" -Endpoint "/api/v1/mosques/{id}" -Role "TEACHER" -Input "no mosque" -Expected 403 -Actual 0 -Note "SKIPPED"
    $n++
    Add-TestResult -Number $n -Case "Get by id as STUDENT (no assignment)" -Method "GET" -Endpoint "/api/v1/mosques/{id}" -Role "STUDENT" -Input "no mosque" -Expected 403 -Actual 0 -Note "SKIPPED"
}

Write-Case "Step 4 - GET /mosques/search (TEACHER, STUDENT)"
$n++
Add-TestResult -Number $n -Case "Search all as TEACHER" -Method "GET" -Endpoint "/api/v1/mosques/search" -Role "TEACHER" -Input "q=@darb" -Expected 200 -Actual (Invoke-ApiRequest -Method "Get" -Path "/api/v1/mosques/search" -Token $teacher.accessToken -Query @{ q = "@darb" }).Status
$n++
Add-TestResult -Number $n -Case "Search city as STUDENT" -Method "GET" -Endpoint "/api/v1/mosques/search" -Role "STUDENT" -Input "city=Riyadh" -Expected 200 -Actual (Invoke-ApiRequest -Method "Get" -Path "/api/v1/mosques/search" -Token $student.accessToken -Query @{ city = "Riyadh" }).Status
$n++
Add-TestResult -Number $n -Case "Search as MOSQUE_ADMIN" -Method "GET" -Endpoint "/api/v1/mosques/search" -Role "MOSQUE_ADMIN" -Input "q=Riyadh" -Expected 403 -Actual (Invoke-ApiRequest -Method "Get" -Path "/api/v1/mosques/search" -Token $mosqueAdmin.accessToken -Query @{ q = "Riyadh" }).Status
$n++
Add-TestResult -Number $n -Case "Search as SUPER_ADMIN" -Method "GET" -Endpoint "/api/v1/mosques/search" -Role "SUPER_ADMIN" -Input "q=Riyadh" -Expected 403 -Actual (Invoke-ApiRequest -Method "Get" -Path "/api/v1/mosques/search" -Token $admin.accessToken -Query @{ q = "Riyadh" }).Status
$n++
Add-TestResult -Number $n -Case "Search as PARENT" -Method "GET" -Endpoint "/api/v1/mosques/search" -Role "PARENT" -Input "q=Riyadh" -Expected 403 -Actual (Invoke-ApiRequest -Method "Get" -Path "/api/v1/mosques/search" -Token $parent.accessToken -Query @{ q = "Riyadh" }).Status
$n++
Add-TestResult -Number $n -Case "Search without token" -Method "GET" -Endpoint "/api/v1/mosques/search" -Role "none" -Input "" -Expected 401 -Actual (Invoke-ApiRequest -Method "Get" -Path "/api/v1/mosques/search").Status

Write-Case "Step 5 - GET /mosques/member-join/preview (TEACHER, STUDENT)"
$n++
Add-TestResult -Number $n -Case "Preview missing code" -Method "GET" -Endpoint "/api/v1/mosques/member-join/preview" -Role "TEACHER" -Input "role=TEACHER only" -Expected 400 -Actual (Invoke-ApiRequest -Method "Get" -Path "/api/v1/mosques/member-join/preview" -Token $teacher.accessToken -Query @{ role = "TEACHER" }).Status
$n++
Add-TestResult -Number $n -Case "Preview invalid code" -Method "GET" -Endpoint "/api/v1/mosques/member-join/preview" -Role "TEACHER" -Input "code=bogus" -Expected 404 -Actual (Invoke-ApiRequest -Method "Get" -Path "/api/v1/mosques/member-join/preview" -Token $teacher.accessToken -Query @{ code = "bogus"; role = "TEACHER" }).Status
$n++
Add-TestResult -Number $n -Case "Preview role=PARENT" -Method "GET" -Endpoint "/api/v1/mosques/member-join/preview" -Role "TEACHER" -Input "role=PARENT" -Expected 404 -Actual (Invoke-ApiRequest -Method "Get" -Path "/api/v1/mosques/member-join/preview" -Token $teacher.accessToken -Query @{ code = "whatever"; role = "PARENT" }).Status
if ($teacherInviteCode) {
    $n++
    Add-TestResult -Number $n -Case "Preview valid teacher code" -Method "GET" -Endpoint "/api/v1/mosques/member-join/preview" -Role "TEACHER" -Input "teacher code" -Expected 200 -Actual (Invoke-ApiRequest -Method "Get" -Path "/api/v1/mosques/member-join/preview" -Token $teacher.accessToken -Query @{ code = $teacherInviteCode; role = "TEACHER" }).Status
} else {
    $n++
    Add-TestResult -Number $n -Case "Preview valid teacher code" -Method "GET" -Endpoint "/api/v1/mosques/member-join/preview" -Role "TEACHER" -Input "no code" -Expected 200 -Actual 0 -Note "SKIPPED"
}
$n++
Add-TestResult -Number $n -Case "Preview as MOSQUE_ADMIN" -Method "GET" -Endpoint "/api/v1/mosques/member-join/preview" -Role "MOSQUE_ADMIN" -Input "code=whatever" -Expected 403 -Actual (Invoke-ApiRequest -Method "Get" -Path "/api/v1/mosques/member-join/preview" -Token $mosqueAdmin.accessToken -Query @{ code = "whatever"; role = "TEACHER" }).Status
$n++
Add-TestResult -Number $n -Case "Preview without token" -Method "GET" -Endpoint "/api/v1/mosques/member-join/preview" -Role "none" -Input "" -Expected 401 -Actual (Invoke-ApiRequest -Method "Get" -Path "/api/v1/mosques/member-join/preview").Status

Write-Case "Step 6 - Join requests: POST /join-requests + DELETE /join-requests/my"
$joinOk = $false
if (-not $ReadOnly -and $mosqueId -and $twStudent) {
    $jr = Invoke-ApiRequest -Method "Post" -Path "/api/v1/mosques/join-requests" -Token $twStudent.accessToken -Body (Test-Body @{ mosqueId = $mosqueId })
    $joinOk = ($jr.Status -eq 201)
    $n++
    Add-TestResult -Number $n -Case "Create join request as STUDENT" -Method "POST" -Endpoint "/api/v1/mosques/join-requests" -Role "STUDENT" -Input "mosque=$mosqueId" -Expected 201 -Actual $jr.Status
} else {
    $n++
    Add-TestResult -Number $n -Case "Create join request as STUDENT" -Method "POST" -Endpoint "/api/v1/mosques/join-requests" -Role "STUDENT" -Input "mosque" -Expected 201 -Actual 0 -Note "SKIPPED"
}
if ($joinOk) {
    $n++
    Add-TestResult -Number $n -Case "Create join request while pending" -Method "POST" -Endpoint "/api/v1/mosques/join-requests" -Role "STUDENT" -Input "same mosque" -Expected 403 -Actual (Invoke-ApiRequest -Method "Post" -Path "/api/v1/mosques/join-requests" -Token $twStudent.accessToken -Body (Test-Body @{ mosqueId = $mosqueId })).Status
    $n++
    Add-TestResult -Number $n -Case "Search blocked while pending" -Method "GET" -Endpoint "/api/v1/mosques/search" -Role "STUDENT" -Input "pending request" -Expected 403 -Actual (Invoke-ApiRequest -Method "Get" -Path "/api/v1/mosques/search" -Token $twStudent.accessToken -Query @{ q = "Riyadh" }).Status
    $n++
    Add-TestResult -Number $n -Case "Cancel my join request" -Method "DELETE" -Endpoint "/api/v1/mosques/join-requests/my" -Role "STUDENT" -Input "" -Expected 200 -Actual (Invoke-ApiRequest -Method "Delete" -Path "/api/v1/mosques/join-requests/my" -Token $twStudent.accessToken).Status
    $n++
    Add-TestResult -Number $n -Case "Cancel again (no pending)" -Method "DELETE" -Endpoint "/api/v1/mosques/join-requests/my" -Role "STUDENT" -Input "" -Expected 404 -Actual (Invoke-ApiRequest -Method "Delete" -Path "/api/v1/mosques/join-requests/my" -Token $twStudent.accessToken).Status
} else {
    $n++
    Add-TestResult -Number $n -Case "Create join request while pending" -Method "POST" -Endpoint "/api/v1/mosques/join-requests" -Role "STUDENT" -Input "" -Expected 403 -Actual 0 -Note "SKIPPED"
    $n++
    Add-TestResult -Number $n -Case "Search blocked while pending" -Method "GET" -Endpoint "/api/v1/mosques/search" -Role "STUDENT" -Input "" -Expected 403 -Actual 0 -Note "SKIPPED"
    $n++
    Add-TestResult -Number $n -Case "Cancel my join request" -Method "DELETE" -Endpoint "/api/v1/mosques/join-requests/my" -Role "STUDENT" -Input "" -Expected 200 -Actual 0 -Note "SKIPPED"
    $n++
    Add-TestResult -Number $n -Case "Cancel again (no pending)" -Method "DELETE" -Endpoint "/api/v1/mosques/join-requests/my" -Role "STUDENT" -Input "" -Expected 404 -Actual 0 -Note "SKIPPED"
}
if (-not $ReadOnly -and $mosqueId -and $twTeacher) {
    $n++
    Add-TestResult -Number $n -Case "Create join request as TEACHER" -Method "POST" -Endpoint "/api/v1/mosques/join-requests" -Role "TEACHER" -Input "mosque=$mosqueId" -Expected 201 -Actual (Invoke-ApiRequest -Method "Post" -Path "/api/v1/mosques/join-requests" -Token $twTeacher.accessToken -Body (Test-Body @{ mosqueId = $mosqueId })).Status
    $n++
    Add-TestResult -Number $n -Case "Cancel teacher join request" -Method "DELETE" -Endpoint "/api/v1/mosques/join-requests/my" -Role "TEACHER" -Input "" -Expected 200 -Actual (Invoke-ApiRequest -Method "Delete" -Path "/api/v1/mosques/join-requests/my" -Token $twTeacher.accessToken).Status
} else {
    $n++
    Add-TestResult -Number $n -Case "Create join request as TEACHER" -Method "POST" -Endpoint "/api/v1/mosques/join-requests" -Role "TEACHER" -Input "mosque" -Expected 201 -Actual 0 -Note "SKIPPED"
    $n++
    Add-TestResult -Number $n -Case "Cancel teacher join request" -Method "DELETE" -Endpoint "/api/v1/mosques/join-requests/my" -Role "TEACHER" -Input "" -Expected 200 -Actual 0 -Note "SKIPPED"
}
$n++
Add-TestResult -Number $n -Case "Create join request missing mosqueId" -Method "POST" -Endpoint "/api/v1/mosques/join-requests" -Role "TEACHER" -Input "empty body" -Expected 400 -Actual (Invoke-ApiRequest -Method "Post" -Path "/api/v1/mosques/join-requests" -Token $teacher.accessToken -Body (Test-Body @{ })).Status
$n++
Add-TestResult -Number $n -Case "Create join request as MOSQUE_ADMIN" -Method "POST" -Endpoint "/api/v1/mosques/join-requests" -Role "MOSQUE_ADMIN" -Input "mosque=$mosqueId" -Expected 403 -Actual (Invoke-ApiRequest -Method "Post" -Path "/api/v1/mosques/join-requests" -Token $mosqueAdmin.accessToken -Body (Test-Body @{ mosqueId = $mosqueId })).Status
$n++
Add-TestResult -Number $n -Case "Create join request without token" -Method "POST" -Endpoint "/api/v1/mosques/join-requests" -Role "none" -Input "" -Expected 401 -Actual (Invoke-ApiRequest -Method "Post" -Path "/api/v1/mosques/join-requests" -Body (Test-Body @{ mosqueId = $mosqueId })).Status

Write-Case "Step 7 - GET /mosques/invite-codes (MOSQUE_ADMIN)"
if ($mosqueId -and $twAdmin) {
    $n++
    Add-TestResult -Number $n -Case "Get invite codes (assigned admin)" -Method "GET" -Endpoint "/api/v1/mosques/invite-codes" -Role "MOSQUE_ADMIN" -Input "tw admin" -Expected 200 -Actual (Invoke-ApiRequest -Method "Get" -Path "/api/v1/mosques/invite-codes" -Token $twAdmin.accessToken).Status
} else {
    $n++
    Add-TestResult -Number $n -Case "Get invite codes (assigned admin)" -Method "GET" -Endpoint "/api/v1/mosques/invite-codes" -Role "MOSQUE_ADMIN" -Input "" -Expected 200 -Actual 0 -Note "SKIPPED"
}
$n++
Add-TestResult -Number $n -Case "Invite codes no assignment" -Method "GET" -Endpoint "/api/v1/mosques/invite-codes" -Role "MOSQUE_ADMIN" -Input "seed admin" -Expected 403 -Actual (Invoke-ApiRequest -Method "Get" -Path "/api/v1/mosques/invite-codes" -Token $mosqueAdmin.accessToken).Status
$n++
Add-TestResult -Number $n -Case "Invite codes as TEACHER" -Method "GET" -Endpoint "/api/v1/mosques/invite-codes" -Role "TEACHER" -Input "" -Expected 403 -Actual (Invoke-ApiRequest -Method "Get" -Path "/api/v1/mosques/invite-codes" -Token $teacher.accessToken).Status
$n++
Add-TestResult -Number $n -Case "Invite codes as SUPER_ADMIN" -Method "GET" -Endpoint "/api/v1/mosques/invite-codes" -Role "SUPER_ADMIN" -Input "" -Expected 403 -Actual (Invoke-ApiRequest -Method "Get" -Path "/api/v1/mosques/invite-codes" -Token $admin.accessToken).Status
$n++
Add-TestResult -Number $n -Case "Invite codes without token" -Method "GET" -Endpoint "/api/v1/mosques/invite-codes" -Role "none" -Input "" -Expected 401 -Actual (Invoke-ApiRequest -Method "Get" -Path "/api/v1/mosques/invite-codes").Status

Write-Case "Step 8 - GET /mosques/join/preview (MOSQUE_ADMIN)"
$n++
Add-TestResult -Number $n -Case "Admin preview missing code" -Method "GET" -Endpoint "/api/v1/mosques/join/preview" -Role "MOSQUE_ADMIN" -Input "no code" -Expected 400 -Actual (Invoke-ApiRequest -Method "Get" -Path "/api/v1/mosques/join/preview" -Token $mosqueAdmin.accessToken).Status
$n++
Add-TestResult -Number $n -Case "Admin preview invalid code" -Method "GET" -Endpoint "/api/v1/mosques/join/preview" -Role "MOSQUE_ADMIN" -Input "code=bogus" -Expected 404 -Actual (Invoke-ApiRequest -Method "Get" -Path "/api/v1/mosques/join/preview" -Token $mosqueAdmin.accessToken -Query @{ code = "bogus" }).Status
if ($adminInviteCode) {
    $n++
    Add-TestResult -Number $n -Case "Admin preview valid code" -Method "GET" -Endpoint "/api/v1/mosques/join/preview" -Role "MOSQUE_ADMIN" -Input "admin code" -Expected 200 -Actual (Invoke-ApiRequest -Method "Get" -Path "/api/v1/mosques/join/preview" -Token $mosqueAdmin.accessToken -Query @{ code = $adminInviteCode }).Status
} else {
    $n++
    Add-TestResult -Number $n -Case "Admin preview valid code" -Method "GET" -Endpoint "/api/v1/mosques/join/preview" -Role "MOSQUE_ADMIN" -Input "no code" -Expected 200 -Actual 0 -Note "SKIPPED"
}
$n++
Add-TestResult -Number $n -Case "Admin preview as TEACHER" -Method "GET" -Endpoint "/api/v1/mosques/join/preview" -Role "TEACHER" -Input "code=bogus" -Expected 403 -Actual (Invoke-ApiRequest -Method "Get" -Path "/api/v1/mosques/join/preview" -Token $teacher.accessToken -Query @{ code = "bogus" }).Status
$n++
Add-TestResult -Number $n -Case "Admin preview without token" -Method "GET" -Endpoint "/api/v1/mosques/join/preview" -Role "none" -Input "" -Expected 401 -Actual (Invoke-ApiRequest -Method "Get" -Path "/api/v1/mosques/join/preview").Status

Write-Case "Step 9 - POST /mosques/onboard (MOSQUE_ADMIN)"
if ($twAdmin) {
    $n++
    Add-TestResult -Number $n -Case "Re-onboard already assigned" -Method "POST" -Endpoint "/api/v1/mosques/onboard" -Role "MOSQUE_ADMIN" -Input "tw admin" -Expected 403 -Actual (Invoke-ApiRequest -Method "Post" -Path "/api/v1/mosques/onboard" -Token $twAdmin.accessToken -Body (Test-Body @{ name = "Again"; city = "Riyadh" })).Status
} else {
    $n++
    Add-TestResult -Number $n -Case "Re-onboard already assigned" -Method "POST" -Endpoint "/api/v1/mosques/onboard" -Role "MOSQUE_ADMIN" -Input "" -Expected 403 -Actual 0 -Note "SKIPPED"
}
$n++
Add-TestResult -Number $n -Case "Onboard as TEACHER" -Method "POST" -Endpoint "/api/v1/mosques/onboard" -Role "TEACHER" -Input "name=Test" -Expected 403 -Actual (Invoke-ApiRequest -Method "Post" -Path "/api/v1/mosques/onboard" -Token $teacher.accessToken -Body (Test-Body @{ name = "Test" })).Status
$n++
Add-TestResult -Number $n -Case "Onboard as STUDENT" -Method "POST" -Endpoint "/api/v1/mosques/onboard" -Role "STUDENT" -Input "name=Test" -Expected 403 -Actual (Invoke-ApiRequest -Method "Post" -Path "/api/v1/mosques/onboard" -Token $student.accessToken -Body (Test-Body @{ name = "Test" })).Status
$n++
Add-TestResult -Number $n -Case "Onboard as SUPER_ADMIN" -Method "POST" -Endpoint "/api/v1/mosques/onboard" -Role "SUPER_ADMIN" -Input "name=Test" -Expected 403 -Actual (Invoke-ApiRequest -Method "Post" -Path "/api/v1/mosques/onboard" -Token $admin.accessToken -Body (Test-Body @{ name = "Test" })).Status
if ($twAdmin2) {
    $n++
    Add-TestResult -Number $n -Case "Onboard blank name" -Method "POST" -Endpoint "/api/v1/mosques/onboard" -Role "MOSQUE_ADMIN" -Input "no name" -Expected 400 -Actual (Invoke-ApiRequest -Method "Post" -Path "/api/v1/mosques/onboard" -Token $twAdmin2.accessToken -Body (Test-Body @{ city = "Riyadh" })).Status
} else {
    $n++
    Add-TestResult -Number $n -Case "Onboard blank name" -Method "POST" -Endpoint "/api/v1/mosques/onboard" -Role "MOSQUE_ADMIN" -Input "" -Expected 400 -Actual 0 -Note "SKIPPED"
}
$n++
Add-TestResult -Number $n -Case "Onboard without token" -Method "POST" -Endpoint "/api/v1/mosques/onboard" -Role "none" -Input "" -Expected 401 -Actual (Invoke-ApiRequest -Method "Post" -Path "/api/v1/mosques/onboard" -Body (Test-Body @{ name = "Test" })).Status

Write-Case "Step 10 - POST /mosques/join (MOSQUE_ADMIN)"
if (-not $ReadOnly -and $adminInviteCode -and $twAdmin2) {
    $n++
    Add-TestResult -Number $n -Case "Join mosque by admin code" -Method "POST" -Endpoint "/api/v1/mosques/join" -Role "MOSQUE_ADMIN" -Input "tw admin2" -Expected 201 -Actual (Invoke-ApiRequest -Method "Post" -Path "/api/v1/mosques/join" -Token $twAdmin2.accessToken -Body (Test-Body @{ inviteCode = $adminInviteCode })).Status
    $n++
    Add-TestResult -Number $n -Case "Join again (already assigned)" -Method "POST" -Endpoint "/api/v1/mosques/join" -Role "MOSQUE_ADMIN" -Input "tw admin2" -Expected 403 -Actual (Invoke-ApiRequest -Method "Post" -Path "/api/v1/mosques/join" -Token $twAdmin2.accessToken -Body (Test-Body @{ inviteCode = $adminInviteCode })).Status
} else {
    $n++
    Add-TestResult -Number $n -Case "Join mosque by admin code" -Method "POST" -Endpoint "/api/v1/mosques/join" -Role "MOSQUE_ADMIN" -Input "code" -Expected 201 -Actual 0 -Note "SKIPPED"
    $n++
    Add-TestResult -Number $n -Case "Join again (already assigned)" -Method "POST" -Endpoint "/api/v1/mosques/join" -Role "MOSQUE_ADMIN" -Input "" -Expected 403 -Actual 0 -Note "SKIPPED"
}
if ($twAdmin3) {
    $n++
    Add-TestResult -Number $n -Case "Join invalid code" -Method "POST" -Endpoint "/api/v1/mosques/join" -Role "MOSQUE_ADMIN" -Input "tw admin3" -Expected 404 -Actual (Invoke-ApiRequest -Method "Post" -Path "/api/v1/mosques/join" -Token $twAdmin3.accessToken -Body (Test-Body @{ inviteCode = "boguscode" })).Status
    $n++
    Add-TestResult -Number $n -Case "Join blank code" -Method "POST" -Endpoint "/api/v1/mosques/join" -Role "MOSQUE_ADMIN" -Input "tw admin3" -Expected 400 -Actual (Invoke-ApiRequest -Method "Post" -Path "/api/v1/mosques/join" -Token $twAdmin3.accessToken -Body (Test-Body @{ inviteCode = "" })).Status
} else {
    $n++
    Add-TestResult -Number $n -Case "Join invalid code" -Method "POST" -Endpoint "/api/v1/mosques/join" -Role "MOSQUE_ADMIN" -Input "" -Expected 404 -Actual 0 -Note "SKIPPED"
    $n++
    Add-TestResult -Number $n -Case "Join blank code" -Method "POST" -Endpoint "/api/v1/mosques/join" -Role "MOSQUE_ADMIN" -Input "" -Expected 400 -Actual 0 -Note "SKIPPED"
}
$n++
Add-TestResult -Number $n -Case "Join as TEACHER" -Method "POST" -Endpoint "/api/v1/mosques/join" -Role "TEACHER" -Input "code=bogus" -Expected 403 -Actual (Invoke-ApiRequest -Method "Post" -Path "/api/v1/mosques/join" -Token $teacher.accessToken -Body (Test-Body @{ inviteCode = "boguscode" })).Status
$n++
Add-TestResult -Number $n -Case "Join without token" -Method "POST" -Endpoint "/api/v1/mosques/join" -Role "none" -Input "" -Expected 401 -Actual (Invoke-ApiRequest -Method "Post" -Path "/api/v1/mosques/join" -Body (Test-Body @{ inviteCode = "boguscode" })).Status

Write-Case "Step 11 - POST /mosques (SUPER_ADMIN only)"
$superMosqueId = $null
if (-not $ReadOnly) {
    $cr = Invoke-ApiRequest -Method "Post" -Path "/api/v1/mosques" -Token $admin.accessToken -Body (Test-Body @{ name = "Super Mosque $runSuffix"; city = "Jeddah" })
    if ($cr.Status -eq 201) { $superMosqueId = ($cr.Body | ConvertFrom-Json).data.id }
    $n++
    Add-TestResult -Number $n -Case "Create mosque as SUPER_ADMIN" -Method "POST" -Endpoint "/api/v1/mosques" -Role "SUPER_ADMIN" -Input "name+city" -Expected 201 -Actual $cr.Status
} else {
    $n++
    Add-TestResult -Number $n -Case "Create mosque as SUPER_ADMIN" -Method "POST" -Endpoint "/api/v1/mosques" -Role "SUPER_ADMIN" -Input "name" -Expected 201 -Actual 0 -Note "SKIPPED"
}
$n++
Add-TestResult -Number $n -Case "Create mosque as MOSQUE_ADMIN" -Method "POST" -Endpoint "/api/v1/mosques" -Role "MOSQUE_ADMIN" -Input "name=Test" -Expected 403 -Actual (Invoke-ApiRequest -Method "Post" -Path "/api/v1/mosques" -Token $mosqueAdmin.accessToken -Body (Test-Body @{ name = "Test" })).Status
$n++
Add-TestResult -Number $n -Case "Create mosque as TEACHER" -Method "POST" -Endpoint "/api/v1/mosques" -Role "TEACHER" -Input "name=Test" -Expected 403 -Actual (Invoke-ApiRequest -Method "Post" -Path "/api/v1/mosques" -Token $teacher.accessToken -Body (Test-Body @{ name = "Test" })).Status
$n++
Add-TestResult -Number $n -Case "Create mosque as STUDENT" -Method "POST" -Endpoint "/api/v1/mosques" -Role "STUDENT" -Input "name=Test" -Expected 403 -Actual (Invoke-ApiRequest -Method "Post" -Path "/api/v1/mosques" -Token $student.accessToken -Body (Test-Body @{ name = "Test" })).Status
$n++
Add-TestResult -Number $n -Case "Create mosque blank name" -Method "POST" -Endpoint "/api/v1/mosques" -Role "SUPER_ADMIN" -Input "no name" -Expected 400 -Actual (Invoke-ApiRequest -Method "Post" -Path "/api/v1/mosques" -Token $admin.accessToken -Body (Test-Body @{ city = "Riyadh" })).Status
$n++
Add-TestResult -Number $n -Case "Create mosque without token" -Method "POST" -Endpoint "/api/v1/mosques" -Role "none" -Input "" -Expected 401 -Actual (Invoke-ApiRequest -Method "Post" -Path "/api/v1/mosques" -Body (Test-Body @{ name = "Test" })).Status

Write-Case "Step 12 - PUT /mosques/{id} (SUPER_ADMIN, MOSQUE_ADMIN)"
if ($superMosqueId) {
    $n++
    Add-TestResult -Number $n -Case "Update as SUPER_ADMIN" -Method "PUT" -Endpoint "/api/v1/mosques/{id}" -Role "SUPER_ADMIN" -Input "mosque=$superMosqueId" -Expected 200 -Actual (Invoke-ApiRequest -Method "Put" -Path "/api/v1/mosques/$superMosqueId" -Token $admin.accessToken -Body (Test-Body @{ name = "Super Mosque Renamed" })).Status
    $n++
    Add-TestResult -Number $n -Case "Update as MOSQUE_ADMIN (no assignment)" -Method "PUT" -Endpoint "/api/v1/mosques/{id}" -Role "MOSQUE_ADMIN" -Input "mosque=$superMosqueId" -Expected 403 -Actual (Invoke-ApiRequest -Method "Put" -Path "/api/v1/mosques/$superMosqueId" -Token $mosqueAdmin.accessToken -Body (Test-Body @{ name = "Hax" })).Status
    $n++
    Add-TestResult -Number $n -Case "Update name >200 chars" -Method "PUT" -Endpoint "/api/v1/mosques/{id}" -Role "SUPER_ADMIN" -Input "name=201 chars" -Expected 400 -Actual (Invoke-ApiRequest -Method "Put" -Path "/api/v1/mosques/$superMosqueId" -Token $admin.accessToken -Body (Test-Body @{ name = ("x" * 201) })).Status
} else {
    $n++
    Add-TestResult -Number $n -Case "Update as SUPER_ADMIN" -Method "PUT" -Endpoint "/api/v1/mosques/{id}" -Role "SUPER_ADMIN" -Input "no mosque" -Expected 200 -Actual 0 -Note "SKIPPED"
    $n++
    Add-TestResult -Number $n -Case "Update as MOSQUE_ADMIN (no assignment)" -Method "PUT" -Endpoint "/api/v1/mosques/{id}" -Role "MOSQUE_ADMIN" -Input "no mosque" -Expected 403 -Actual 0 -Note "SKIPPED"
    $n++
    Add-TestResult -Number $n -Case "Update name >200 chars" -Method "PUT" -Endpoint "/api/v1/mosques/{id}" -Role "SUPER_ADMIN" -Input "no mosque" -Expected 400 -Actual 0 -Note "SKIPPED"
}
if ($mosqueId -and $twAdmin) {
    $n++
    Add-TestResult -Number $n -Case "Update own mosque as MOSQUE_ADMIN" -Method "PUT" -Endpoint "/api/v1/mosques/{id}" -Role "MOSQUE_ADMIN" -Input "tw admin" -Expected 200 -Actual (Invoke-ApiRequest -Method "Put" -Path "/api/v1/mosques/$mosqueId" -Token $twAdmin.accessToken -Body (Test-Body @{ name = "Throwaway Mosque Renamed" })).Status
} else {
    $n++
    Add-TestResult -Number $n -Case "Update own mosque as MOSQUE_ADMIN" -Method "PUT" -Endpoint "/api/v1/mosques/{id}" -Role "MOSQUE_ADMIN" -Input "" -Expected 200 -Actual 0 -Note "SKIPPED"
}
$n++
Add-TestResult -Number $n -Case "Update random UUID" -Method "PUT" -Endpoint "/api/v1/mosques/{id}" -Role "SUPER_ADMIN" -Input "random uuid" -Expected 404 -Actual (Invoke-ApiRequest -Method "Put" -Path "/api/v1/mosques/$([guid]::NewGuid())" -Token $admin.accessToken -Body (Test-Body @{ name = "X" })).Status
$n++
Add-TestResult -Number $n -Case "Update without token" -Method "PUT" -Endpoint "/api/v1/mosques/{id}" -Role "none" -Input "" -Expected 401 -Actual (Invoke-ApiRequest -Method "Put" -Path "/api/v1/mosques/$([guid]::NewGuid())" -Body (Test-Body @{ name = "X" })).Status

Write-Case "Step 13 - DELETE /mosques/{id} (SUPER_ADMIN only)"
if ($superMosqueId) {
    $n++
    Add-TestResult -Number $n -Case "Deactivate mosque as SUPER_ADMIN" -Method "DELETE" -Endpoint "/api/v1/mosques/{id}" -Role "SUPER_ADMIN" -Input "mosque=$superMosqueId" -Expected 200 -Actual (Invoke-ApiRequest -Method "Delete" -Path "/api/v1/mosques/$superMosqueId" -Token $admin.accessToken).Status
    $n++
    Add-TestResult -Number $n -Case "Deactivate as MOSQUE_ADMIN" -Method "DELETE" -Endpoint "/api/v1/mosques/{id}" -Role "MOSQUE_ADMIN" -Input "mosque=$superMosqueId" -Expected 403 -Actual (Invoke-ApiRequest -Method "Delete" -Path "/api/v1/mosques/$superMosqueId" -Token $mosqueAdmin.accessToken).Status
} else {
    $n++
    Add-TestResult -Number $n -Case "Deactivate mosque as SUPER_ADMIN" -Method "DELETE" -Endpoint "/api/v1/mosques/{id}" -Role "SUPER_ADMIN" -Input "no mosque" -Expected 200 -Actual 0 -Note "SKIPPED"
    $n++
    Add-TestResult -Number $n -Case "Deactivate as MOSQUE_ADMIN" -Method "DELETE" -Endpoint "/api/v1/mosques/{id}" -Role "MOSQUE_ADMIN" -Input "no mosque" -Expected 403 -Actual 0 -Note "SKIPPED"
}
$n++
Add-TestResult -Number $n -Case "Deactivate random UUID" -Method "DELETE" -Endpoint "/api/v1/mosques/{id}" -Role "SUPER_ADMIN" -Input "random uuid" -Expected 404 -Actual (Invoke-ApiRequest -Method "Delete" -Path "/api/v1/mosques/$([guid]::NewGuid())" -Token $admin.accessToken).Status
$n++
Add-TestResult -Number $n -Case "Deactivate without token" -Method "DELETE" -Endpoint "/api/v1/mosques/{id}" -Role "none" -Input "" -Expected 401 -Actual (Invoke-ApiRequest -Method "Delete" -Path "/api/v1/mosques/$([guid]::NewGuid())").Status

Write-Case "Step 14 - Cleanup"
$deletedOk = $false
if (-not $ReadOnly -and $mosqueId) {
    $del = Invoke-ApiRequest -Method "Delete" -Path "/api/v1/mosques/$mosqueId" -Token $admin.accessToken
    $deletedOk = ($del.Status -eq 200)
    $n++
    Add-TestResult -Number $n -Case "Cleanup: deactivate throwaway mosque" -Method "DELETE" -Endpoint "/api/v1/mosques/{id}" -Role "SUPER_ADMIN" -Input "mosque=$mosqueId" -Expected 200 -Actual $del.Status
} else {
    $n++
    Add-TestResult -Number $n -Case "Cleanup: deactivate throwaway mosque" -Method "DELETE" -Endpoint "/api/v1/mosques/{id}" -Role "SUPER_ADMIN" -Input "no mosque" -Expected 200 -Actual 0 -Note "SKIPPED"
}
if ($deletedOk -and $twStudent) {
    $n++
    Add-TestResult -Number $n -Case "Join request on inactive mosque" -Method "POST" -Endpoint "/api/v1/mosques/join-requests" -Role "STUDENT" -Input "inactive mosque" -Expected 400 -Actual (Invoke-ApiRequest -Method "Post" -Path "/api/v1/mosques/join-requests" -Token $twStudent.accessToken -Body (Test-Body @{ mosqueId = $mosqueId })).Status
} else {
    $n++
    Add-TestResult -Number $n -Case "Join request on inactive mosque" -Method "POST" -Endpoint "/api/v1/mosques/join-requests" -Role "STUDENT" -Input "no mosque" -Expected 400 -Actual 0 -Note "SKIPPED"
}

Show-TestResults
