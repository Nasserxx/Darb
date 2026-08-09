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

# Resolve existing domain ids. Seed users have no mosque/student/circle/enrollment
# profiles, so these resolve only if prior manual testing created the data.
$anyStudentId = $null
$anyCircleId = $null
$activeEnrollmentId = $null
$activeEnrollmentCircleId = $null
$activeEnrollmentStudentId = $null
$attendanceId = $null
$parentChildId = $null
$selfStudentId = $null
$childAttendanceId = $null
$selfAttendanceId = $null
$createdId = $null

$res = Invoke-ApiRequest -Method "Get" -Path "/api/v1/students" -Token $admin.accessToken
if ($res.Status -eq 200) {
    $items = @(($res.Body | ConvertFrom-Json).data.content)
    if ($items.Count -gt 0) { $anyStudentId = $items[0].id }
    foreach ($s in $items) {
        if ($s.userId -eq $student.userId) { $selfStudentId = $s.id }
    }
}
$res = Invoke-ApiRequest -Method "Get" -Path "/api/v1/circles" -Token $admin.accessToken
if ($res.Status -eq 200) {
    $items = @(($res.Body | ConvertFrom-Json).data.content)
    if ($items.Count -gt 0) { $anyCircleId = $items[0].id }
}
$res = Invoke-ApiRequest -Method "Get" -Path "/api/v1/enrollments" -Token $admin.accessToken
if ($res.Status -eq 200) {
    $items = @(($res.Body | ConvertFrom-Json).data.content)
    foreach ($enr in $items) {
        if ($enr.status -eq "ACTIVE") {
            $activeEnrollmentId = $enr.id
            $activeEnrollmentCircleId = $enr.circleId
            $activeEnrollmentStudentId = $enr.studentId
            break
        }
    }
}
$res = Invoke-ApiRequest -Method "Get" -Path "/api/v1/attendance" -Token $admin.accessToken
if ($res.Status -eq 200) {
    $items = @(($res.Body | ConvertFrom-Json).data.content)
    if ($items.Count -gt 0) { $attendanceId = $items[0].id }
}
$res = Invoke-ApiRequest -Method "Get" -Path "/api/v1/parent-students/my-children" -Token $parent.accessToken
if ($res.Status -eq 200) {
    $kids = @(($res.Body | ConvertFrom-Json).data)
    if ($kids.Count -gt 0) { $parentChildId = $kids[0].id }
}
if ($parentChildId) {
    $res = Invoke-ApiRequest -Method "Get" -Path "/api/v1/attendance/student/$parentChildId" -Token $parent.accessToken
    if ($res.Status -eq 200) {
        $items = @(($res.Body | ConvertFrom-Json).data.content)
        if ($items.Count -gt 0) { $childAttendanceId = $items[0].id }
    }
}
if ($selfStudentId) {
    $res = Invoke-ApiRequest -Method "Get" -Path "/api/v1/attendance/student/$selfStudentId" -Token $student.accessToken
    if ($res.Status -eq 200) {
        $items = @(($res.Body | ConvertFrom-Json).data.content)
        if ($items.Count -gt 0) { $selfAttendanceId = $items[0].id }
    }
}

Write-Host "Resolved: student=$anyStudentId circle=$anyCircleId activeEnrollment=$activeEnrollmentId attendance=$attendanceId parentChild=$parentChildId"

Write-Case "Step 1 - Auth basics"
$n++
Add-TestResult -Number $n -Case "GET /attendance without token" -Method "GET" -Endpoint "/api/v1/attendance" -Role "none" -Input "" -Expected 401 -Actual (Invoke-ApiRequest -Method "Get" -Path "/api/v1/attendance").Status
$n++
Add-TestResult -Number $n -Case "GET /attendance garbage token" -Method "GET" -Endpoint "/api/v1/attendance" -Role "garbage" -Input "Bearer xyz" -Expected 401 -Actual (Invoke-ApiRequest -Method "Get" -Path "/api/v1/attendance" -Token "xyz").Status

