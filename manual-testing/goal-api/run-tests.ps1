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
$anyCircleId = $null
$goalId = $null
$createdId = $null

$res = Invoke-ApiRequest -Method "Get" -Path "/api/v1/students" -Token $admin.accessToken
if ($res.Status -eq 200) {
    $items = @(($res.Body | ConvertFrom-Json).data.content)
    if ($items.Count -gt 0) { $anyStudentId = $items[0].id }
}
$res = Invoke-ApiRequest -Method "Get" -Path "/api/v1/circles" -Token $admin.accessToken
if ($res.Status -eq 200) {
    $items = @(($res.Body | ConvertFrom-Json).data.content)
    if ($items.Count -gt 0) { $anyCircleId = $items[0].id }
}

Write-Host "Resolved: student=$anyStudentId circle=$anyCircleId"

Write-Case "Step 1 - Auth basics"
$n++
Add-TestResult -Number $n -Case "GET /goals/{id} without token" -Method "GET" -Endpoint "/api/v1/goals/{id}" -Role "none" -Input "" -Expected 401 -Actual (Invoke-ApiRequest -Method "Get" -Path "/api/v1/goals/$([guid]::NewGuid())").Status
$n++
Add-TestResult -Number $n -Case "GET /goals/{id} garbage token" -Method "GET" -Endpoint "/api/v1/goals/{id}" -Role "garbage" -Input "Bearer xyz" -Expected 401 -Actual (Invoke-ApiRequest -Method "Get" -Path "/api/v1/goals/$([guid]::NewGuid())" -Token "xyz").Status

Write-Case "Step 2 - POST /goals (create: SUPER_ADMIN/MOSQUE_ADMIN/TEACHER)"
$createReady = (-not $ReadOnly) -and $anyStudentId -and $anyCircleId
if ($createReady) {
    $resp = Invoke-ApiRequest -Method "Post" -Path "/api/v1/goals" -Token $admin.accessToken -Body (Test-Body @{
        studentId = $anyStudentId; circleId = $anyCircleId; title = "Memorize Surah Al-Mulk";
        targetSurah = 67; status = "IN_PROGRESS"; dueDate = "2026-08-30"
    })
    $createActual = $resp.Status
    if ($createActual -eq 201) {
        $createdId = ($resp.Body | ConvertFrom-Json).data.id
    }
} else { $createActual = 0 }
$createNote = ""
if (-not $createReady) { $createNote = "SKIPPED" }
$n++
Add-TestResult -Number $n -Case "Create goal as SUPER_ADMIN" -Method "POST" -Endpoint "/api/v1/goals" -Role "SUPER_ADMIN" -Input "student+circle" -Expected 201 -Actual $createActual -Note $createNote

