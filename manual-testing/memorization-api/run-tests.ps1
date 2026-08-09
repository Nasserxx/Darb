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

# Resolve existing domain ids. Seed users have no profiles, so these resolve
# only if prior manual testing created the data.
$anyStudentId = $null
$selfStudentId = $null
$anyCircleId = $null
$teacherId = $null
$activeEnrollmentId = $null
$activeEnrollmentCircleId = $null
$activeEnrollmentStudentId = $null
$selfCircleId = $null
$progressId = $null
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
$res = Invoke-ApiRequest -Method "Get" -Path "/api/v1/teachers" -Token $admin.accessToken
if ($res.Status -eq 200) {
    $items = @(($res.Body | ConvertFrom-Json).data.content)
    if ($items.Count -gt 0) { $teacherId = $items[0].id }
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
    foreach ($enr in $items) {
        if ($enr.status -eq "ACTIVE" -and $selfStudentId -and $enr.studentId -eq $selfStudentId) {
            $selfCircleId = $enr.circleId
            break
        }
    }
}

Write-Host "Resolved: student=$anyStudentId selfStudent=$selfStudentId circle=$anyCircleId teacher=$teacherId activeEnrollment=$activeEnrollmentId selfCircle=$selfCircleId"

Write-Case "Step 1 - Auth basics"
$n++
Add-TestResult -Number $n -Case "GET /memorization/{id} without token" -Method "GET" -Endpoint "/api/v1/memorization/{id}" -Role "none" -Input "" -Expected 401 -Actual (Invoke-ApiRequest -Method "Get" -Path "/api/v1/memorization/$([guid]::NewGuid())").Status
$n++
Add-TestResult -Number $n -Case "GET /memorization/{id} garbage token" -Method "GET" -Endpoint "/api/v1/memorization/{id}" -Role "garbage" -Input "Bearer xyz" -Expected 401 -Actual (Invoke-ApiRequest -Method "Get" -Path "/api/v1/memorization/$([guid]::NewGuid())" -Token "xyz").Status

Write-Case "Step 2 - POST /memorization (record progress: SUPER_ADMIN/MOSQUE_ADMIN/TEACHER)"
$createReady = (-not $ReadOnly) -and $activeEnrollmentId -and $activeEnrollmentCircleId -and $activeEnrollmentStudentId -and $teacherId
if ($createReady) {
    $resp = Invoke-ApiRequest -Method "Post" -Path "/api/v1/memorization" -Token $admin.accessToken -Body (Test-Body @{
        studentId = $activeEnrollmentStudentId; circleId = $activeEnrollmentCircleId; teacherId = $teacherId;
        surahNumber = 2; ayahFrom = 1; ayahTo = 25; grade = "VERY_GOOD"; tajweedScore = 85;
        sessionDate = "2026-08-09"
    })
    $createActual = $resp.Status
    if ($createActual -eq 201) {
        $createdId = ($resp.Body | ConvertFrom-Json).data.id
    }
} else { $createActual = 0 }
$createNote = ""
if (-not $createReady) { $createNote = "SKIPPED" }
$n++
Add-TestResult -Number $n -Case "Record progress as SUPER_ADMIN" -Method "POST" -Endpoint "/api/v1/memorization" -Role "SUPER_ADMIN" -Input "active enrollment + teacher" -Expected 201 -Actual $createActual -Note $createNote