Write-Case "Step 2 - GET /attendance (list)"
$n++
Add-TestResult -Number $n -Case "List attendance" -Method "GET" -Endpoint "/api/v1/attendance" -Role "SUPER_ADMIN" -Input "" -Expected 200 -Actual (Invoke-ApiRequest -Method "Get" -Path "/api/v1/attendance" -Token $admin.accessToken).Status
$n++
Add-TestResult -Number $n -Case "List attendance (scoped)" -Method "GET" -Endpoint "/api/v1/attendance" -Role "MOSQUE_ADMIN" -Input "" -Expected 200 -Actual (Invoke-ApiRequest -Method "Get" -Path "/api/v1/attendance" -Token $mosqueAdmin.accessToken).Status
$n++
Add-TestResult -Number $n -Case "List attendance (scoped)" -Method "GET" -Endpoint "/api/v1/attendance" -Role "TEACHER" -Input "" -Expected 200 -Actual (Invoke-ApiRequest -Method "Get" -Path "/api/v1/attendance" -Token $teacher.accessToken).Status
$n++
Add-TestResult -Number $n -Case "List attendance as STUDENT" -Method "GET" -Endpoint "/api/v1/attendance" -Role "STUDENT" -Input "" -Expected 403 -Actual (Invoke-ApiRequest -Method "Get" -Path "/api/v1/attendance" -Token $student.accessToken).Status
$n++
Add-TestResult -Number $n -Case "List attendance as PARENT" -Method "GET" -Endpoint "/api/v1/attendance" -Role "PARENT" -Input "" -Expected 403 -Actual (Invoke-ApiRequest -Method "Get" -Path "/api/v1/attendance" -Token $parent.accessToken).Status

Write-Case "Step 3 - GET /attendance/circle/{circleId}"
$circleCasesReady = [bool]$anyCircleId
$circleOkNote = ""
if (-not $circleCasesReady) { $circleOkNote = "SKIPPED" }
if ($circleCasesReady) {
    $circleActual = (Invoke-ApiRequest -Method "Get" -Path "/api/v1/attendance/circle/$anyCircleId" -Token $admin.accessToken).Status
} else { $circleActual = 0 }
$n++
Add-TestResult -Number $n -Case "Circle attendance as SUPER_ADMIN" -Method "GET" -Endpoint "/api/v1/attendance/circle/{circleId}" -Role "SUPER_ADMIN" -Input "circle=$anyCircleId" -Expected 200 -Actual $circleActual -Note $circleOkNote
if ($circleCasesReady) {
    $circleStudentActual = (Invoke-ApiRequest -Method "Get" -Path "/api/v1/attendance/circle/$anyCircleId" -Token $student.accessToken).Status
} else { $circleStudentActual = 0 }
$n++
Add-TestResult -Number $n -Case "Circle attendance as STUDENT (no mosque)" -Method "GET" -Endpoint "/api/v1/attendance/circle/{circleId}" -Role "STUDENT" -Input "circle=$anyCircleId" -Expected 403 -Actual $circleStudentActual -Note $circleOkNote
$n++
Add-TestResult -Number $n -Case "Circle attendance random circle" -Method "GET" -Endpoint "/api/v1/attendance/circle/{circleId}" -Role "SUPER_ADMIN" -Input "random uuid" -Expected 404 -Actual (Invoke-ApiRequest -Method "Get" -Path "/api/v1/attendance/circle/$([guid]::NewGuid())" -Token $admin.accessToken).Status
$n++
Add-TestResult -Number $n -Case "Circle attendance invalid uuid" -Method "GET" -Endpoint "/api/v1/attendance/circle/{circleId}" -Role "SUPER_ADMIN" -Input "not-a-uuid" -Expected 400 -Actual (Invoke-ApiRequest -Method "Get" -Path "/api/v1/attendance/circle/not-a-uuid" -Token $admin.accessToken).Status

