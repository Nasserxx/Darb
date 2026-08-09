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
$studentInviteCode = $null
$twAdmin = $null
$twStudent = $null
$twStudent2 = $null

if (-not $ReadOnly) {
    $twAdmin = Ensure-User "ST Throwaway Admin" "st.admin$runSuffix@darb.app" "StAdmin123!" "MOSQUE_ADMIN"
    $twStudent = Ensure-User "ST Throwaway Student" "st.student$runSuffix@darb.app" "StStudent123!" "STUDENT"
    $twStudent2 = Ensure-User "ST Throwaway Student2" "st.student2$runSuffix@darb.app" "StStudent123!" "STUDENT"

    if ($twAdmin) {
        $onboard = Invoke-ApiRequest -Method "Post" -Path "/api/v1/mosques/onboard" -Token $twAdmin.accessToken -Body (Test-Body @{
            name = "Throwaway Mosque $runSuffix"; city = "Riyadh"; timezone = "Asia/Riyadh"
        })
        $n++
        Add-TestResult -Number $n -Case "Onboard mosque (throwaway admin)" -Method "POST" -Endpoint "/api/v1/mosques/onboard" -Role "MOSQUE_ADMIN" -Input "name+city" -Expected 201 -Actual $onboard.Status
        if ($onboard.Status -eq 201) {
            $od = ($onboard.Body | ConvertFrom-Json).data
            $mosqueId = $od.mosque.id
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
Add-TestResult -Number $n -Case "GET /students without token" -Method "GET" -Endpoint "/api/v1/students" -Role "none" -Input "" -Expected 401 -Actual (Invoke-ApiRequest -Method "Get" -Path "/api/v1/students").Status
$n++
Add-TestResult -Number $n -Case "GET /students with garbage token" -Method "GET" -Endpoint "/api/v1/students" -Role "garbage" -Input "Bearer xyz" -Expected 401 -Actual (Invoke-ApiRequest -Method "Get" -Path "/api/v1/students" -Token "xyz").Status

Write-Case "Step 2 - GET /students (pagination)"
$n++
Add-TestResult -Number $n -Case "List students as SUPER_ADMIN" -Method "GET" -Endpoint "/api/v1/students" -Role "SUPER_ADMIN" -Input "default page" -Expected 200 -Actual (Invoke-ApiRequest -Method "Get" -Path "/api/v1/students" -Token $admin.accessToken).Status
$n++
Add-TestResult -Number $n -Case "List students paginated" -Method "GET" -Endpoint "/api/v1/students?page=0&size=1" -Role "SUPER_ADMIN" -Input "page=0&size=1" -Expected 200 -Actual (Invoke-ApiRequest -Method "Get" -Path "/api/v1/students" -Token $admin.accessToken -Query @{ page = 0; size = 1 }).Status
if ($mosqueId -and $twAdmin) {
    $n++
    Add-TestResult -Number $n -Case "List students as MOSQUE_ADMIN (scoped)" -Method "GET" -Endpoint "/api/v1/students" -Role "MOSQUE_ADMIN" -Input "tw admin" -Expected 200 -Actual (Invoke-ApiRequest -Method "Get" -Path "/api/v1/students" -Token $twAdmin.accessToken).Status
} else {
    $n++
    Add-TestResult -Number $n -Case "List students as MOSQUE_ADMIN (scoped)" -Method "GET" -Endpoint "/api/v1/students" -Role "MOSQUE_ADMIN" -Input "no mosque" -Expected 200 -Actual 0 -Note "SKIPPED"
}
$n++
Add-TestResult -Number $n -Case "List students seed admin (empty page)" -Method "GET" -Endpoint "/api/v1/students" -Role "MOSQUE_ADMIN" -Input "seed admin" -Expected 200 -Actual (Invoke-ApiRequest -Method "Get" -Path "/api/v1/students" -Token $mosqueAdmin.accessToken).Status
$n++
Add-TestResult -Number $n -Case "List students seed teacher (no profile)" -Method "GET" -Endpoint "/api/v1/students" -Role "TEACHER" -Input "seed teacher" -Expected 200 -Actual (Invoke-ApiRequest -Method "Get" -Path "/api/v1/students" -Token $teacher.accessToken).Status
$n++
Add-TestResult -Number $n -Case "List students as STUDENT" -Method "GET" -Endpoint "/api/v1/students" -Role "STUDENT" -Input "" -Expected 403 -Actual (Invoke-ApiRequest -Method "Get" -Path "/api/v1/students" -Token $student.accessToken).Status
$n++
Add-TestResult -Number $n -Case "List students as PARENT" -Method "GET" -Endpoint "/api/v1/students" -Role "PARENT" -Input "" -Expected 403 -Actual (Invoke-ApiRequest -Method "Get" -Path "/api/v1/students" -Token $parent.accessToken).Status

Write-Case "Step 3 - GET /students/search (MOSQUE_ADMIN)"
if ($mosqueId -and $twAdmin) {
    $n++
    Add-TestResult -Number $n -Case "Search students as MOSQUE_ADMIN" -Method "GET" -Endpoint "/api/v1/students/search" -Role "MOSQUE_ADMIN" -Input "q=Riyadh" -Expected 200 -Actual (Invoke-ApiRequest -Method "Get" -Path "/api/v1/students/search" -Token $twAdmin.accessToken -Query @{ q = "Riyadh" }).Status
} else {
    $n++
    Add-TestResult -Number $n -Case "Search students as MOSQUE_ADMIN" -Method "GET" -Endpoint "/api/v1/students/search" -Role "MOSQUE_ADMIN" -Input "no mosque" -Expected 200 -Actual 0 -Note "SKIPPED"
}
$n++
Add-TestResult -Number $n -Case "Search students seed admin (no assignment)" -Method "GET" -Endpoint "/api/v1/students/search" -Role "MOSQUE_ADMIN" -Input "seed admin" -Expected 403 -Actual (Invoke-ApiRequest -Method "Get" -Path "/api/v1/students/search" -Token $mosqueAdmin.accessToken -Query @{ q = "Riyadh" }).Status
$n++
Add-TestResult -Number $n -Case "Search students missing q" -Method "GET" -Endpoint "/api/v1/students/search" -Role "MOSQUE_ADMIN" -Input "no q" -Expected 400 -Actual (Invoke-ApiRequest -Method "Get" -Path "/api/v1/students/search" -Token $mosqueAdmin.accessToken).Status
$n++
Add-TestResult -Number $n -Case "Search students as SUPER_ADMIN" -Method "GET" -Endpoint "/api/v1/students/search" -Role "SUPER_ADMIN" -Input "q=Riyadh" -Expected 403 -Actual (Invoke-ApiRequest -Method "Get" -Path "/api/v1/students/search" -Token $admin.accessToken -Query @{ q = "Riyadh" }).Status
$n++
Add-TestResult -Number $n -Case "Search students as TEACHER" -Method "GET" -Endpoint "/api/v1/students/search" -Role "TEACHER" -Input "q=Riyadh" -Expected 403 -Actual (Invoke-ApiRequest -Method "Get" -Path "/api/v1/students/search" -Token $teacher.accessToken -Query @{ q = "Riyadh" }).Status
$n++
Add-TestResult -Number $n -Case "Search students as STUDENT" -Method "GET" -Endpoint "/api/v1/students/search" -Role "STUDENT" -Input "q=Riyadh" -Expected 403 -Actual (Invoke-ApiRequest -Method "Get" -Path "/api/v1/students/search" -Token $student.accessToken -Query @{ q = "Riyadh" }).Status
$n++
Add-TestResult -Number $n -Case "Search students without token" -Method "GET" -Endpoint "/api/v1/students/search" -Role "none" -Input "" -Expected 401 -Actual (Invoke-ApiRequest -Method "Get" -Path "/api/v1/students/search").Status

Write-Case "Step 4 - GET /students/{id}"
$studentId = $null
if ($mosqueId -and $twStudent) {
    $onb = Invoke-ApiRequest -Method "Post" -Path "/api/v1/students/onboard" -Token $twStudent.accessToken -Body (Test-Body @{ mosqueId = $mosqueId })
    $n++
    Add-TestResult -Number $n -Case "Onboard throwaway student" -Method "POST" -Endpoint "/api/v1/students/onboard" -Role "STUDENT" -Input "mosque=$mosqueId" -Expected 201 -Actual $onb.Status
    if ($onb.Status -eq 201) { $studentId = ($onb.Body | ConvertFrom-Json).data.id; Write-Host "Student id: $studentId" }
} else {
    $n++
    Add-TestResult -Number $n -Case "Onboard throwaway student" -Method "POST" -Endpoint "/api/v1/students/onboard" -Role "STUDENT" -Input "no mosque" -Expected 201 -Actual 0 -Note "SKIPPED"
}
if ($studentId) {
    $n++
    Add-TestResult -Number $n -Case "Get student as SUPER_ADMIN" -Method "GET" -Endpoint "/api/v1/students/{id}" -Role "SUPER_ADMIN" -Input "student=$studentId" -Expected 200 -Actual (Invoke-ApiRequest -Method "Get" -Path "/api/v1/students/$studentId" -Token $admin.accessToken).Status
    $n++
    Add-TestResult -Number $n -Case "Get student as MOSQUE_ADMIN (same mosque)" -Method "GET" -Endpoint "/api/v1/students/{id}" -Role "MOSQUE_ADMIN" -Input "tw admin" -Expected 200 -Actual (Invoke-ApiRequest -Method "Get" -Path "/api/v1/students/$studentId" -Token $twAdmin.accessToken).Status
    $n++
    Add-TestResult -Number $n -Case "Get student as the student" -Method "GET" -Endpoint "/api/v1/students/{id}" -Role "STUDENT" -Input "tw student" -Expected 200 -Actual (Invoke-ApiRequest -Method "Get" -Path "/api/v1/students/$studentId" -Token $twStudent.accessToken).Status
    $n++
    Add-TestResult -Number $n -Case "Get student as other student" -Method "GET" -Endpoint "/api/v1/students/{id}" -Role "STUDENT" -Input "tw student2" -Expected 403 -Actual (Invoke-ApiRequest -Method "Get" -Path "/api/v1/students/$studentId" -Token $twStudent2.accessToken).Status
    $n++
    Add-TestResult -Number $n -Case "Get student as seed student (no profile)" -Method "GET" -Endpoint "/api/v1/students/{id}" -Role "STUDENT" -Input "seed student" -Expected 403 -Actual (Invoke-ApiRequest -Method "Get" -Path "/api/v1/students/$studentId" -Token $student.accessToken).Status
    $n++
    Add-TestResult -Number $n -Case "Get student as seed MOSQUE_ADMIN (no assignment)" -Method "GET" -Endpoint "/api/v1/students/{id}" -Role "MOSQUE_ADMIN" -Input "seed admin" -Expected 403 -Actual (Invoke-ApiRequest -Method "Get" -Path "/api/v1/students/$studentId" -Token $mosqueAdmin.accessToken).Status
    $n++
    Add-TestResult -Number $n -Case "Get student as seed TEACHER (no profile)" -Method "GET" -Endpoint "/api/v1/students/{id}" -Role "TEACHER" -Input "seed teacher" -Expected 403 -Actual (Invoke-ApiRequest -Method "Get" -Path "/api/v1/students/$studentId" -Token $teacher.accessToken).Status
    $n++
    Add-TestResult -Number $n -Case "Get student as PARENT (no link)" -Method "GET" -Endpoint "/api/v1/students/{id}" -Role "PARENT" -Input "seed parent" -Expected 403 -Actual (Invoke-ApiRequest -Method "Get" -Path "/api/v1/students/$studentId" -Token $parent.accessToken).Status
} else {
    $n++
    Add-TestResult -Number $n -Case "Get student as SUPER_ADMIN" -Method "GET" -Endpoint "/api/v1/students/{id}" -Role "SUPER_ADMIN" -Input "no student" -Expected 200 -Actual 0 -Note "SKIPPED"
    $n++
    Add-TestResult -Number $n -Case "Get student as MOSQUE_ADMIN (same mosque)" -Method "GET" -Endpoint "/api/v1/students/{id}" -Role "MOSQUE_ADMIN" -Input "no student" -Expected 200 -Actual 0 -Note "SKIPPED"
    $n++
    Add-TestResult -Number $n -Case "Get student as the student" -Method "GET" -Endpoint "/api/v1/students/{id}" -Role "STUDENT" -Input "no student" -Expected 200 -Actual 0 -Note "SKIPPED"
    $n++
    Add-TestResult -Number $n -Case "Get student as other student" -Method "GET" -Endpoint "/api/v1/students/{id}" -Role "STUDENT" -Input "no student" -Expected 403 -Actual 0 -Note "SKIPPED"
    $n++
    Add-TestResult -Number $n -Case "Get student as seed student (no profile)" -Method "GET" -Endpoint "/api/v1/students/{id}" -Role "STUDENT" -Input "no student" -Expected 403 -Actual 0 -Note "SKIPPED"
    $n++
    Add-TestResult -Number $n -Case "Get student as seed MOSQUE_ADMIN (no assignment)" -Method "GET" -Endpoint "/api/v1/students/{id}" -Role "MOSQUE_ADMIN" -Input "no student" -Expected 403 -Actual 0 -Note "SKIPPED"
    $n++
    Add-TestResult -Number $n -Case "Get student as seed TEACHER (no profile)" -Method "GET" -Endpoint "/api/v1/students/{id}" -Role "TEACHER" -Input "no student" -Expected 403 -Actual 0 -Note "SKIPPED"
    $n++
    Add-TestResult -Number $n -Case "Get student as PARENT (no link)" -Method "GET" -Endpoint "/api/v1/students/{id}" -Role "PARENT" -Input "no student" -Expected 403 -Actual 0 -Note "SKIPPED"
}
$n++
Add-TestResult -Number $n -Case "Get student invalid UUID" -Method "GET" -Endpoint "/api/v1/students/{id}" -Role "SUPER_ADMIN" -Input "not-a-uuid" -Expected 400 -Actual (Invoke-ApiRequest -Method "Get" -Path "/api/v1/students/not-a-uuid" -Token $admin.accessToken).Status
$n++
Add-TestResult -Number $n -Case "Get student random UUID" -Method "GET" -Endpoint "/api/v1/students/{id}" -Role "SUPER_ADMIN" -Input "random uuid" -Expected 404 -Actual (Invoke-ApiRequest -Method "Get" -Path "/api/v1/students/$([guid]::NewGuid())" -Token $admin.accessToken).Status
$n++
Add-TestResult -Number $n -Case "Get student without token" -Method "GET" -Endpoint "/api/v1/students/{id}" -Role "none" -Input "" -Expected 401 -Actual (Invoke-ApiRequest -Method "Get" -Path "/api/v1/students/$([guid]::NewGuid())").Status

Write-Case "Step 5 - POST /students/onboard (STUDENT)"
if (-not $ReadOnly -and $mosqueId) {
    $n++
    Add-TestResult -Number $n -Case "Onboard as TEACHER" -Method "POST" -Endpoint "/api/v1/students/onboard" -Role "TEACHER" -Input "mosque=$mosqueId" -Expected 403 -Actual (Invoke-ApiRequest -Method "Post" -Path "/api/v1/students/onboard" -Token $teacher.accessToken -Body (Test-Body @{ mosqueId = $mosqueId })).Status
    $n++
    Add-TestResult -Number $n -Case "Onboard as MOSQUE_ADMIN" -Method "POST" -Endpoint "/api/v1/students/onboard" -Role "MOSQUE_ADMIN" -Input "mosque=$mosqueId" -Expected 403 -Actual (Invoke-ApiRequest -Method "Post" -Path "/api/v1/students/onboard" -Token $twAdmin.accessToken -Body (Test-Body @{ mosqueId = $mosqueId })).Status
    $n++
    Add-TestResult -Number $n -Case "Onboard as SUPER_ADMIN" -Method "POST" -Endpoint "/api/v1/students/onboard" -Role "SUPER_ADMIN" -Input "mosque=$mosqueId" -Expected 403 -Actual (Invoke-ApiRequest -Method "Post" -Path "/api/v1/students/onboard" -Token $admin.accessToken -Body (Test-Body @{ mosqueId = $mosqueId })).Status
    $n++
    Add-TestResult -Number $n -Case "Onboard missing mosqueId" -Method "POST" -Endpoint "/api/v1/students/onboard" -Role "STUDENT" -Input "empty body" -Expected 400 -Actual (Invoke-ApiRequest -Method "Post" -Path "/api/v1/students/onboard" -Token $twStudent2.accessToken -Body (Test-Body @{ })).Status
    $n++
    Add-TestResult -Number $n -Case "Onboard unknown mosque" -Method "POST" -Endpoint "/api/v1/students/onboard" -Role "STUDENT" -Input "random mosque uuid" -Expected 404 -Actual (Invoke-ApiRequest -Method "Post" -Path "/api/v1/students/onboard" -Token $twStudent2.accessToken -Body (Test-Body @{ mosqueId = [guid]::NewGuid() })).Status
} else {
    $n++
    Add-TestResult -Number $n -Case "Onboard as TEACHER" -Method "POST" -Endpoint "/api/v1/students/onboard" -Role "TEACHER" -Input "no mosque" -Expected 403 -Actual 0 -Note "SKIPPED"
    $n++
    Add-TestResult -Number $n -Case "Onboard as MOSQUE_ADMIN" -Method "POST" -Endpoint "/api/v1/students/onboard" -Role "MOSQUE_ADMIN" -Input "no mosque" -Expected 403 -Actual 0 -Note "SKIPPED"
    $n++
    Add-TestResult -Number $n -Case "Onboard as SUPER_ADMIN" -Method "POST" -Endpoint "/api/v1/students/onboard" -Role "SUPER_ADMIN" -Input "no mosque" -Expected 403 -Actual 0 -Note "SKIPPED"
    $n++
    Add-TestResult -Number $n -Case "Onboard missing mosqueId" -Method "POST" -Endpoint "/api/v1/students/onboard" -Role "STUDENT" -Input "no mosque" -Expected 400 -Actual 0 -Note "SKIPPED"
    $n++
    Add-TestResult -Number $n -Case "Onboard unknown mosque" -Method "POST" -Endpoint "/api/v1/students/onboard" -Role "STUDENT" -Input "no mosque" -Expected 404 -Actual 0 -Note "SKIPPED"
}
if ($mosqueId -and $studentId) {
    $n++
    Add-TestResult -Number $n -Case "Onboard again (already has profile)" -Method "POST" -Endpoint "/api/v1/students/onboard" -Role "STUDENT" -Input "tw student" -Expected 403 -Actual (Invoke-ApiRequest -Method "Post" -Path "/api/v1/students/onboard" -Token $twStudent.accessToken -Body (Test-Body @{ mosqueId = $mosqueId })).Status
} else {
    $n++
    Add-TestResult -Number $n -Case "Onboard again (already has profile)" -Method "POST" -Endpoint "/api/v1/students/onboard" -Role "STUDENT" -Input "no student" -Expected 403 -Actual 0 -Note "SKIPPED"
}
$n++
Add-TestResult -Number $n -Case "Onboard without token" -Method "POST" -Endpoint "/api/v1/students/onboard" -Role "none" -Input "" -Expected 401 -Actual (Invoke-ApiRequest -Method "Post" -Path "/api/v1/students/onboard" -Body (Test-Body @{ mosqueId = [guid]::NewGuid() })).Status

Write-Case "Step 6 - POST /students/join (STUDENT)"
if (-not $ReadOnly -and $studentInviteCode -and $twStudent2) {
    $n++
    Add-TestResult -Number $n -Case "Join by student invite code" -Method "POST" -Endpoint "/api/v1/students/join" -Role "STUDENT" -Input "tw student2" -Expected 201 -Actual (Invoke-ApiRequest -Method "Post" -Path "/api/v1/students/join" -Token $twStudent2.accessToken -Body (Test-Body @{ inviteCode = $studentInviteCode })).Status
    $n++
    Add-TestResult -Number $n -Case "Join again (already joined)" -Method "POST" -Endpoint "/api/v1/students/join" -Role "STUDENT" -Input "tw student2" -Expected 403 -Actual (Invoke-ApiRequest -Method "Post" -Path "/api/v1/students/join" -Token $twStudent2.accessToken -Body (Test-Body @{ inviteCode = $studentInviteCode })).Status
} else {
    $n++
    Add-TestResult -Number $n -Case "Join by student invite code" -Method "POST" -Endpoint "/api/v1/students/join" -Role "STUDENT" -Input "no code" -Expected 201 -Actual 0 -Note "SKIPPED"
    $n++
    Add-TestResult -Number $n -Case "Join again (already joined)" -Method "POST" -Endpoint "/api/v1/students/join" -Role "STUDENT" -Input "no code" -Expected 403 -Actual 0 -Note "SKIPPED"
}
$n++
Add-TestResult -Number $n -Case "Join invalid code" -Method "POST" -Endpoint "/api/v1/students/join" -Role "STUDENT" -Input "code=bogus" -Expected 404 -Actual (Invoke-ApiRequest -Method "Post" -Path "/api/v1/students/join" -Token $student.accessToken -Body (Test-Body @{ inviteCode = "boguscode" })).Status
$n++
Add-TestResult -Number $n -Case "Join blank code" -Method "POST" -Endpoint "/api/v1/students/join" -Role "STUDENT" -Input "empty body" -Expected 400 -Actual (Invoke-ApiRequest -Method "Post" -Path "/api/v1/students/join" -Token $student.accessToken -Body (Test-Body @{ })).Status
$n++
Add-TestResult -Number $n -Case "Join as TEACHER" -Method "POST" -Endpoint "/api/v1/students/join" -Role "TEACHER" -Input "code=bogus" -Expected 403 -Actual (Invoke-ApiRequest -Method "Post" -Path "/api/v1/students/join" -Token $teacher.accessToken -Body (Test-Body @{ inviteCode = "boguscode" })).Status
$n++
Add-TestResult -Number $n -Case "Join without token" -Method "POST" -Endpoint "/api/v1/students/join" -Role "none" -Input "" -Expected 401 -Actual (Invoke-ApiRequest -Method "Post" -Path "/api/v1/students/join" -Body (Test-Body @{ inviteCode = "boguscode" })).Status

Write-Case "Step 7 - POST /students (SUPER_ADMIN, MOSQUE_ADMIN)"
$createdStudentId = $null
if (-not $ReadOnly -and $mosqueId -and $twStudent) {
    $cr = Invoke-ApiRequest -Method "Post" -Path "/api/v1/students" -Token $admin.accessToken -Body (Test-Body @{ userId = $twStudent.userId; mosqueId = $mosqueId })
    $n++
    Add-TestResult -Number $n -Case "Create student as SUPER_ADMIN" -Method "POST" -Endpoint "/api/v1/students" -Role "SUPER_ADMIN" -Input "twStudent+mosque" -Expected 201 -Actual $cr.Status
    if ($cr.Status -eq 201) { $createdStudentId = ($cr.Body | ConvertFrom-Json).data.id }
} else {
    $n++
    Add-TestResult -Number $n -Case "Create student as SUPER_ADMIN" -Method "POST" -Endpoint "/api/v1/students" -Role "SUPER_ADMIN" -Input "no mosque" -Expected 201 -Actual 0 -Note "SKIPPED"
}
if (-not $ReadOnly -and $mosqueId -and $twStudent -and $twAdmin) {
    $n++
    Add-TestResult -Number $n -Case "Create student as MOSQUE_ADMIN" -Method "POST" -Endpoint "/api/v1/students" -Role "MOSQUE_ADMIN" -Input "twStudent+mosque" -Expected 201 -Actual (Invoke-ApiRequest -Method "Post" -Path "/api/v1/students" -Token $twAdmin.accessToken -Body (Test-Body @{ userId = $twStudent2.userId; mosqueId = $mosqueId })).Status
} else {
    $n++
    Add-TestResult -Number $n -Case "Create student as MOSQUE_ADMIN" -Method "POST" -Endpoint "/api/v1/students" -Role "MOSQUE_ADMIN" -Input "no mosque" -Expected 201 -Actual 0 -Note "SKIPPED"
}
if (-not $ReadOnly -and $mosqueId -and $twStudent) {
    $n++
    Add-TestResult -Number $n -Case "Create student as TEACHER" -Method "POST" -Endpoint "/api/v1/students" -Role "TEACHER" -Input "twStudent+mosque" -Expected 403 -Actual (Invoke-ApiRequest -Method "Post" -Path "/api/v1/students" -Token $teacher.accessToken -Body (Test-Body @{ userId = $twStudent.userId; mosqueId = $mosqueId })).Status
    $n++
    Add-TestResult -Number $n -Case "Create student as STUDENT" -Method "POST" -Endpoint "/api/v1/students" -Role "STUDENT" -Input "twStudent+mosque" -Expected 403 -Actual (Invoke-ApiRequest -Method "Post" -Path "/api/v1/students" -Token $twStudent.accessToken -Body (Test-Body @{ userId = $twStudent.userId; mosqueId = $mosqueId })).Status
    $n++
    Add-TestResult -Number $n -Case "Create student unknown user" -Method "POST" -Endpoint "/api/v1/students" -Role "SUPER_ADMIN" -Input "random user uuid" -Expected 404 -Actual (Invoke-ApiRequest -Method "Post" -Path "/api/v1/students" -Token $admin.accessToken -Body (Test-Body @{ userId = [guid]::NewGuid(); mosqueId = $mosqueId })).Status
    $n++
    Add-TestResult -Number $n -Case "Create student unknown mosque" -Method "POST" -Endpoint "/api/v1/students" -Role "SUPER_ADMIN" -Input "random mosque uuid" -Expected 404 -Actual (Invoke-ApiRequest -Method "Post" -Path "/api/v1/students" -Token $admin.accessToken -Body (Test-Body @{ userId = $twStudent.userId; mosqueId = [guid]::NewGuid() })).Status
    $n++
    Add-TestResult -Number $n -Case "Create student missing userId" -Method "POST" -Endpoint "/api/v1/students" -Role "SUPER_ADMIN" -Input "mosque only" -Expected 400 -Actual (Invoke-ApiRequest -Method "Post" -Path "/api/v1/students" -Token $admin.accessToken -Body (Test-Body @{ mosqueId = $mosqueId })).Status
} else {
    $n++
    Add-TestResult -Number $n -Case "Create student as TEACHER" -Method "POST" -Endpoint "/api/v1/students" -Role "TEACHER" -Input "no mosque" -Expected 403 -Actual 0 -Note "SKIPPED"
    $n++
    Add-TestResult -Number $n -Case "Create student as STUDENT" -Method "POST" -Endpoint "/api/v1/students" -Role "STUDENT" -Input "no mosque" -Expected 403 -Actual 0 -Note "SKIPPED"
    $n++
    Add-TestResult -Number $n -Case "Create student unknown user" -Method "POST" -Endpoint "/api/v1/students" -Role "SUPER_ADMIN" -Input "no mosque" -Expected 404 -Actual 0 -Note "SKIPPED"
    $n++
    Add-TestResult -Number $n -Case "Create student unknown mosque" -Method "POST" -Endpoint "/api/v1/students" -Role "SUPER_ADMIN" -Input "no mosque" -Expected 404 -Actual 0 -Note "SKIPPED"
    $n++
    Add-TestResult -Number $n -Case "Create student missing userId" -Method "POST" -Endpoint "/api/v1/students" -Role "SUPER_ADMIN" -Input "no mosque" -Expected 400 -Actual 0 -Note "SKIPPED"
}
$n++
Add-TestResult -Number $n -Case "Create student without token" -Method "POST" -Endpoint "/api/v1/students" -Role "none" -Input "" -Expected 401 -Actual (Invoke-ApiRequest -Method "Post" -Path "/api/v1/students" -Body (Test-Body @{ userId = [guid]::NewGuid(); mosqueId = [guid]::NewGuid() })).Status

Write-Case "Step 8 - PUT /students/{id} (SUPER_ADMIN, MOSQUE_ADMIN)"
if ($studentId) {
    $n++
    Add-TestResult -Number $n -Case "Update student as SUPER_ADMIN" -Method "PUT" -Endpoint "/api/v1/students/{id}" -Role "SUPER_ADMIN" -Input "student=$studentId" -Expected 200 -Actual (Invoke-ApiRequest -Method "Put" -Path "/api/v1/students/$studentId" -Token $admin.accessToken -Body (Test-Body @{ mosqueId = $mosqueId })).Status
    $n++
    Add-TestResult -Number $n -Case "Update student as MOSQUE_ADMIN (same mosque)" -Method "PUT" -Endpoint "/api/v1/students/{id}" -Role "MOSQUE_ADMIN" -Input "tw admin" -Expected 200 -Actual (Invoke-ApiRequest -Method "Put" -Path "/api/v1/students/$studentId" -Token $twAdmin.accessToken -Body (Test-Body @{ mosqueId = $mosqueId })).Status
    $n++
    Add-TestResult -Number $n -Case "Update student as TEACHER" -Method "PUT" -Endpoint "/api/v1/students/{id}" -Role "TEACHER" -Input "seed teacher" -Expected 403 -Actual (Invoke-ApiRequest -Method "Put" -Path "/api/v1/students/$studentId" -Token $teacher.accessToken -Body (Test-Body @{ mosqueId = $mosqueId })).Status
    $n++
    Add-TestResult -Number $n -Case "Update student as STUDENT (self)" -Method "PUT" -Endpoint "/api/v1/students/{id}" -Role "STUDENT" -Input "tw student" -Expected 403 -Actual (Invoke-ApiRequest -Method "Put" -Path "/api/v1/students/$studentId" -Token $twStudent.accessToken -Body (Test-Body @{ mosqueId = $mosqueId })).Status
    $n++
    Add-TestResult -Number $n -Case "Update student as seed MOSQUE_ADMIN (no assignment)" -Method "PUT" -Endpoint "/api/v1/students/{id}" -Role "MOSQUE_ADMIN" -Input "seed admin" -Expected 403 -Actual (Invoke-ApiRequest -Method "Put" -Path "/api/v1/students/$studentId" -Token $mosqueAdmin.accessToken -Body (Test-Body @{ mosqueId = $mosqueId })).Status
} else {
    $n++
    Add-TestResult -Number $n -Case "Update student as SUPER_ADMIN" -Method "PUT" -Endpoint "/api/v1/students/{id}" -Role "SUPER_ADMIN" -Input "no student" -Expected 200 -Actual 0 -Note "SKIPPED"
    $n++
    Add-TestResult -Number $n -Case "Update student as MOSQUE_ADMIN (same mosque)" -Method "PUT" -Endpoint "/api/v1/students/{id}" -Role "MOSQUE_ADMIN" -Input "no student" -Expected 200 -Actual 0 -Note "SKIPPED"
    $n++
    Add-TestResult -Number $n -Case "Update student as TEACHER" -Method "PUT" -Endpoint "/api/v1/students/{id}" -Role "TEACHER" -Input "no student" -Expected 403 -Actual 0 -Note "SKIPPED"
    $n++
    Add-TestResult -Number $n -Case "Update student as STUDENT (self)" -Method "PUT" -Endpoint "/api/v1/students/{id}" -Role "STUDENT" -Input "no student" -Expected 403 -Actual 0 -Note "SKIPPED"
    $n++
    Add-TestResult -Number $n -Case "Update student as seed MOSQUE_ADMIN (no assignment)" -Method "PUT" -Endpoint "/api/v1/students/{id}" -Role "MOSQUE_ADMIN" -Input "no student" -Expected 403 -Actual 0 -Note "SKIPPED"
}
$n++
Add-TestResult -Number $n -Case "Update student random UUID" -Method "PUT" -Endpoint "/api/v1/students/{id}" -Role "SUPER_ADMIN" -Input "random uuid" -Expected 404 -Actual (Invoke-ApiRequest -Method "Put" -Path "/api/v1/students/$([guid]::NewGuid())" -Token $admin.accessToken -Body (Test-Body @{ mosqueId = [guid]::NewGuid() })).Status
$n++
Add-TestResult -Number $n -Case "Update student without token" -Method "PUT" -Endpoint "/api/v1/students/{id}" -Role "none" -Input "" -Expected 401 -Actual (Invoke-ApiRequest -Method "Put" -Path "/api/v1/students/$([guid]::NewGuid())" -Body (Test-Body @{ mosqueId = [guid]::NewGuid() })).Status

Write-Case "Step 9 - DELETE /students/{id} (SUPER_ADMIN, MOSQUE_ADMIN)"
if ($studentId) {
    $n++
    Add-TestResult -Number $n -Case "Delete student as MOSQUE_ADMIN (same mosque)" -Method "DELETE" -Endpoint "/api/v1/students/{id}" -Role "MOSQUE_ADMIN" -Input "tw admin" -Expected 200 -Actual (Invoke-ApiRequest -Method "Delete" -Path "/api/v1/students/$studentId" -Token $twAdmin.accessToken).Status
    $n++
    Add-TestResult -Number $n -Case "Delete student again (already removed)" -Method "DELETE" -Endpoint "/api/v1/students/{id}" -Role "SUPER_ADMIN" -Input "student=$studentId" -Expected 404 -Actual (Invoke-ApiRequest -Method "Delete" -Path "/api/v1/students/$studentId" -Token $admin.accessToken).Status
} else {
    $n++
    Add-TestResult -Number $n -Case "Delete student as MOSQUE_ADMIN (same mosque)" -Method "DELETE" -Endpoint "/api/v1/students/{id}" -Role "MOSQUE_ADMIN" -Input "no student" -Expected 200 -Actual 0 -Note "SKIPPED"
    $n++
    Add-TestResult -Number $n -Case "Delete student again (already removed)" -Method "DELETE" -Endpoint "/api/v1/students/{id}" -Role "SUPER_ADMIN" -Input "no student" -Expected 404 -Actual 0 -Note "SKIPPED"
}
if ($createdStudentId) {
    $n++
    Add-TestResult -Number $n -Case "Delete student as SUPER_ADMIN" -Method "DELETE" -Endpoint "/api/v1/students/{id}" -Role "SUPER_ADMIN" -Input "student=$createdStudentId" -Expected 200 -Actual (Invoke-ApiRequest -Method "Delete" -Path "/api/v1/students/$createdStudentId" -Token $admin.accessToken).Status
} else {
    $n++
    Add-TestResult -Number $n -Case "Delete student as SUPER_ADMIN" -Method "DELETE" -Endpoint "/api/v1/students/{id}" -Role "SUPER_ADMIN" -Input "no student" -Expected 200 -Actual 0 -Note "SKIPPED"
}
if ($studentId) {
    $n++
    Add-TestResult -Number $n -Case "Delete student as TEACHER" -Method "DELETE" -Endpoint "/api/v1/students/{id}" -Role "TEACHER" -Input "seed teacher" -Expected 403 -Actual (Invoke-ApiRequest -Method "Delete" -Path "/api/v1/students/$studentId" -Token $teacher.accessToken).Status
    $n++
    Add-TestResult -Number $n -Case "Delete student as STUDENT (self)" -Method "DELETE" -Endpoint "/api/v1/students/{id}" -Role "STUDENT" -Input "tw student" -Expected 403 -Actual (Invoke-ApiRequest -Method "Delete" -Path "/api/v1/students/$studentId" -Token $twStudent.accessToken).Status
} else {
    $n++
    Add-TestResult -Number $n -Case "Delete student as TEACHER" -Method "DELETE" -Endpoint "/api/v1/students/{id}" -Role "TEACHER" -Input "no student" -Expected 403 -Actual 0 -Note "SKIPPED"
    $n++
    Add-TestResult -Number $n -Case "Delete student as STUDENT (self)" -Method "DELETE" -Endpoint "/api/v1/students/{id}" -Role "STUDENT" -Input "no student" -Expected 403 -Actual 0 -Note "SKIPPED"
}
$n++
Add-TestResult -Number $n -Case "Delete student random UUID" -Method "DELETE" -Endpoint "/api/v1/students/{id}" -Role "SUPER_ADMIN" -Input "random uuid" -Expected 404 -Actual (Invoke-ApiRequest -Method "Delete" -Path "/api/v1/students/$([guid]::NewGuid())" -Token $admin.accessToken).Status
$n++
Add-TestResult -Number $n -Case "Delete student without token" -Method "DELETE" -Endpoint "/api/v1/students/{id}" -Role "none" -Input "" -Expected 401 -Actual (Invoke-ApiRequest -Method "Delete" -Path "/api/v1/students/$([guid]::NewGuid())").Status

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
