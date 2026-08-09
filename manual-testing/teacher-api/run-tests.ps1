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
$teacherInviteCode = $null
$twAdmin = $null
$twTeacher = $null
$twTeacher2 = $null
$twStudent = $null

if (-not $ReadOnly) {
    $twAdmin = Ensure-User "TE Throwaway Admin" "te.admin$runSuffix@darb.app" "TeAdmin123!" "MOSQUE_ADMIN"
    $twTeacher = Ensure-User "TE Throwaway Teacher" "te.teacher$runSuffix@darb.app" "TeTeacher123!" "TEACHER"
    $twTeacher2 = Ensure-User "TE Throwaway Teacher2" "te.teacher2$runSuffix@darb.app" "TeTeacher123!" "TEACHER"
    $twStudent = Ensure-User "TE Throwaway Student" "te.student$runSuffix@darb.app" "TeStudent123!" "STUDENT"

    if ($twAdmin) {
        $onboard = Invoke-ApiRequest -Method "Post" -Path "/api/v1/mosques/onboard" -Token $twAdmin.accessToken -Body (Test-Body @{
            name = "Throwaway Mosque $runSuffix"; city = "Riyadh"; timezone = "Asia/Riyadh"
        })
        $n++
        Add-TestResult -Number $n -Case "Onboard mosque (throwaway admin)" -Method "POST" -Endpoint "/api/v1/mosques/onboard" -Role "MOSQUE_ADMIN" -Input "name+city" -Expected 201 -Actual $onboard.Status
        if ($onboard.Status -eq 201) {
            $od = ($onboard.Body | ConvertFrom-Json).data
            $mosqueId = $od.mosque.id
            $teacherInviteCode = $od.teacherInviteCode
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
Add-TestResult -Number $n -Case "GET /teachers without token" -Method "GET" -Endpoint "/api/v1/teachers" -Role "none" -Input "" -Expected 401 -Actual (Invoke-ApiRequest -Method "Get" -Path "/api/v1/teachers").Status
$n++
Add-TestResult -Number $n -Case "GET /teachers with garbage token" -Method "GET" -Endpoint "/api/v1/teachers" -Role "garbage" -Input "Bearer xyz" -Expected 401 -Actual (Invoke-ApiRequest -Method "Get" -Path "/api/v1/teachers" -Token "xyz").Status

Write-Case "Step 2 - GET /teachers (pagination)"
$n++
Add-TestResult -Number $n -Case "List teachers as SUPER_ADMIN" -Method "GET" -Endpoint "/api/v1/teachers" -Role "SUPER_ADMIN" -Input "default page" -Expected 200 -Actual (Invoke-ApiRequest -Method "Get" -Path "/api/v1/teachers" -Token $admin.accessToken).Status
$n++
Add-TestResult -Number $n -Case "List teachers paginated" -Method "GET" -Endpoint "/api/v1/teachers?page=0&size=1" -Role "SUPER_ADMIN" -Input "page=0&size=1" -Expected 200 -Actual (Invoke-ApiRequest -Method "Get" -Path "/api/v1/teachers" -Token $admin.accessToken -Query @{ page = 0; size = 1 }).Status
if ($mosqueId -and $twAdmin) {
    $n++
    Add-TestResult -Number $n -Case "List teachers as MOSQUE_ADMIN (scoped)" -Method "GET" -Endpoint "/api/v1/teachers" -Role "MOSQUE_ADMIN" -Input "tw admin" -Expected 200 -Actual (Invoke-ApiRequest -Method "Get" -Path "/api/v1/teachers" -Token $twAdmin.accessToken).Status
} else {
    $n++
    Add-TestResult -Number $n -Case "List teachers as MOSQUE_ADMIN (scoped)" -Method "GET" -Endpoint "/api/v1/teachers" -Role "MOSQUE_ADMIN" -Input "no mosque" -Expected 200 -Actual 0 -Note "SKIPPED"
}
$n++
Add-TestResult -Number $n -Case "List teachers seed admin (empty page)" -Method "GET" -Endpoint "/api/v1/teachers" -Role "MOSQUE_ADMIN" -Input "seed admin" -Expected 200 -Actual (Invoke-ApiRequest -Method "Get" -Path "/api/v1/teachers" -Token $mosqueAdmin.accessToken).Status
$n++
Add-TestResult -Number $n -Case "List teachers seed teacher (no profile)" -Method "GET" -Endpoint "/api/v1/teachers" -Role "TEACHER" -Input "seed teacher" -Expected 200 -Actual (Invoke-ApiRequest -Method "Get" -Path "/api/v1/teachers" -Token $teacher.accessToken).Status
$n++
Add-TestResult -Number $n -Case "List teachers as STUDENT" -Method "GET" -Endpoint "/api/v1/teachers" -Role "STUDENT" -Input "" -Expected 403 -Actual (Invoke-ApiRequest -Method "Get" -Path "/api/v1/teachers" -Token $student.accessToken).Status
$n++
Add-TestResult -Number $n -Case "List teachers as PARENT" -Method "GET" -Endpoint "/api/v1/teachers" -Role "PARENT" -Input "" -Expected 403 -Actual (Invoke-ApiRequest -Method "Get" -Path "/api/v1/teachers" -Token $parent.accessToken).Status

Write-Case "Step 3 - GET /teachers/search (MOSQUE_ADMIN)"
if ($mosqueId -and $twAdmin) {
    $n++
    Add-TestResult -Number $n -Case "Search teachers as MOSQUE_ADMIN" -Method "GET" -Endpoint "/api/v1/teachers/search" -Role "MOSQUE_ADMIN" -Input "q=Riyadh" -Expected 200 -Actual (Invoke-ApiRequest -Method "Get" -Path "/api/v1/teachers/search" -Token $twAdmin.accessToken -Query @{ q = "Riyadh" }).Status
} else {
    $n++
    Add-TestResult -Number $n -Case "Search teachers as MOSQUE_ADMIN" -Method "GET" -Endpoint "/api/v1/teachers/search" -Role "MOSQUE_ADMIN" -Input "no mosque" -Expected 200 -Actual 0 -Note "SKIPPED"
}
$n++
Add-TestResult -Number $n -Case "Search teachers seed admin (no assignment)" -Method "GET" -Endpoint "/api/v1/teachers/search" -Role "MOSQUE_ADMIN" -Input "seed admin" -Expected 403 -Actual (Invoke-ApiRequest -Method "Get" -Path "/api/v1/teachers/search" -Token $mosqueAdmin.accessToken -Query @{ q = "Riyadh" }).Status
$n++
Add-TestResult -Number $n -Case "Search teachers missing q" -Method "GET" -Endpoint "/api/v1/teachers/search" -Role "MOSQUE_ADMIN" -Input "no q" -Expected 400 -Actual (Invoke-ApiRequest -Method "Get" -Path "/api/v1/teachers/search" -Token $mosqueAdmin.accessToken).Status
$n++
Add-TestResult -Number $n -Case "Search teachers as SUPER_ADMIN" -Method "GET" -Endpoint "/api/v1/teachers/search" -Role "SUPER_ADMIN" -Input "q=Riyadh" -Expected 403 -Actual (Invoke-ApiRequest -Method "Get" -Path "/api/v1/teachers/search" -Token $admin.accessToken -Query @{ q = "Riyadh" }).Status
$n++
Add-TestResult -Number $n -Case "Search teachers as TEACHER" -Method "GET" -Endpoint "/api/v1/teachers/search" -Role "TEACHER" -Input "q=Riyadh" -Expected 403 -Actual (Invoke-ApiRequest -Method "Get" -Path "/api/v1/teachers/search" -Token $teacher.accessToken -Query @{ q = "Riyadh" }).Status
$n++
Add-TestResult -Number $n -Case "Search teachers as STUDENT" -Method "GET" -Endpoint "/api/v1/teachers/search" -Role "STUDENT" -Input "q=Riyadh" -Expected 403 -Actual (Invoke-ApiRequest -Method "Get" -Path "/api/v1/teachers/search" -Token $student.accessToken -Query @{ q = "Riyadh" }).Status
$n++
Add-TestResult -Number $n -Case "Search teachers without token" -Method "GET" -Endpoint "/api/v1/teachers/search" -Role "none" -Input "" -Expected 401 -Actual (Invoke-ApiRequest -Method "Get" -Path "/api/v1/teachers/search").Status

Write-Case "Step 4 - GET /teachers/{id}"
$teacherId = $null
if ($mosqueId -and $twTeacher) {
    $onb = Invoke-ApiRequest -Method "Post" -Path "/api/v1/teachers/onboard" -Token $twTeacher.accessToken -Body (Test-Body @{ mosqueId = $mosqueId })
    $n++
    Add-TestResult -Number $n -Case "Onboard throwaway teacher" -Method "POST" -Endpoint "/api/v1/teachers/onboard" -Role "TEACHER" -Input "mosque=$mosqueId" -Expected 201 -Actual $onb.Status
    if ($onb.Status -eq 201) { $teacherId = ($onb.Body | ConvertFrom-Json).data.id; Write-Host "Teacher id: $teacherId" }
} else {
    $n++
    Add-TestResult -Number $n -Case "Onboard throwaway teacher" -Method "POST" -Endpoint "/api/v1/teachers/onboard" -Role "TEACHER" -Input "no mosque" -Expected 201 -Actual 0 -Note "SKIPPED"
}
if ($teacherId) {
    $n++
    Add-TestResult -Number $n -Case "Get teacher as SUPER_ADMIN" -Method "GET" -Endpoint "/api/v1/teachers/{id}" -Role "SUPER_ADMIN" -Input "teacher=$teacherId" -Expected 200 -Actual (Invoke-ApiRequest -Method "Get" -Path "/api/v1/teachers/$teacherId" -Token $admin.accessToken).Status
    $n++
    Add-TestResult -Number $n -Case "Get teacher as MOSQUE_ADMIN (same mosque)" -Method "GET" -Endpoint "/api/v1/teachers/{id}" -Role "MOSQUE_ADMIN" -Input "tw admin" -Expected 200 -Actual (Invoke-ApiRequest -Method "Get" -Path "/api/v1/teachers/$teacherId" -Token $twAdmin.accessToken).Status
    $n++
    Add-TestResult -Number $n -Case "Get teacher as the teacher" -Method "GET" -Endpoint "/api/v1/teachers/{id}" -Role "TEACHER" -Input "tw teacher" -Expected 200 -Actual (Invoke-ApiRequest -Method "Get" -Path "/api/v1/teachers/$teacherId" -Token $twTeacher.accessToken).Status
    $n++
    Add-TestResult -Number $n -Case "Get teacher as other teacher" -Method "GET" -Endpoint "/api/v1/teachers/{id}" -Role "TEACHER" -Input "tw teacher2" -Expected 403 -Actual (Invoke-ApiRequest -Method "Get" -Path "/api/v1/teachers/$teacherId" -Token $twTeacher2.accessToken).Status
    $n++
    Add-TestResult -Number $n -Case "Get teacher as seed teacher (no profile)" -Method "GET" -Endpoint "/api/v1/teachers/{id}" -Role "TEACHER" -Input "seed teacher" -Expected 403 -Actual (Invoke-ApiRequest -Method "Get" -Path "/api/v1/teachers/$teacherId" -Token $teacher.accessToken).Status
    $n++
    Add-TestResult -Number $n -Case "Get teacher as seed MOSQUE_ADMIN (no assignment)" -Method "GET" -Endpoint "/api/v1/teachers/{id}" -Role "MOSQUE_ADMIN" -Input "seed admin" -Expected 403 -Actual (Invoke-ApiRequest -Method "Get" -Path "/api/v1/teachers/$teacherId" -Token $mosqueAdmin.accessToken).Status
    $n++
    Add-TestResult -Number $n -Case "Get teacher as STUDENT" -Method "GET" -Endpoint "/api/v1/teachers/{id}" -Role "STUDENT" -Input "" -Expected 403 -Actual (Invoke-ApiRequest -Method "Get" -Path "/api/v1/teachers/$teacherId" -Token $student.accessToken).Status
} else {
    $n++
    Add-TestResult -Number $n -Case "Get teacher as SUPER_ADMIN" -Method "GET" -Endpoint "/api/v1/teachers/{id}" -Role "SUPER_ADMIN" -Input "no teacher" -Expected 200 -Actual 0 -Note "SKIPPED"
    $n++
    Add-TestResult -Number $n -Case "Get teacher as MOSQUE_ADMIN (same mosque)" -Method "GET" -Endpoint "/api/v1/teachers/{id}" -Role "MOSQUE_ADMIN" -Input "no teacher" -Expected 200 -Actual 0 -Note "SKIPPED"
    $n++
    Add-TestResult -Number $n -Case "Get teacher as the teacher" -Method "GET" -Endpoint "/api/v1/teachers/{id}" -Role "TEACHER" -Input "no teacher" -Expected 200 -Actual 0 -Note "SKIPPED"
    $n++
    Add-TestResult -Number $n -Case "Get teacher as other teacher" -Method "GET" -Endpoint "/api/v1/teachers/{id}" -Role "TEACHER" -Input "no teacher" -Expected 403 -Actual 0 -Note "SKIPPED"
    $n++
    Add-TestResult -Number $n -Case "Get teacher as seed teacher (no profile)" -Method "GET" -Endpoint "/api/v1/teachers/{id}" -Role "TEACHER" -Input "no teacher" -Expected 403 -Actual 0 -Note "SKIPPED"
    $n++
    Add-TestResult -Number $n -Case "Get teacher as seed MOSQUE_ADMIN (no assignment)" -Method "GET" -Endpoint "/api/v1/teachers/{id}" -Role "MOSQUE_ADMIN" -Input "no teacher" -Expected 403 -Actual 0 -Note "SKIPPED"
    $n++
    Add-TestResult -Number $n -Case "Get teacher as STUDENT" -Method "GET" -Endpoint "/api/v1/teachers/{id}" -Role "STUDENT" -Input "no teacher" -Expected 403 -Actual 0 -Note "SKIPPED"
}
$n++
Add-TestResult -Number $n -Case "Get teacher invalid UUID" -Method "GET" -Endpoint "/api/v1/teachers/{id}" -Role "SUPER_ADMIN" -Input "not-a-uuid" -Expected 400 -Actual (Invoke-ApiRequest -Method "Get" -Path "/api/v1/teachers/not-a-uuid" -Token $admin.accessToken).Status
$n++
Add-TestResult -Number $n -Case "Get teacher random UUID" -Method "GET" -Endpoint "/api/v1/teachers/{id}" -Role "SUPER_ADMIN" -Input "random uuid" -Expected 404 -Actual (Invoke-ApiRequest -Method "Get" -Path "/api/v1/teachers/$([guid]::NewGuid())" -Token $admin.accessToken).Status
$n++
Add-TestResult -Number $n -Case "Get teacher without token" -Method "GET" -Endpoint "/api/v1/teachers/{id}" -Role "none" -Input "" -Expected 401 -Actual (Invoke-ApiRequest -Method "Get" -Path "/api/v1/teachers/$([guid]::NewGuid())").Status

Write-Case "Step 5 - POST /teachers/onboard (TEACHER)"
if (-not $ReadOnly -and $mosqueId) {
    $n++
    Add-TestResult -Number $n -Case "Onboard as STUDENT" -Method "POST" -Endpoint "/api/v1/teachers/onboard" -Role "STUDENT" -Input "mosque=$mosqueId" -Expected 403 -Actual (Invoke-ApiRequest -Method "Post" -Path "/api/v1/teachers/onboard" -Token $student.accessToken -Body (Test-Body @{ mosqueId = $mosqueId })).Status
    $n++
    Add-TestResult -Number $n -Case "Onboard as MOSQUE_ADMIN" -Method "POST" -Endpoint "/api/v1/teachers/onboard" -Role "MOSQUE_ADMIN" -Input "mosque=$mosqueId" -Expected 403 -Actual (Invoke-ApiRequest -Method "Post" -Path "/api/v1/teachers/onboard" -Token $twAdmin.accessToken -Body (Test-Body @{ mosqueId = $mosqueId })).Status
    $n++
    Add-TestResult -Number $n -Case "Onboard as SUPER_ADMIN" -Method "POST" -Endpoint "/api/v1/teachers/onboard" -Role "SUPER_ADMIN" -Input "mosque=$mosqueId" -Expected 403 -Actual (Invoke-ApiRequest -Method "Post" -Path "/api/v1/teachers/onboard" -Token $admin.accessToken -Body (Test-Body @{ mosqueId = $mosqueId })).Status
    $n++
    Add-TestResult -Number $n -Case "Onboard missing mosqueId" -Method "POST" -Endpoint "/api/v1/teachers/onboard" -Role "TEACHER" -Input "empty body" -Expected 400 -Actual (Invoke-ApiRequest -Method "Post" -Path "/api/v1/teachers/onboard" -Token $twTeacher2.accessToken -Body (Test-Body @{ })).Status
    $n++
    Add-TestResult -Number $n -Case "Onboard unknown mosque" -Method "POST" -Endpoint "/api/v1/teachers/onboard" -Role "TEACHER" -Input "random mosque uuid" -Expected 404 -Actual (Invoke-ApiRequest -Method "Post" -Path "/api/v1/teachers/onboard" -Token $twTeacher2.accessToken -Body (Test-Body @{ mosqueId = [guid]::NewGuid() })).Status
} else {
    $n++
    Add-TestResult -Number $n -Case "Onboard as STUDENT" -Method "POST" -Endpoint "/api/v1/teachers/onboard" -Role "STUDENT" -Input "no mosque" -Expected 403 -Actual 0 -Note "SKIPPED"
    $n++
    Add-TestResult -Number $n -Case "Onboard as MOSQUE_ADMIN" -Method "POST" -Endpoint "/api/v1/teachers/onboard" -Role "MOSQUE_ADMIN" -Input "no mosque" -Expected 403 -Actual 0 -Note "SKIPPED"
    $n++
    Add-TestResult -Number $n -Case "Onboard as SUPER_ADMIN" -Method "POST" -Endpoint "/api/v1/teachers/onboard" -Role "SUPER_ADMIN" -Input "no mosque" -Expected 403 -Actual 0 -Note "SKIPPED"
    $n++
    Add-TestResult -Number $n -Case "Onboard missing mosqueId" -Method "POST" -Endpoint "/api/v1/teachers/onboard" -Role "TEACHER" -Input "no mosque" -Expected 400 -Actual 0 -Note "SKIPPED"
    $n++
    Add-TestResult -Number $n -Case "Onboard unknown mosque" -Method "POST" -Endpoint "/api/v1/teachers/onboard" -Role "TEACHER" -Input "no mosque" -Expected 404 -Actual 0 -Note "SKIPPED"
}
if ($mosqueId -and $teacherId) {
    $n++
    Add-TestResult -Number $n -Case "Onboard again (already has profile)" -Method "POST" -Endpoint "/api/v1/teachers/onboard" -Role "TEACHER" -Input "tw teacher" -Expected 403 -Actual (Invoke-ApiRequest -Method "Post" -Path "/api/v1/teachers/onboard" -Token $twTeacher.accessToken -Body (Test-Body @{ mosqueId = $mosqueId })).Status
} else {
    $n++
    Add-TestResult -Number $n -Case "Onboard again (already has profile)" -Method "POST" -Endpoint "/api/v1/teachers/onboard" -Role "TEACHER" -Input "no teacher" -Expected 403 -Actual 0 -Note "SKIPPED"
}
$n++
Add-TestResult -Number $n -Case "Onboard without token" -Method "POST" -Endpoint "/api/v1/teachers/onboard" -Role "none" -Input "" -Expected 401 -Actual (Invoke-ApiRequest -Method "Post" -Path "/api/v1/teachers/onboard" -Body (Test-Body @{ mosqueId = [guid]::NewGuid() })).Status

Write-Case "Step 6 - POST /teachers/join (TEACHER)"
if (-not $ReadOnly -and $teacherInviteCode -and $twTeacher2) {
    $n++
    Add-TestResult -Number $n -Case "Join by teacher invite code" -Method "POST" -Endpoint "/api/v1/teachers/join" -Role "TEACHER" -Input "tw teacher2" -Expected 201 -Actual (Invoke-ApiRequest -Method "Post" -Path "/api/v1/teachers/join" -Token $twTeacher2.accessToken -Body (Test-Body @{ inviteCode = $teacherInviteCode })).Status
    $n++
    Add-TestResult -Number $n -Case "Join again (already joined)" -Method "POST" -Endpoint "/api/v1/teachers/join" -Role "TEACHER" -Input "tw teacher2" -Expected 403 -Actual (Invoke-ApiRequest -Method "Post" -Path "/api/v1/teachers/join" -Token $twTeacher2.accessToken -Body (Test-Body @{ inviteCode = $teacherInviteCode })).Status
} else {
    $n++
    Add-TestResult -Number $n -Case "Join by teacher invite code" -Method "POST" -Endpoint "/api/v1/teachers/join" -Role "TEACHER" -Input "no code" -Expected 201 -Actual 0 -Note "SKIPPED"
    $n++
    Add-TestResult -Number $n -Case "Join again (already joined)" -Method "POST" -Endpoint "/api/v1/teachers/join" -Role "TEACHER" -Input "no code" -Expected 403 -Actual 0 -Note "SKIPPED"
}
$n++
Add-TestResult -Number $n -Case "Join invalid code" -Method "POST" -Endpoint "/api/v1/teachers/join" -Role "TEACHER" -Input "code=bogus" -Expected 404 -Actual (Invoke-ApiRequest -Method "Post" -Path "/api/v1/teachers/join" -Token $teacher.accessToken -Body (Test-Body @{ inviteCode = "boguscode" })).Status
$n++
Add-TestResult -Number $n -Case "Join blank code" -Method "POST" -Endpoint "/api/v1/teachers/join" -Role "TEACHER" -Input "empty body" -Expected 400 -Actual (Invoke-ApiRequest -Method "Post" -Path "/api/v1/teachers/join" -Token $teacher.accessToken -Body (Test-Body @{ })).Status
$n++
Add-TestResult -Number $n -Case "Join as STUDENT" -Method "POST" -Endpoint "/api/v1/teachers/join" -Role "STUDENT" -Input "code=bogus" -Expected 403 -Actual (Invoke-ApiRequest -Method "Post" -Path "/api/v1/teachers/join" -Token $student.accessToken -Body (Test-Body @{ inviteCode = "boguscode" })).Status
$n++
Add-TestResult -Number $n -Case "Join without token" -Method "POST" -Endpoint "/api/v1/teachers/join" -Role "none" -Input "" -Expected 401 -Actual (Invoke-ApiRequest -Method "Post" -Path "/api/v1/teachers/join" -Body (Test-Body @{ inviteCode = "boguscode" })).Status

Write-Case "Step 7 - POST /teachers (SUPER_ADMIN, MOSQUE_ADMIN)"
$createdTeacherId = $null
if (-not $ReadOnly -and $mosqueId -and $twTeacher) {
    $cr = Invoke-ApiRequest -Method "Post" -Path "/api/v1/teachers" -Token $admin.accessToken -Body (Test-Body @{ userId = $twTeacher.userId; mosqueId = $mosqueId })
    $n++
    Add-TestResult -Number $n -Case "Create teacher as SUPER_ADMIN" -Method "POST" -Endpoint "/api/v1/teachers" -Role "SUPER_ADMIN" -Input "twTeacher+mosque" -Expected 201 -Actual $cr.Status
    if ($cr.Status -eq 201) { $createdTeacherId = ($cr.Body | ConvertFrom-Json).data.id }
} else {
    $n++
    Add-TestResult -Number $n -Case "Create teacher as SUPER_ADMIN" -Method "POST" -Endpoint "/api/v1/teachers" -Role "SUPER_ADMIN" -Input "no mosque" -Expected 201 -Actual 0 -Note "SKIPPED"
}
if (-not $ReadOnly -and $mosqueId -and $twTeacher -and $twAdmin) {
    $n++
    Add-TestResult -Number $n -Case "Create teacher as MOSQUE_ADMIN" -Method "POST" -Endpoint "/api/v1/teachers" -Role "MOSQUE_ADMIN" -Input "twTeacher+mosque" -Expected 201 -Actual (Invoke-ApiRequest -Method "Post" -Path "/api/v1/teachers" -Token $twAdmin.accessToken -Body (Test-Body @{ userId = $twTeacher2.userId; mosqueId = $mosqueId })).Status
} else {
    $n++
    Add-TestResult -Number $n -Case "Create teacher as MOSQUE_ADMIN" -Method "POST" -Endpoint "/api/v1/teachers" -Role "MOSQUE_ADMIN" -Input "no mosque" -Expected 201 -Actual 0 -Note "SKIPPED"
}
if (-not $ReadOnly -and $mosqueId -and $twTeacher) {
    $n++
    Add-TestResult -Number $n -Case "Create teacher as TEACHER" -Method "POST" -Endpoint "/api/v1/teachers" -Role "TEACHER" -Input "twTeacher+mosque" -Expected 403 -Actual (Invoke-ApiRequest -Method "Post" -Path "/api/v1/teachers" -Token $twTeacher.accessToken -Body (Test-Body @{ userId = $twTeacher.userId; mosqueId = $mosqueId })).Status
    $n++
    Add-TestResult -Number $n -Case "Create teacher unknown user" -Method "POST" -Endpoint "/api/v1/teachers" -Role "SUPER_ADMIN" -Input "random user uuid" -Expected 404 -Actual (Invoke-ApiRequest -Method "Post" -Path "/api/v1/teachers" -Token $admin.accessToken -Body (Test-Body @{ userId = [guid]::NewGuid(); mosqueId = $mosqueId })).Status
    $n++
    Add-TestResult -Number $n -Case "Create teacher unknown mosque" -Method "POST" -Endpoint "/api/v1/teachers" -Role "SUPER_ADMIN" -Input "random mosque uuid" -Expected 404 -Actual (Invoke-ApiRequest -Method "Post" -Path "/api/v1/teachers" -Token $admin.accessToken -Body (Test-Body @{ userId = $twTeacher.userId; mosqueId = [guid]::NewGuid() })).Status
    $n++
    Add-TestResult -Number $n -Case "Create teacher missing userId" -Method "POST" -Endpoint "/api/v1/teachers" -Role "SUPER_ADMIN" -Input "mosque only" -Expected 400 -Actual (Invoke-ApiRequest -Method "Post" -Path "/api/v1/teachers" -Token $admin.accessToken -Body (Test-Body @{ mosqueId = $mosqueId })).Status
} else {
    $n++
    Add-TestResult -Number $n -Case "Create teacher as TEACHER" -Method "POST" -Endpoint "/api/v1/teachers" -Role "TEACHER" -Input "no mosque" -Expected 403 -Actual 0 -Note "SKIPPED"
    $n++
    Add-TestResult -Number $n -Case "Create teacher unknown user" -Method "POST" -Endpoint "/api/v1/teachers" -Role "SUPER_ADMIN" -Input "no mosque" -Expected 404 -Actual 0 -Note "SKIPPED"
    $n++
    Add-TestResult -Number $n -Case "Create teacher unknown mosque" -Method "POST" -Endpoint "/api/v1/teachers" -Role "SUPER_ADMIN" -Input "no mosque" -Expected 404 -Actual 0 -Note "SKIPPED"
    $n++
    Add-TestResult -Number $n -Case "Create teacher missing userId" -Method "POST" -Endpoint "/api/v1/teachers" -Role "SUPER_ADMIN" -Input "no mosque" -Expected 400 -Actual 0 -Note "SKIPPED"
}
$n++
Add-TestResult -Number $n -Case "Create teacher without token" -Method "POST" -Endpoint "/api/v1/teachers" -Role "none" -Input "" -Expected 401 -Actual (Invoke-ApiRequest -Method "Post" -Path "/api/v1/teachers" -Body (Test-Body @{ userId = [guid]::NewGuid(); mosqueId = [guid]::NewGuid() })).Status

Write-Case "Step 8 - PUT /teachers/{id} (SUPER_ADMIN, MOSQUE_ADMIN)"
if ($teacherId) {
    $n++
    Add-TestResult -Number $n -Case "Update teacher as SUPER_ADMIN" -Method "PUT" -Endpoint "/api/v1/teachers/{id}" -Role "SUPER_ADMIN" -Input "teacher=$teacherId" -Expected 200 -Actual (Invoke-ApiRequest -Method "Put" -Path "/api/v1/teachers/$teacherId" -Token $admin.accessToken -Body (Test-Body @{ mosqueId = $mosqueId })).Status
    $n++
    Add-TestResult -Number $n -Case "Update teacher as MOSQUE_ADMIN (same mosque)" -Method "PUT" -Endpoint "/api/v1/teachers/{id}" -Role "MOSQUE_ADMIN" -Input "tw admin" -Expected 200 -Actual (Invoke-ApiRequest -Method "Put" -Path "/api/v1/teachers/$teacherId" -Token $twAdmin.accessToken -Body (Test-Body @{ mosqueId = $mosqueId })).Status
    $n++
    Add-TestResult -Number $n -Case "Update teacher as TEACHER" -Method "PUT" -Endpoint "/api/v1/teachers/{id}" -Role "TEACHER" -Input "tw teacher" -Expected 403 -Actual (Invoke-ApiRequest -Method "Put" -Path "/api/v1/teachers/$teacherId" -Token $twTeacher.accessToken -Body (Test-Body @{ mosqueId = $mosqueId })).Status
    $n++
    Add-TestResult -Number $n -Case "Update teacher as seed MOSQUE_ADMIN (no assignment)" -Method "PUT" -Endpoint "/api/v1/teachers/{id}" -Role "MOSQUE_ADMIN" -Input "seed admin" -Expected 403 -Actual (Invoke-ApiRequest -Method "Put" -Path "/api/v1/teachers/$teacherId" -Token $mosqueAdmin.accessToken -Body (Test-Body @{ mosqueId = $mosqueId })).Status
} else {
    $n++
    Add-TestResult -Number $n -Case "Update teacher as SUPER_ADMIN" -Method "PUT" -Endpoint "/api/v1/teachers/{id}" -Role "SUPER_ADMIN" -Input "no teacher" -Expected 200 -Actual 0 -Note "SKIPPED"
    $n++
    Add-TestResult -Number $n -Case "Update teacher as MOSQUE_ADMIN (same mosque)" -Method "PUT" -Endpoint "/api/v1/teachers/{id}" -Role "MOSQUE_ADMIN" -Input "no teacher" -Expected 200 -Actual 0 -Note "SKIPPED"
    $n++
    Add-TestResult -Number $n -Case "Update teacher as TEACHER" -Method "PUT" -Endpoint "/api/v1/teachers/{id}" -Role "TEACHER" -Input "no teacher" -Expected 403 -Actual 0 -Note "SKIPPED"
    $n++
    Add-TestResult -Number $n -Case "Update teacher as seed MOSQUE_ADMIN (no assignment)" -Method "PUT" -Endpoint "/api/v1/teachers/{id}" -Role "MOSQUE_ADMIN" -Input "no teacher" -Expected 403 -Actual 0 -Note "SKIPPED"
}
$n++
Add-TestResult -Number $n -Case "Update teacher random UUID" -Method "PUT" -Endpoint "/api/v1/teachers/{id}" -Role "SUPER_ADMIN" -Input "random uuid" -Expected 404 -Actual (Invoke-ApiRequest -Method "Put" -Path "/api/v1/teachers/$([guid]::NewGuid())" -Token $admin.accessToken -Body (Test-Body @{ mosqueId = [guid]::NewGuid() })).Status
$n++
Add-TestResult -Number $n -Case "Update teacher without token" -Method "PUT" -Endpoint "/api/v1/teachers/{id}" -Role "none" -Input "" -Expected 401 -Actual (Invoke-ApiRequest -Method "Put" -Path "/api/v1/teachers/$([guid]::NewGuid())" -Body (Test-Body @{ mosqueId = [guid]::NewGuid() })).Status

Write-Case "Step 9 - DELETE /teachers/{id} (SUPER_ADMIN, MOSQUE_ADMIN)"
if ($teacherId) {
    $n++
    Add-TestResult -Number $n -Case "Delete teacher as MOSQUE_ADMIN (same mosque)" -Method "DELETE" -Endpoint "/api/v1/teachers/{id}" -Role "MOSQUE_ADMIN" -Input "tw admin" -Expected 200 -Actual (Invoke-ApiRequest -Method "Delete" -Path "/api/v1/teachers/$teacherId" -Token $twAdmin.accessToken).Status
    $n++
    Add-TestResult -Number $n -Case "Delete teacher again (already removed)" -Method "DELETE" -Endpoint "/api/v1/teachers/{id}" -Role "SUPER_ADMIN" -Input "teacher=$teacherId" -Expected 404 -Actual (Invoke-ApiRequest -Method "Delete" -Path "/api/v1/teachers/$teacherId" -Token $admin.accessToken).Status
} else {
    $n++
    Add-TestResult -Number $n -Case "Delete teacher as MOSQUE_ADMIN (same mosque)" -Method "DELETE" -Endpoint "/api/v1/teachers/{id}" -Role "MOSQUE_ADMIN" -Input "no teacher" -Expected 200 -Actual 0 -Note "SKIPPED"
    $n++
    Add-TestResult -Number $n -Case "Delete teacher again (already removed)" -Method "DELETE" -Endpoint "/api/v1/teachers/{id}" -Role "SUPER_ADMIN" -Input "no teacher" -Expected 404 -Actual 0 -Note "SKIPPED"
}
if ($createdTeacherId) {
    $n++
    Add-TestResult -Number $n -Case "Delete teacher as SUPER_ADMIN" -Method "DELETE" -Endpoint "/api/v1/teachers/{id}" -Role "SUPER_ADMIN" -Input "teacher=$createdTeacherId" -Expected 200 -Actual (Invoke-ApiRequest -Method "Delete" -Path "/api/v1/teachers/$createdTeacherId" -Token $admin.accessToken).Status
} else {
    $n++
    Add-TestResult -Number $n -Case "Delete teacher as SUPER_ADMIN" -Method "DELETE" -Endpoint "/api/v1/teachers/{id}" -Role "SUPER_ADMIN" -Input "no teacher" -Expected 200 -Actual 0 -Note "SKIPPED"
}
if ($teacherId) {
    $n++
    Add-TestResult -Number $n -Case "Delete teacher as TEACHER (non-self)" -Method "DELETE" -Endpoint "/api/v1/teachers/{id}" -Role "TEACHER" -Input "tw teacher" -Expected 403 -Actual (Invoke-ApiRequest -Method "Delete" -Path "/api/v1/teachers/$teacherId" -Token $twTeacher.accessToken).Status
} else {
    $n++
    Add-TestResult -Number $n -Case "Delete teacher as TEACHER (non-self)" -Method "DELETE" -Endpoint "/api/v1/teachers/{id}" -Role "TEACHER" -Input "no teacher" -Expected 403 -Actual 0 -Note "SKIPPED"
}
$n++
Add-TestResult -Number $n -Case "Delete teacher random UUID" -Method "DELETE" -Endpoint "/api/v1/teachers/{id}" -Role "SUPER_ADMIN" -Input "random uuid" -Expected 404 -Actual (Invoke-ApiRequest -Method "Delete" -Path "/api/v1/teachers/$([guid]::NewGuid())" -Token $admin.accessToken).Status
$n++
Add-TestResult -Number $n -Case "Delete teacher without token" -Method "DELETE" -Endpoint "/api/v1/teachers/{id}" -Role "none" -Input "" -Expected 401 -Actual (Invoke-ApiRequest -Method "Delete" -Path "/api/v1/teachers/$([guid]::NewGuid())").Status

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