Write-Case "Step 4 - GET /attendance/student/{studentId}"
$studentCasesReady = [bool]$anyStudentId
$studentOkNote = ""
if (-not $studentCasesReady) { $studentOkNote = "SKIPPED" }
if ($studentCasesReady) {
    $stActual = (Invoke-ApiRequest -Method "Get" -Path "/api/v1/attendance/student/$anyStudentId" -Token $admin.accessToken).Status
} else { $stActual = 0 }
$n++
Add-TestResult -Number $n -Case "Student attendance as SUPER_ADMIN" -Method "GET" -Endpoint "/api/v1/attendance/student/{studentId}" -Role "SUPER_ADMIN" -Input "student=$anyStudentId" -Expected 200 -Actual $stActual -Note $studentOkNote
if ($studentCasesReady) {
    $stParentActual = (Invoke-ApiRequest -Method "Get" -Path "/api/v1/attendance/student/$anyStudentId" -Token $parent.accessToken).Status
} else { $stParentActual = 0 }
$n++
Add-TestResult -Number $n -Case "Student attendance as PARENT (no link)" -Method "GET" -Endpoint "/api/v1/attendance/student/{studentId}" -Role "PARENT" -Input "student=$anyStudentId" -Expected 403 -Actual $stParentActual -Note $studentOkNote
$n++
Add-TestResult -Number $n -Case "Student attendance random student" -Method "GET" -Endpoint "/api/v1/attendance/student/{studentId}" -Role "SUPER_ADMIN" -Input "random uuid" -Expected 404 -Actual (Invoke-ApiRequest -Method "Get" -Path "/api/v1/attendance/student/$([guid]::NewGuid())" -Token $admin.accessToken).Status
$n++
Add-TestResult -Number $n -Case "Student attendance invalid uuid" -Method "GET" -Endpoint "/api/v1/attendance/student/{studentId}" -Role "SUPER_ADMIN" -Input "not-a-uuid" -Expected 400 -Actual (Invoke-ApiRequest -Method "Get" -Path "/api/v1/attendance/student/not-a-uuid" -Token $admin.accessToken).Status

Write-Case "Step 5 - POST /attendance (Mark: SUPER_ADMIN/MOSQUE_ADMIN/TEACHER)"
$createReady = (-not $ReadOnly) -and $activeEnrollmentId -and $activeEnrollmentCircleId
if ($createReady) {
    $resp = Invoke-ApiRequest -Method "Post" -Path "/api/v1/attendance" -Token $admin.accessToken -Body (Test-Body @{
        enrollmentId = $activeEnrollmentId; circleId = $activeEnrollmentCircleId;
        sessionDate = "2026-08-09"; status = "PRESENT"; scheduledStart = "16:00"
    })
    $createActual = $resp.Status
    if ($createActual -eq 201) {
        $createdId = ($resp.Body | ConvertFrom-Json).data.id
    }
} else { $createActual = 0 }
$createNote = ""
if (-not $createReady) { $createNote = "SKIPPED" }
$n++
Add-TestResult -Number $n -Case "Mark attendance as SUPER_ADMIN" -Method "POST" -Endpoint "/api/v1/attendance" -Role "SUPER_ADMIN" -Input "active enrollment" -Expected 201 -Actual $createActual -Note $createNote

if ($createReady) {
    $maCreateActual = (Invoke-ApiRequest -Method "Post" -Path "/api/v1/attendance" -Token $mosqueAdmin.accessToken -Body (Test-Body @{
        enrollmentId = $activeEnrollmentId; circleId = $activeEnrollmentCircleId;
        sessionDate = "2026-08-10"; status = "LATE"; scheduledStart = "16:00"; minutesLate = 5
    })).Status
} else { $maCreateActual = 0 }
$n++
Add-TestResult -Number $n -Case "Mark attendance as MOSQUE_ADMIN" -Method "POST" -Endpoint "/api/v1/attendance" -Role "MOSQUE_ADMIN" -Input "active enrollment" -Expected 201 -Actual $maCreateActual -Note $createNote

if ($createReady) {
    $tCreateActual = (Invoke-ApiRequest -Method "Post" -Path "/api/v1/attendance" -Token $teacher.accessToken -Body (Test-Body @{
        enrollmentId = $activeEnrollmentId; circleId = $activeEnrollmentCircleId;
        sessionDate = "2026-08-11"; status = "ABSENT"; scheduledStart = "16:00"
    })).Status
} else { $tCreateActual = 0 }
$n++
Add-TestResult -Number $n -Case "Mark attendance as TEACHER (not assigned to circle)" -Method "POST" -Endpoint "/api/v1/attendance" -Role "TEACHER" -Input "active enrollment" -Expected 403 -Actual $tCreateActual -Note $createNote