$studentDenyNote = ""
if (-not $anyStudentId) { $studentDenyNote = "SKIPPED" }
if ($anyStudentId) {
    $maActual = (Invoke-ApiRequest -Method "Post" -Path "/api/v1/memorization" -Token $mosqueAdmin.accessToken -Body (Test-Body @{
        studentId = $anyStudentId; circleId = $anyCircleId; teacherId = $teacherId;
        surahNumber = 2; ayahFrom = 1; ayahTo = 5; grade = "GOOD"; sessionDate = "2026-08-09"
    })).Status
} else { $maActual = 0 }
$n++
Add-TestResult -Number $n -Case "Record progress as MOSQUE_ADMIN (no mosque)" -Method "POST" -Endpoint "/api/v1/memorization" -Role "MOSQUE_ADMIN" -Input "student=$anyStudentId" -Expected 403 -Actual $maActual -Note $studentDenyNote
if ($anyStudentId) {
    $teActual = (Invoke-ApiRequest -Method "Post" -Path "/api/v1/memorization" -Token $teacher.accessToken -Body (Test-Body @{
        studentId = $anyStudentId; circleId = $anyCircleId; teacherId = $teacherId;
        surahNumber = 2; ayahFrom = 1; ayahTo = 5; grade = "GOOD"; sessionDate = "2026-08-09"
    })).Status
} else { $teActual = 0 }
$n++
Add-TestResult -Number $n -Case "Record progress as TEACHER (no mosque)" -Method "POST" -Endpoint "/api/v1/memorization" -Role "TEACHER" -Input "student=$anyStudentId" -Expected 403 -Actual $teActual -Note $studentDenyNote
$n++
Add-TestResult -Number $n -Case "Record progress as STUDENT" -Method "POST" -Endpoint "/api/v1/memorization" -Role "STUDENT" -Input "valid body" -Expected 403 -Actual (Invoke-ApiRequest -Method "Post" -Path "/api/v1/memorization" -Token $student.accessToken -Body (Test-Body @{ studentId = [guid]::NewGuid(); circleId = [guid]::NewGuid(); teacherId = [guid]::NewGuid(); surahNumber = 1; ayahFrom = 1; ayahTo = 1; sessionDate = "2026-08-09" })).Status
$n++
Add-TestResult -Number $n -Case "Record progress as PARENT" -Method "POST" -Endpoint "/api/v1/memorization" -Role "PARENT" -Input "valid body" -Expected 403 -Actual (Invoke-ApiRequest -Method "Post" -Path "/api/v1/memorization" -Token $parent.accessToken -Body (Test-Body @{ studentId = [guid]::NewGuid(); circleId = [guid]::NewGuid(); teacherId = [guid]::NewGuid(); surahNumber = 1; ayahFrom = 1; ayahTo = 1; sessionDate = "2026-08-09" })).Status
$n++
Add-TestResult -Number $n -Case "Record progress empty body" -Method "POST" -Endpoint "/api/v1/memorization" -Role "SUPER_ADMIN" -Input "{}" -Expected 400 -Actual (Invoke-ApiRequest -Method "Post" -Path "/api/v1/memorization" -Token $admin.accessToken -Body "{}").Status
$n++
Add-TestResult -Number $n -Case "Record progress invalid grade enum" -Method "POST" -Endpoint "/api/v1/memorization" -Role "SUPER_ADMIN" -Input "grade=AVERAGE" -Expected 400 -Actual (Invoke-ApiRequest -Method "Post" -Path "/api/v1/memorization" -Token $admin.accessToken -Body (Test-Body @{ studentId = [guid]::NewGuid(); circleId = [guid]::NewGuid(); teacherId = [guid]::NewGuid(); surahNumber = 1; ayahFrom = 1; ayahTo = 1; grade = "AVERAGE"; sessionDate = "2026-08-09" })).Status
$n++
Add-TestResult -Number $n -Case "Record progress nonexistent student" -Method "POST" -Endpoint "/api/v1/memorization" -Role "SUPER_ADMIN" -Input "random studentId" -Expected 404 -Actual (Invoke-ApiRequest -Method "Post" -Path "/api/v1/memorization" -Token $admin.accessToken -Body (Test-Body @{ studentId = [guid]::NewGuid(); circleId = "00000000-0000-0000-0000-000000000001"; teacherId = "00000000-0000-0000-0000-000000000002"; surahNumber = 1; ayahFrom = 1; ayahTo = 1; grade = "GOOD"; sessionDate = "2026-08-09" })).Status

$noActiveNote = ""
if (-not ($anyStudentId -and $anyCircleId -and $teacherId -and (-not $activeEnrollmentId))) { $noActiveNote = "SKIPPED" }
if ($anyStudentId -and $anyCircleId -and $teacherId -and (-not $activeEnrollmentId)) {
    $naActual = (Invoke-ApiRequest -Method "Post" -Path "/api/v1/memorization" -Token $admin.accessToken -Body (Test-Body @{
        studentId = $anyStudentId; circleId = $anyCircleId; teacherId = $teacherId;
        surahNumber = 1; ayahFrom = 1; ayahTo = 1; grade = "GOOD"; sessionDate = "2026-08-09"
    })).Status
} else { $naActual = 0 }
$n++
Add-TestResult -Number $n -Case "Record progress without active enrollment" -Method "POST" -Endpoint "/api/v1/memorization" -Role "SUPER_ADMIN" -Input "student+circle+teacher" -Expected 400 -Actual $naActual -Note $noActiveNote