# Note: GoalService.create performs no mosque/tenant assertion, so MOSQUE_ADMIN
# and TEACHER succeed even without a mosque assignment (gap, flagged in guide).
if ($createReady) {
    $maCreateActual = (Invoke-ApiRequest -Method "Post" -Path "/api/v1/goals" -Token $mosqueAdmin.accessToken -Body (Test-Body @{
        studentId = $anyStudentId; circleId = $anyCircleId; title = "Revise Juz Amma";
        targetJuz = 30; status = "IN_PROGRESS"; dueDate = "2026-08-30"
    })).Status
} else { $maCreateActual = 0 }
$n++
Add-TestResult -Number $n -Case "Create goal as MOSQUE_ADMIN" -Method "POST" -Endpoint "/api/v1/goals" -Role "MOSQUE_ADMIN" -Input "student+circle" -Expected 201 -Actual $maCreateActual -Note $createNote
if ($createReady) {
    $teCreateActual = (Invoke-ApiRequest -Method "Post" -Path "/api/v1/goals" -Token $teacher.accessToken -Body (Test-Body @{
        studentId = $anyStudentId; circleId = $anyCircleId; title = "Memorize Surah Al-Fatihah";
        targetSurah = 1; status = "IN_PROGRESS"; dueDate = "2026-08-30"
    })).Status
} else { $teCreateActual = 0 }
$n++
Add-TestResult -Number $n -Case "Create goal as TEACHER" -Method "POST" -Endpoint "/api/v1/goals" -Role "TEACHER" -Input "student+circle" -Expected 201 -Actual $teCreateActual -Note $createNote
$n++
Add-TestResult -Number $n -Case "Create goal as STUDENT" -Method "POST" -Endpoint "/api/v1/goals" -Role "STUDENT" -Input "valid body" -Expected 403 -Actual (Invoke-ApiRequest -Method "Post" -Path "/api/v1/goals" -Token $student.accessToken -Body (Test-Body @{ studentId = [guid]::NewGuid(); circleId = [guid]::NewGuid(); title = "x" })).Status
$n++
Add-TestResult -Number $n -Case "Create goal as PARENT" -Method "POST" -Endpoint "/api/v1/goals" -Role "PARENT" -Input "valid body" -Expected 403 -Actual (Invoke-ApiRequest -Method "Post" -Path "/api/v1/goals" -Token $parent.accessToken -Body (Test-Body @{ studentId = [guid]::NewGuid(); circleId = [guid]::NewGuid(); title = "x" })).Status
$n++
Add-TestResult -Number $n -Case "Create goal empty body" -Method "POST" -Endpoint "/api/v1/goals" -Role "SUPER_ADMIN" -Input "{}" -Expected 400 -Actual (Invoke-ApiRequest -Method "Post" -Path "/api/v1/goals" -Token $admin.accessToken -Body "{}").Status
$n++
Add-TestResult -Number $n -Case "Create goal title > 200 chars" -Method "POST" -Endpoint "/api/v1/goals" -Role "SUPER_ADMIN" -Input "title=201 chars" -Expected 400 -Actual (Invoke-ApiRequest -Method "Post" -Path "/api/v1/goals" -Token $admin.accessToken -Body (Test-Body @{ studentId = [guid]::NewGuid(); circleId = [guid]::NewGuid(); title = ("x" * 201) })).Status
$n++
Add-TestResult -Number $n -Case "Create goal invalid status enum" -Method "POST" -Endpoint "/api/v1/goals" -Role "SUPER_ADMIN" -Input "status=DONE" -Expected 400 -Actual (Invoke-ApiRequest -Method "Post" -Path "/api/v1/goals" -Token $admin.accessToken -Body (Test-Body @{ studentId = [guid]::NewGuid(); circleId = [guid]::NewGuid(); title = "x"; status = "DONE" })).Status
$n++
Add-TestResult -Number $n -Case "Create goal nonexistent student" -Method "POST" -Endpoint "/api/v1/goals" -Role "SUPER_ADMIN" -Input "random studentId" -Expected 404 -Actual (Invoke-ApiRequest -Method "Post" -Path "/api/v1/goals" -Token $admin.accessToken -Body (Test-Body @{ studentId = [guid]::NewGuid(); circleId = "00000000-0000-0000-0000-000000000001"; title = "x" })).Status
$missingCircleNote = ""
if (-not $anyStudentId) { $missingCircleNote = "SKIPPED" }
if ($anyStudentId) {
    $missCircleActual = (Invoke-ApiRequest -Method "Post" -Path "/api/v1/goals" -Token $admin.accessToken -Body (Test-Body @{ studentId = $anyStudentId; circleId = [guid]::NewGuid(); title = "x" })).Status
} else { $missCircleActual = 0 }
$n++
Add-TestResult -Number $n -Case "Create goal nonexistent circle" -Method "POST" -Endpoint "/api/v1/goals" -Role "SUPER_ADMIN" -Input "random circleId" -Expected 404 -Actual $missCircleActual -Note $missingCircleNote

# Target goal for GET/PUT: prefer existing, else the one just created
$targetId = $goalId
if (-not $targetId) { $targetId = $createdId }

Write-Case "Step 3 - GET /goals/{id}"
$targetReady = [bool]$targetId
$targetNote = ""
if (-not $targetReady) { $targetNote = "SKIPPED" }
if ($targetReady) {
    $getActual = (Invoke-ApiRequest -Method "Get" -Path "/api/v1/goals/$targetId" -Token $admin.accessToken).Status
} else { $getActual = 0 }
$n++
Add-TestResult -Number $n -Case "Get goal by id" -Method "GET" -Endpoint "/api/v1/goals/{id}" -Role "SUPER_ADMIN" -Input "id=$targetId" -Expected 200 -Actual $getActual -Note $targetNote
if ($targetReady) {
    $getParentActual = (Invoke-ApiRequest -Method "Get" -Path "/api/v1/goals/$targetId" -Token $parent.accessToken).Status
} else { $getParentActual = 0 }
$n++
Add-TestResult -Number $n -Case "Get goal as PARENT (no link)" -Method "GET" -Endpoint "/api/v1/goals/{id}" -Role "PARENT" -Input "id=$targetId" -Expected 403 -Actual $getParentActual -Note $targetNote
$n++
Add-TestResult -Number $n -Case "Get goal random id" -Method "GET" -Endpoint "/api/v1/goals/{id}" -Role "SUPER_ADMIN" -Input "random uuid" -Expected 404 -Actual (Invoke-ApiRequest -Method "Get" -Path "/api/v1/goals/$([guid]::NewGuid())" -Token $admin.accessToken).Status
$n++
Add-TestResult -Number $n -Case "Get goal invalid uuid" -Method "GET" -Endpoint "/api/v1/goals/{id}" -Role "SUPER_ADMIN" -Input "not-a-uuid" -Expected 400 -Actual (Invoke-ApiRequest -Method "Get" -Path "/api/v1/goals/not-a-uuid" -Token $admin.accessToken).Status