$n++
Add-TestResult -Number $n -Case "Mark attendance as STUDENT" -Method "POST" -Endpoint "/api/v1/attendance" -Role "STUDENT" -Input "valid body" -Expected 403 -Actual (Invoke-ApiRequest -Method "Post" -Path "/api/v1/attendance" -Token $student.accessToken -Body (Test-Body @{ enrollmentId = [guid]::NewGuid(); circleId = [guid]::NewGuid(); sessionDate = "2026-08-09"; status = "PRESENT" })).Status
$n++
Add-TestResult -Number $n -Case "Mark attendance as PARENT" -Method "POST" -Endpoint "/api/v1/attendance" -Role "PARENT" -Input "valid body" -Expected 403 -Actual (Invoke-ApiRequest -Method "Post" -Path "/api/v1/attendance" -Token $parent.accessToken -Body (Test-Body @{ enrollmentId = [guid]::NewGuid(); circleId = [guid]::NewGuid(); sessionDate = "2026-08-09"; status = "PRESENT" })).Status
$n++
Add-TestResult -Number $n -Case "Mark attendance empty body" -Method "POST" -Endpoint "/api/v1/attendance" -Role "SUPER_ADMIN" -Input "{}" -Expected 400 -Actual (Invoke-ApiRequest -Method "Post" -Path "/api/v1/attendance" -Token $admin.accessToken -Body "{}").Status
$n++
Add-TestResult -Number $n -Case "Mark attendance invalid status enum" -Method "POST" -Endpoint "/api/v1/attendance" -Role "SUPER_ADMIN" -Input "status=MAYBE" -Expected 400 -Actual (Invoke-ApiRequest -Method "Post" -Path "/api/v1/attendance" -Token $admin.accessToken -Body (Test-Body @{ enrollmentId = [guid]::NewGuid(); circleId = [guid]::NewGuid(); sessionDate = "2026-08-09"; status = "MAYBE" })).Status
$n++
Add-TestResult -Number $n -Case "Mark attendance nonexistent enrollment" -Method "POST" -Endpoint "/api/v1/attendance" -Role "SUPER_ADMIN" -Input "random enrollmentId" -Expected 404 -Actual (Invoke-ApiRequest -Method "Post" -Path "/api/v1/attendance" -Token $admin.accessToken -Body (Test-Body @{ enrollmentId = [guid]::NewGuid(); circleId = [guid]::NewGuid(); sessionDate = "2026-08-09"; status = "PRESENT"; scheduledStart = "16:00" })).Status

# Target record for GET/PUT/excuse: prefer an existing record, else the one just created
$targetId = $attendanceId
if (-not $targetId) { $targetId = $createdId }

Write-Case "Step 6 - GET /attendance/{id}"
$targetReady = [bool]$targetId
$targetNote = ""
if (-not $targetReady) { $targetNote = "SKIPPED" }
if ($targetReady) {
    $getActual = (Invoke-ApiRequest -Method "Get" -Path "/api/v1/attendance/$targetId" -Token $admin.accessToken).Status
} else { $getActual = 0 }
$n++
Add-TestResult -Number $n -Case "Get attendance by id" -Method "GET" -Endpoint "/api/v1/attendance/{id}" -Role "SUPER_ADMIN" -Input "id=$targetId" -Expected 200 -Actual $getActual -Note $targetNote
if ($targetReady) {
    $getTeacherActual = (Invoke-ApiRequest -Method "Get" -Path "/api/v1/attendance/$targetId" -Token $teacher.accessToken).Status
} else { $getTeacherActual = 0 }
$n++
Add-TestResult -Number $n -Case "Get attendance as TEACHER (no mosque)" -Method "GET" -Endpoint "/api/v1/attendance/{id}" -Role "TEACHER" -Input "id=$targetId" -Expected 403 -Actual $getTeacherActual -Note $targetNote
$n++
Add-TestResult -Number $n -Case "Get attendance random id" -Method "GET" -Endpoint "/api/v1/attendance/{id}" -Role "SUPER_ADMIN" -Input "random uuid" -Expected 404 -Actual (Invoke-ApiRequest -Method "Get" -Path "/api/v1/attendance/$([guid]::NewGuid())" -Token $admin.accessToken).Status
$n++
Add-TestResult -Number $n -Case "Get attendance invalid uuid" -Method "GET" -Endpoint "/api/v1/attendance/{id}" -Role "SUPER_ADMIN" -Input "not-a-uuid" -Expected 400 -Actual (Invoke-ApiRequest -Method "Get" -Path "/api/v1/attendance/not-a-uuid" -Token $admin.accessToken).Status