# Target record for GET/PUT: prefer existing, else the one just created
$targetId = $progressId
if (-not $targetId) { $targetId = $createdId }

Write-Case "Step 3 - GET /memorization/{id}"
$targetReady = [bool]$targetId
$targetNote = ""
if (-not $targetReady) { $targetNote = "SKIPPED" }
if ($targetReady) {
    $getActual = (Invoke-ApiRequest -Method "Get" -Path "/api/v1/memorization/$targetId" -Token $admin.accessToken).Status
} else { $getActual = 0 }
$n++
Add-TestResult -Number $n -Case "Get progress by id" -Method "GET" -Endpoint "/api/v1/memorization/{id}" -Role "SUPER_ADMIN" -Input "id=$targetId" -Expected 200 -Actual $getActual -Note $targetNote
if ($targetReady) {
    $getParentActual = (Invoke-ApiRequest -Method "Get" -Path "/api/v1/memorization/$targetId" -Token $parent.accessToken).Status
} else { $getParentActual = 0 }
$n++
Add-TestResult -Number $n -Case "Get progress as PARENT (no link)" -Method "GET" -Endpoint "/api/v1/memorization/{id}" -Role "PARENT" -Input "id=$targetId" -Expected 403 -Actual $getParentActual -Note $targetNote
$n++
Add-TestResult -Number $n -Case "Get progress random id" -Method "GET" -Endpoint "/api/v1/memorization/{id}" -Role "SUPER_ADMIN" -Input "random uuid" -Expected 404 -Actual (Invoke-ApiRequest -Method "Get" -Path "/api/v1/memorization/$([guid]::NewGuid())" -Token $admin.accessToken).Status
$n++
Add-TestResult -Number $n -Case "Get progress invalid uuid" -Method "GET" -Endpoint "/api/v1/memorization/{id}" -Role "SUPER_ADMIN" -Input "not-a-uuid" -Expected 400 -Actual (Invoke-ApiRequest -Method "Get" -Path "/api/v1/memorization/not-a-uuid" -Token $admin.accessToken).Status

Write-Case "Step 4 - GET /memorization/student/{studentId}"
$studentCasesReady = [bool]$anyStudentId
$studentNote = ""
if (-not $studentCasesReady) { $studentNote = "SKIPPED" }
if ($studentCasesReady) {
    $stActual = (Invoke-ApiRequest -Method "Get" -Path "/api/v1/memorization/student/$anyStudentId" -Token $admin.accessToken).Status
} else { $stActual = 0 }
$n++
Add-TestResult -Number $n -Case "Student progress as SUPER_ADMIN" -Method "GET" -Endpoint "/api/v1/memorization/student/{studentId}" -Role "SUPER_ADMIN" -Input "student=$anyStudentId" -Expected 200 -Actual $stActual -Note $studentNote
if ($studentCasesReady) {
    $stParentActual = (Invoke-ApiRequest -Method "Get" -Path "/api/v1/memorization/student/$anyStudentId" -Token $parent.accessToken).Status
} else { $stParentActual = 0 }
$n++
Add-TestResult -Number $n -Case "Student progress as PARENT (no link)" -Method "GET" -Endpoint "/api/v1/memorization/student/{studentId}" -Role "PARENT" -Input "student=$anyStudentId" -Expected 403 -Actual $stParentActual -Note $studentNote
$n++
Add-TestResult -Number $n -Case "Student progress random student" -Method "GET" -Endpoint "/api/v1/memorization/student/{studentId}" -Role "SUPER_ADMIN" -Input "random uuid" -Expected 404 -Actual (Invoke-ApiRequest -Method "Get" -Path "/api/v1/memorization/student/$([guid]::NewGuid())" -Token $admin.accessToken).Status
$n++
Add-TestResult -Number $n -Case "Student progress invalid uuid" -Method "GET" -Endpoint "/api/v1/memorization/student/{studentId}" -Role "SUPER_ADMIN" -Input "not-a-uuid" -Expected 400 -Actual (Invoke-ApiRequest -Method "Get" -Path "/api/v1/memorization/student/not-a-uuid" -Token $admin.accessToken).Status