Write-Case "Step 4 - GET /goals/student/{studentId}"
$studentCasesReady = [bool]$anyStudentId
$studentNote = ""
if (-not $studentCasesReady) { $studentNote = "SKIPPED" }
if ($studentCasesReady) {
    $stActual = (Invoke-ApiRequest -Method "Get" -Path "/api/v1/goals/student/$anyStudentId" -Token $admin.accessToken).Status
} else { $stActual = 0 }
$n++
Add-TestResult -Number $n -Case "Student goals as SUPER_ADMIN" -Method "GET" -Endpoint "/api/v1/goals/student/{studentId}" -Role "SUPER_ADMIN" -Input "student=$anyStudentId" -Expected 200 -Actual $stActual -Note $studentNote

# GAP: no access assertion - any authenticated caller can list goals by student id
if ($studentCasesReady) {
    $stParentActual = (Invoke-ApiRequest -Method "Get" -Path "/api/v1/goals/student/$anyStudentId" -Token $parent.accessToken).Status
} else { $stParentActual = 0 }
$n++
Add-TestResult -Number $n -Case "Student goals as PARENT (no link, gap)" -Method "GET" -Endpoint "/api/v1/goals/student/{studentId}" -Role "PARENT" -Input "student=$anyStudentId" -Expected 200 -Actual $stParentActual -Note $studentNote
$n++
Add-TestResult -Number $n -Case "Student goals random student (no check)" -Method "GET" -Endpoint "/api/v1/goals/student/{studentId}" -Role "SUPER_ADMIN" -Input "random uuid" -Expected 200 -Actual (Invoke-ApiRequest -Method "Get" -Path "/api/v1/goals/student/$([guid]::NewGuid())" -Token $admin.accessToken).Status
$n++
Add-TestResult -Number $n -Case "Student goals invalid uuid" -Method "GET" -Endpoint "/api/v1/goals/student/{studentId}" -Role "SUPER_ADMIN" -Input "not-a-uuid" -Expected 400 -Actual (Invoke-ApiRequest -Method "Get" -Path "/api/v1/goals/student/not-a-uuid" -Token $admin.accessToken).Status

Write-Case "Step 5 - PUT /goals/{id} (SUPER_ADMIN/MOSQUE_ADMIN/TEACHER)"
if ($targetReady) {
    $putAdminActual = (Invoke-ApiRequest -Method "Put" -Path "/api/v1/goals/$targetId" -Token $admin.accessToken -Body (Test-Body @{ status = "COMPLETED"; completedDate = "2026-08-09" })).Status
} else { $putAdminActual = 0 }
$n++
Add-TestResult -Number $n -Case "Update goal as SUPER_ADMIN" -Method "PUT" -Endpoint "/api/v1/goals/{id}" -Role "SUPER_ADMIN" -Input "status=COMPLETED" -Expected 200 -Actual $putAdminActual -Note $targetNote

# GAP: no access assertion in GoalService.update - any allowed role can update any goal
if ($targetReady) {
    $putTeacherActual = (Invoke-ApiRequest -Method "Put" -Path "/api/v1/goals/$targetId" -Token $teacher.accessToken -Body (Test-Body @{ status = "IN_PROGRESS" })).Status
} else { $putTeacherActual = 0 }
$n++
Add-TestResult -Number $n -Case "Update goal as TEACHER (no check, gap)" -Method "PUT" -Endpoint "/api/v1/goals/{id}" -Role "TEACHER" -Input "id=$targetId" -Expected 200 -Actual $putTeacherActual -Note $targetNote
$n++
Add-TestResult -Number $n -Case "Update goal as STUDENT" -Method "PUT" -Endpoint "/api/v1/goals/{id}" -Role "STUDENT" -Input "valid body" -Expected 403 -Actual (Invoke-ApiRequest -Method "Put" -Path "/api/v1/goals/$([guid]::NewGuid())" -Token $student.accessToken -Body (Test-Body @{ status = "COMPLETED" })).Status
$n++
Add-TestResult -Number $n -Case "Update goal invalid status enum" -Method "PUT" -Endpoint "/api/v1/goals/{id}" -Role "SUPER_ADMIN" -Input "status=DONE" -Expected 400 -Actual (Invoke-ApiRequest -Method "Put" -Path "/api/v1/goals/$([guid]::NewGuid())" -Token $admin.accessToken -Body (Test-Body @{ status = "DONE" })).Status
$n++
Add-TestResult -Number $n -Case "Update nonexistent goal" -Method "PUT" -Endpoint "/api/v1/goals/{id}" -Role "SUPER_ADMIN" -Input "random uuid" -Expected 404 -Actual (Invoke-ApiRequest -Method "Put" -Path "/api/v1/goals/$([guid]::NewGuid())" -Token $admin.accessToken -Body (Test-Body @{ status = "COMPLETED" })).Status

Show-TestResults