Write-Case "Step 7 - PUT /attendance/{id} (SUPER_ADMIN/MOSQUE_ADMIN/TEACHER, override audit)"
$n++
Add-TestResult -Number $n -Case "Update as SUPER_ADMIN without audit reason" -Method "PUT" -Endpoint "/api/v1/attendance/{id}" -Role "SUPER_ADMIN" -Input "no audit reason" -Expected 400 -Actual (Invoke-ApiRequest -Method "Put" -Path "/api/v1/attendance/$([guid]::NewGuid())" -Token $admin.accessToken -Body (Test-Body @{ status = "LATE" })).Status
if ($targetReady) {
    $putAdminActual = (Invoke-ApiRequest -Method "Put" -Path "/api/v1/attendance/$targetId" -Token $admin.accessToken -Body (Test-Body @{ status = "LATE"; minutesLate = 10; auditReason = "manual override test" })).Status
} else { $putAdminActual = 0 }
$n++
Add-TestResult -Number $n -Case "Update as SUPER_ADMIN with audit reason" -Method "PUT" -Endpoint "/api/v1/attendance/{id}" -Role "SUPER_ADMIN" -Input "status=LATE, audit" -Expected 200 -Actual $putAdminActual -Note $targetNote
if ($targetReady) {
    $putTeacherActual = (Invoke-ApiRequest -Method "Put" -Path "/api/v1/attendance/$targetId" -Token $teacher.accessToken -Body (Test-Body @{ status = "LATE" })).Status
} else { $putTeacherActual = 0 }
$n++
Add-TestResult -Number $n -Case "Update as TEACHER (no mosque)" -Method "PUT" -Endpoint "/api/v1/attendance/{id}" -Role "TEACHER" -Input "id=$targetId" -Expected 403 -Actual $putTeacherActual -Note $targetNote
$n++
Add-TestResult -Number $n -Case "Update as STUDENT" -Method "PUT" -Endpoint "/api/v1/attendance/{id}" -Role "STUDENT" -Input "valid body" -Expected 403 -Actual (Invoke-ApiRequest -Method "Put" -Path "/api/v1/attendance/$([guid]::NewGuid())" -Token $student.accessToken -Body (Test-Body @{ status = "LATE" })).Status
$n++
Add-TestResult -Number $n -Case "Update nonexistent attendance" -Method "PUT" -Endpoint "/api/v1/attendance/{id}" -Role "SUPER_ADMIN" -Input "random uuid + audit" -Expected 404 -Actual (Invoke-ApiRequest -Method "Put" -Path "/api/v1/attendance/$([guid]::NewGuid())" -Token $admin.accessToken -Body (Test-Body @{ status = "LATE"; auditReason = "manual override test" })).Status