Write-Case "Step 5 - GET /memorization/circle/{circleId}"
$circleCasesReady = [bool]$anyCircleId
$circleNote = ""
if (-not $circleCasesReady) { $circleNote = "SKIPPED" }
if ($circleCasesReady) {
    $cirActual = (Invoke-ApiRequest -Method "Get" -Path "/api/v1/memorization/circle/$anyCircleId" -Token $admin.accessToken).Status
} else { $cirActual = 0 }
$n++
Add-TestResult -Number $n -Case "Circle progress as SUPER_ADMIN" -Method "GET" -Endpoint "/api/v1/memorization/circle/{circleId}" -Role "SUPER_ADMIN" -Input "circle=$anyCircleId" -Expected 200 -Actual $cirActual -Note $circleNote
if ($circleCasesReady) {
    $cirStudentActual = (Invoke-ApiRequest -Method "Get" -Path "/api/v1/memorization/circle/$anyCircleId" -Token $student.accessToken).Status
} else { $cirStudentActual = 0 }
$n++
Add-TestResult -Number $n -Case "Circle progress as STUDENT (no mosque)" -Method "GET" -Endpoint "/api/v1/memorization/circle/{circleId}" -Role "STUDENT" -Input "circle=$anyCircleId" -Expected 403 -Actual $cirStudentActual -Note $circleNote
$n++
Add-TestResult -Number $n -Case "Circle progress random circle" -Method "GET" -Endpoint "/api/v1/memorization/circle/{circleId}" -Role "SUPER_ADMIN" -Input "random uuid" -Expected 404 -Actual (Invoke-ApiRequest -Method "Get" -Path "/api/v1/memorization/circle/$([guid]::NewGuid())" -Token $admin.accessToken).Status
$n++
Add-TestResult -Number $n -Case "Circle progress invalid uuid" -Method "GET" -Endpoint "/api/v1/memorization/circle/{circleId}" -Role "SUPER_ADMIN" -Input "not-a-uuid" -Expected 400 -Actual (Invoke-ApiRequest -Method "Get" -Path "/api/v1/memorization/circle/not-a-uuid" -Token $admin.accessToken).Status

Write-Case "Step 6 - POST /memorization/me (STUDENT self-report)"
if ($selfStudentId) {
    if ($selfCircleId -and (-not $ReadOnly)) {
        $meActual = (Invoke-ApiRequest -Method "Post" -Path "/api/v1/memorization/me" -Token $student.accessToken -Body (Test-Body @{
            circleId = $selfCircleId; surahNumber = 36; ayahFrom = 1; ayahTo = 10;
            grade = "ACCEPTABLE"; sessionDate = "2026-08-09"
        })).Status
    } else {
        $meActual = 0
    }
} else {
    $meActual = (Invoke-ApiRequest -Method "Post" -Path "/api/v1/memorization/me" -Token $student.accessToken -Body (Test-Body @{ circleId = [guid]::NewGuid(); surahNumber = 1; ayahFrom = 1; ayahTo = 1; sessionDate = "2026-08-09" })).Status
}
$meNote = ""
if ($selfStudentId -and (-not ($selfCircleId -and (-not $ReadOnly)))) { $meNote = "SKIPPED" }
$n++
Add-TestResult -Number $n -Case "Self-report progress as STUDENT" -Method "POST" -Endpoint "/api/v1/memorization/me" -Role "STUDENT" -Input "self active enrollment" -Expected 201 -Actual $meActual -Note $meNote