Write-Case "Step 8 - POST /attendance/{id}/excuse (STUDENT/PARENT only)"
$n++
Add-TestResult -Number $n -Case "Excuse as TEACHER" -Method "POST" -Endpoint "/api/v1/attendance/{id}/excuse" -Role "TEACHER" -Input "random id" -Expected 403 -Actual (Invoke-ApiRequest -Method "Post" -Path "/api/v1/attendance/$([guid]::NewGuid())/excuse" -Token $teacher.accessToken -Body (Test-Body @{ absenceReason = "x" })).Status
$n++
Add-TestResult -Number $n -Case "Excuse as MOSQUE_ADMIN" -Method "POST" -Endpoint "/api/v1/attendance/{id}/excuse" -Role "MOSQUE_ADMIN" -Input "random id" -Expected 403 -Actual (Invoke-ApiRequest -Method "Post" -Path "/api/v1/attendance/$([guid]::NewGuid())/excuse" -Token $mosqueAdmin.accessToken -Body (Test-Body @{ absenceReason = "x" })).Status
$n++
Add-TestResult -Number $n -Case "Excuse as SUPER_ADMIN" -Method "POST" -Endpoint "/api/v1/attendance/{id}/excuse" -Role "SUPER_ADMIN" -Input "random id" -Expected 403 -Actual (Invoke-ApiRequest -Method "Post" -Path "/api/v1/attendance/$([guid]::NewGuid())/excuse" -Token $admin.accessToken -Body (Test-Body @{ absenceReason = "x" })).Status
$n++
Add-TestResult -Number $n -Case "Excuse nonexistent record as PARENT" -Method "POST" -Endpoint "/api/v1/attendance/{id}/excuse" -Role "PARENT" -Input "random id" -Expected 404 -Actual (Invoke-ApiRequest -Method "Post" -Path "/api/v1/attendance/$([guid]::NewGuid())/excuse" -Token $parent.accessToken -Body (Test-Body @{ absenceReason = "sick" })).Status
$n++
Add-TestResult -Number $n -Case "Excuse invalid body (reason > 500 chars)" -Method "POST" -Endpoint "/api/v1/attendance/{id}/excuse" -Role "PARENT" -Input "501 chars" -Expected 400 -Actual (Invoke-ApiRequest -Method "Post" -Path "/api/v1/attendance/$([guid]::NewGuid())/excuse" -Token $parent.accessToken -Body (Test-Body @{ absenceReason = ("x" * 501) })).Status
if ($targetReady) {
    $excParentActual = (Invoke-ApiRequest -Method "Post" -Path "/api/v1/attendance/$targetId/excuse" -Token $parent.accessToken -Body (Test-Body @{ absenceReason = "sick" })).Status
} else { $excParentActual = 0 }
$n++
Add-TestResult -Number $n -Case "Excuse as PARENT (no linked child)" -Method "POST" -Endpoint "/api/v1/attendance/{id}/excuse" -Role "PARENT" -Input "id=$targetId" -Expected 403 -Actual $excParentActual -Note $targetNote

if ($childAttendanceId -and (-not $ReadOnly)) {
    $excChildActual = (Invoke-ApiRequest -Method "Post" -Path "/api/v1/attendance/$childAttendanceId/excuse" -Token $parent.accessToken -Body (Test-Body @{ absenceReason = "sick" })).Status
} else { $excChildActual = 0 }
$excChildNote = ""
if (-not ($childAttendanceId -and (-not $ReadOnly))) { $excChildNote = "SKIPPED" }
$n++
Add-TestResult -Number $n -Case "Excuse as PARENT for linked child" -Method "POST" -Endpoint "/api/v1/attendance/{id}/excuse" -Role "PARENT" -Input "child attendance=$childAttendanceId" -Expected 200 -Actual $excChildActual -Note $excChildNote

if ($selfAttendanceId -and (-not $ReadOnly)) {
    $excSelfActual = (Invoke-ApiRequest -Method "Post" -Path "/api/v1/attendance/$selfAttendanceId/excuse" -Token $student.accessToken -Body (Test-Body @{ absenceReason = "personal" })).Status
} else { $excSelfActual = 0 }
$excSelfNote = ""
if (-not ($selfAttendanceId -and (-not $ReadOnly))) { $excSelfNote = "SKIPPED" }
$n++
Add-TestResult -Number $n -Case "Excuse own record as STUDENT" -Method "POST" -Endpoint "/api/v1/attendance/{id}/excuse" -Role "STUDENT" -Input "self attendance=$selfAttendanceId" -Expected 200 -Actual $excSelfActual -Note $excSelfNote

Show-TestResults