$meNoEnrNote = ""
if (-not ($selfStudentId -and (-not $selfCircleId) -and $anyCircleId)) { $meNoEnrNote = "SKIPPED" }
if ($selfStudentId -and (-not $selfCircleId) -and $anyCircleId) {
    $meNoEnrActual = (Invoke-ApiRequest -Method "Post" -Path "/api/v1/memorization/me" -Token $student.accessToken -Body (Test-Body @{
        circleId = $anyCircleId; surahNumber = 1; ayahFrom = 1; ayahTo = 1; grade = "GOOD"; sessionDate = "2026-08-09"
    })).Status
} else { $meNoEnrActual = 0 }
$n++
Add-TestResult -Number $n -Case "Self-report without active enrollment" -Method "POST" -Endpoint "/api/v1/memorization/me" -Role "STUDENT" -Input "circle=$anyCircleId" -Expected 400 -Actual $meNoEnrActual -Note $meNoEnrNote

$meNoProfileNote = ""
if ($selfStudentId) { $meNoProfileNote = "SKIPPED" }
$n++
Add-TestResult -Number $n -Case "Self-report without student profile" -Method "POST" -Endpoint "/api/v1/memorization/me" -Role "STUDENT" -Input "random circleId" -Expected 404 -Actual $meActual -Note $meNoProfileNote

$n++
Add-TestResult -Number $n -Case "Self-report as TEACHER" -Method "POST" -Endpoint "/api/v1/memorization/me" -Role "TEACHER" -Input "valid body" -Expected 403 -Actual (Invoke-ApiRequest -Method "Post" -Path "/api/v1/memorization/me" -Token $teacher.accessToken -Body (Test-Body @{ circleId = [guid]::NewGuid(); surahNumber = 1; ayahFrom = 1; ayahTo = 1; sessionDate = "2026-08-09" })).Status

Write-Case "Step 7 - PUT /memorization/{id} (SUPER_ADMIN/MOSQUE_ADMIN/TEACHER)"
if ($targetReady) {
    $putAdminActual = (Invoke-ApiRequest -Method "Put" -Path "/api/v1/memorization/$targetId" -Token $admin.accessToken -Body (Test-Body @{ grade = "EXCELLENT"; tajweedScore = 95 })).Status
} else { $putAdminActual = 0 }
$n++
Add-TestResult -Number $n -Case "Update progress as SUPER_ADMIN" -Method "PUT" -Endpoint "/api/v1/memorization/{id}" -Role "SUPER_ADMIN" -Input "grade=EXCELLENT" -Expected 200 -Actual $putAdminActual -Note $targetNote
if ($targetReady) {
    $putTeacherActual = (Invoke-ApiRequest -Method "Put" -Path "/api/v1/memorization/$targetId" -Token $teacher.accessToken -Body (Test-Body @{ grade = "GOOD" })).Status
} else { $putTeacherActual = 0 }
$n++
Add-TestResult -Number $n -Case "Update progress as TEACHER (no mosque)" -Method "PUT" -Endpoint "/api/v1/memorization/{id}" -Role "TEACHER" -Input "id=$targetId" -Expected 403 -Actual $putTeacherActual -Note $targetNote
$n++
Add-TestResult -Number $n -Case "Update progress as STUDENT" -Method "PUT" -Endpoint "/api/v1/memorization/{id}" -Role "STUDENT" -Input "valid body" -Expected 403 -Actual (Invoke-ApiRequest -Method "Put" -Path "/api/v1/memorization/$([guid]::NewGuid())" -Token $student.accessToken -Body (Test-Body @{ grade = "GOOD" })).Status
$n++
Add-TestResult -Number $n -Case "Update progress invalid grade enum" -Method "PUT" -Endpoint "/api/v1/memorization/{id}" -Role "SUPER_ADMIN" -Input "grade=AVERAGE" -Expected 400 -Actual (Invoke-ApiRequest -Method "Put" -Path "/api/v1/memorization/$([guid]::NewGuid())" -Token $admin.accessToken -Body (Test-Body @{ grade = "AVERAGE" })).Status
$n++
Add-TestResult -Number $n -Case "Update nonexistent progress" -Method "PUT" -Endpoint "/api/v1/memorization/{id}" -Role "SUPER_ADMIN" -Input "random uuid" -Expected 404 -Actual (Invoke-ApiRequest -Method "Put" -Path "/api/v1/memorization/$([guid]::NewGuid())" -Token $admin.accessToken -Body (Test-Body @{ grade = "GOOD" })).Status

Show-TestResults
