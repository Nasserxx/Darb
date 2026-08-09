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

# --- Dynamic data resolution -------------------------------------------------
# Existing enrollment, student and circle (needed for success paths)
$enrollmentId = $null
$studentId = $null
$circleId = $null
$res = Invoke-ApiRequest -Method "Get" -Path "/api/v1/enrollments" -Token $admin.accessToken
if ($res.Status -eq 200) {
    $content = @()
    try { $content = @(($res.Body | ConvertFrom-Json).data.content) } catch { }
    if ($content.Count -gt 0) { $enrollmentId = [string]$content[0].id }
}
$res = Invoke-ApiRequest -Method "Get" -Path "/api/v1/students" -Token $admin.accessToken
if ($res.Status -eq 200) {
    $content = @()
    try { $content = @(($res.Body | ConvertFrom-Json).data.content) } catch { }
    if ($content.Count -gt 0) { $studentId = [string]$content[0].id }
}
$res = Invoke-ApiRequest -Method "Get" -Path "/api/v1/circles" -Token $admin.accessToken
if ($res.Status -eq 200) {
    $content = @()
    try { $content = @(($res.Body | ConvertFrom-Json).data.content) } catch { }
    if ($content.Count -gt 0) { $circleId = [string]$content[0].id }
}
Write-Host "Resolved enrollment=$enrollmentId student=$studentId circle=$circleId"

$createdEnrollmentId = $null

Write-Case "Step 1 - Auth basics"
$n++
Add-TestResult -Number $n -Case "GET list without token" -Method "GET" -Endpoint "/api/v1/enrollments" -Role "none" -Input "" -Expected 401 -Actual (Invoke-ApiRequest -Method "Get" -Path "/api/v1/enrollments").Status
$n++
Add-TestResult -Number $n -Case "GET list with garbage token" -Method "GET" -Endpoint "/api/v1/enrollments" -Role "garbage" -Input "Bearer xyz" -Expected 401 -Actual (Invoke-ApiRequest -Method "Get" -Path "/api/v1/enrollments" -Token "xyz").Status
$n++
Add-TestResult -Number $n -Case "POST create without token" -Method "POST" -Endpoint "/api/v1/enrollments" -Role "none" -Input "{}" -Expected 401 -Actual (Invoke-ApiRequest -Method "Post" -Path "/api/v1/enrollments" -Body (Test-Body @{})).Status

Write-Case "Step 2 - GET /enrollments (SUPER_ADMIN, MOSQUE_ADMIN, TEACHER)"
$n++
Add-TestResult -Number $n -Case "List as SUPER_ADMIN" -Method "GET" -Endpoint "/api/v1/enrollments" -Role "SUPER_ADMIN" -Input "default page" -Expected 200 -Actual (Invoke-ApiRequest -Method "Get" -Path "/api/v1/enrollments" -Token $admin.accessToken).Status
$n++
Add-TestResult -Number $n -Case "List as MOSQUE_ADMIN" -Method "GET" -Endpoint "/api/v1/enrollments" -Role "MOSQUE_ADMIN" -Input "no mosque" -Expected 200 -Actual (Invoke-ApiRequest -Method "Get" -Path "/api/v1/enrollments" -Token $mosqueAdmin.accessToken).Status
$n++
Add-TestResult -Number $n -Case "List as TEACHER" -Method "GET" -Endpoint "/api/v1/enrollments" -Role "TEACHER" -Input "no mosque" -Expected 200 -Actual (Invoke-ApiRequest -Method "Get" -Path "/api/v1/enrollments" -Token $teacher.accessToken).Status
$n++
Add-TestResult -Number $n -Case "List as STUDENT" -Method "GET" -Endpoint "/api/v1/enrollments" -Role "STUDENT" -Input "" -Expected 403 -Actual (Invoke-ApiRequest -Method "Get" -Path "/api/v1/enrollments" -Token $student.accessToken).Status
$n++
Add-TestResult -Number $n -Case "List as PARENT" -Method "GET" -Endpoint "/api/v1/enrollments" -Role "PARENT" -Input "" -Expected 403 -Actual (Invoke-ApiRequest -Method "Get" -Path "/api/v1/enrollments" -Token $parent.accessToken).Status

Write-Case "Step 3 - GET /enrollments/student/{studentId} (any authenticated, student-scoped)"
$n++
Add-TestResult -Number $n -Case "Student list without token" -Method "GET" -Endpoint "/api/v1/enrollments/student/{id}" -Role "none" -Input "" -Expected 401 -Actual (Invoke-ApiRequest -Method "Get" -Path "/api/v1/enrollments/student/$([guid]::NewGuid())").Status
$n++
Add-TestResult -Number $n -Case "Student list invalid UUID" -Method "GET" -Endpoint "/api/v1/enrollments/student/{id}" -Role "SUPER_ADMIN" -Input "not-a-uuid" -Expected 400 -Actual (Invoke-ApiRequest -Method "Get" -Path "/api/v1/enrollments/student/not-a-uuid" -Token $admin.accessToken).Status
$n++
Add-TestResult -Number $n -Case "Student list random UUID" -Method "GET" -Endpoint "/api/v1/enrollments/student/{id}" -Role "SUPER_ADMIN" -Input "random" -Expected 404 -Actual (Invoke-ApiRequest -Method "Get" -Path "/api/v1/enrollments/student/$([guid]::NewGuid())" -Token $admin.accessToken).Status
$n++
Add-TestResult -Number $n -Case "Student list random as PARENT" -Method "GET" -Endpoint "/api/v1/enrollments/student/{id}" -Role "PARENT" -Input "random" -Expected 404 -Actual (Invoke-ApiRequest -Method "Get" -Path "/api/v1/enrollments/student/$([guid]::NewGuid())" -Token $parent.accessToken).Status
$n++
Add-TestResult -Number $n -Case "Student list random as STUDENT" -Method "GET" -Endpoint "/api/v1/enrollments/student/{id}" -Role "STUDENT" -Input "random" -Expected 404 -Actual (Invoke-ApiRequest -Method "Get" -Path "/api/v1/enrollments/student/$([guid]::NewGuid())" -Token $student.accessToken).Status
if ($studentId) {
    $n++
    Add-TestResult -Number $n -Case "Student list existing as SUPER_ADMIN" -Method "GET" -Endpoint "/api/v1/enrollments/student/{id}" -Role "SUPER_ADMIN" -Input "existing" -Expected 200 -Actual (Invoke-ApiRequest -Method "Get" -Path "/api/v1/enrollments/student/$studentId" -Token $admin.accessToken).Status
    $n++
    Add-TestResult -Number $n -Case "Student list existing as PARENT (no link)" -Method "GET" -Endpoint "/api/v1/enrollments/student/{id}" -Role "PARENT" -Input "existing" -Expected 403 -Actual (Invoke-ApiRequest -Method "Get" -Path "/api/v1/enrollments/student/$studentId" -Token $parent.accessToken).Status
    $n++
    Add-TestResult -Number $n -Case "Student list existing as STUDENT (other)" -Method "GET" -Endpoint "/api/v1/enrollments/student/{id}" -Role "STUDENT" -Input "existing" -Expected 403 -Actual (Invoke-ApiRequest -Method "Get" -Path "/api/v1/enrollments/student/$studentId" -Token $student.accessToken).Status
    $n++
    Add-TestResult -Number $n -Case "Student list existing as TEACHER (no mosque)" -Method "GET" -Endpoint "/api/v1/enrollments/student/{id}" -Role "TEACHER" -Input "existing" -Expected 403 -Actual (Invoke-ApiRequest -Method "Get" -Path "/api/v1/enrollments/student/$studentId" -Token $teacher.accessToken).Status
    $n++
    Add-TestResult -Number $n -Case "Student list existing as MOSQUE_ADMIN (no mosque)" -Method "GET" -Endpoint "/api/v1/enrollments/student/{id}" -Role "MOSQUE_ADMIN" -Input "existing" -Expected 403 -Actual (Invoke-ApiRequest -Method "Get" -Path "/api/v1/enrollments/student/$studentId" -Token $mosqueAdmin.accessToken).Status
} else {
    $n++
    Add-TestResult -Number $n -Case "Student list existing as SUPER_ADMIN" -Method "GET" -Endpoint "/api/v1/enrollments/student/{id}" -Role "SUPER_ADMIN" -Input "existing" -Expected 200 -Actual 0 -Note "SKIPPED"
    $n++
    Add-TestResult -Number $n -Case "Student list existing as PARENT (no link)" -Method "GET" -Endpoint "/api/v1/enrollments/student/{id}" -Role "PARENT" -Input "existing" -Expected 403 -Actual 0 -Note "SKIPPED"
    $n++
    Add-TestResult -Number $n -Case "Student list existing as STUDENT (other)" -Method "GET" -Endpoint "/api/v1/enrollments/student/{id}" -Role "STUDENT" -Input "existing" -Expected 403 -Actual 0 -Note "SKIPPED"
    $n++
    Add-TestResult -Number $n -Case "Student list existing as TEACHER (no mosque)" -Method "GET" -Endpoint "/api/v1/enrollments/student/{id}" -Role "TEACHER" -Input "existing" -Expected 403 -Actual 0 -Note "SKIPPED"
    $n++
    Add-TestResult -Number $n -Case "Student list existing as MOSQUE_ADMIN (no mosque)" -Method "GET" -Endpoint "/api/v1/enrollments/student/{id}" -Role "MOSQUE_ADMIN" -Input "existing" -Expected 403 -Actual 0 -Note "SKIPPED"
}

Write-Case "Step 4 - GET /enrollments/{id} (any authenticated, student-scoped)"
$n++
Add-TestResult -Number $n -Case "Get without token" -Method "GET" -Endpoint "/api/v1/enrollments/{id}" -Role "none" -Input "" -Expected 401 -Actual (Invoke-ApiRequest -Method "Get" -Path "/api/v1/enrollments/$([guid]::NewGuid())").Status
$n++
Add-TestResult -Number $n -Case "Get invalid UUID" -Method "GET" -Endpoint "/api/v1/enrollments/{id}" -Role "SUPER_ADMIN" -Input "not-a-uuid" -Expected 400 -Actual (Invoke-ApiRequest -Method "Get" -Path "/api/v1/enrollments/not-a-uuid" -Token $admin.accessToken).Status
$n++
Add-TestResult -Number $n -Case "Get random UUID" -Method "GET" -Endpoint "/api/v1/enrollments/{id}" -Role "SUPER_ADMIN" -Input "random" -Expected 404 -Actual (Invoke-ApiRequest -Method "Get" -Path "/api/v1/enrollments/$([guid]::NewGuid())" -Token $admin.accessToken).Status
$n++
Add-TestResult -Number $n -Case "Get random as PARENT" -Method "GET" -Endpoint "/api/v1/enrollments/{id}" -Role "PARENT" -Input "random" -Expected 404 -Actual (Invoke-ApiRequest -Method "Get" -Path "/api/v1/enrollments/$([guid]::NewGuid())" -Token $parent.accessToken).Status
$n++
Add-TestResult -Number $n -Case "Get random as STUDENT" -Method "GET" -Endpoint "/api/v1/enrollments/{id}" -Role "STUDENT" -Input "random" -Expected 404 -Actual (Invoke-ApiRequest -Method "Get" -Path "/api/v1/enrollments/$([guid]::NewGuid())" -Token $student.accessToken).Status
if ($enrollmentId) {
    $n++
    Add-TestResult -Number $n -Case "Get existing as SUPER_ADMIN" -Method "GET" -Endpoint "/api/v1/enrollments/{id}" -Role "SUPER_ADMIN" -Input "existing" -Expected 200 -Actual (Invoke-ApiRequest -Method "Get" -Path "/api/v1/enrollments/$enrollmentId" -Token $admin.accessToken).Status
    $n++
    Add-TestResult -Number $n -Case "Get existing as PARENT (no link)" -Method "GET" -Endpoint "/api/v1/enrollments/{id}" -Role "PARENT" -Input "existing" -Expected 403 -Actual (Invoke-ApiRequest -Method "Get" -Path "/api/v1/enrollments/$enrollmentId" -Token $parent.accessToken).Status
    $n++
    Add-TestResult -Number $n -Case "Get existing as TEACHER (no mosque)" -Method "GET" -Endpoint "/api/v1/enrollments/{id}" -Role "TEACHER" -Input "existing" -Expected 403 -Actual (Invoke-ApiRequest -Method "Get" -Path "/api/v1/enrollments/$enrollmentId" -Token $teacher.accessToken).Status
    $n++
    Add-TestResult -Number $n -Case "Get existing as MOSQUE_ADMIN (no mosque)" -Method "GET" -Endpoint "/api/v1/enrollments/{id}" -Role "MOSQUE_ADMIN" -Input "existing" -Expected 403 -Actual (Invoke-ApiRequest -Method "Get" -Path "/api/v1/enrollments/$enrollmentId" -Token $mosqueAdmin.accessToken).Status
    $n++
    Add-TestResult -Number $n -Case "Get existing as STUDENT (other)" -Method "GET" -Endpoint "/api/v1/enrollments/{id}" -Role "STUDENT" -Input "existing" -Expected 403 -Actual (Invoke-ApiRequest -Method "Get" -Path "/api/v1/enrollments/$enrollmentId" -Token $student.accessToken).Status
} else {
    $n++
    Add-TestResult -Number $n -Case "Get existing as SUPER_ADMIN" -Method "GET" -Endpoint "/api/v1/enrollments/{id}" -Role "SUPER_ADMIN" -Input "existing" -Expected 200 -Actual 0 -Note "SKIPPED"
    $n++
    Add-TestResult -Number $n -Case "Get existing as PARENT (no link)" -Method "GET" -Endpoint "/api/v1/enrollments/{id}" -Role "PARENT" -Input "existing" -Expected 403 -Actual 0 -Note "SKIPPED"
    $n++
    Add-TestResult -Number $n -Case "Get existing as TEACHER (no mosque)" -Method "GET" -Endpoint "/api/v1/enrollments/{id}" -Role "TEACHER" -Input "existing" -Expected 403 -Actual 0 -Note "SKIPPED"
    $n++
    Add-TestResult -Number $n -Case "Get existing as MOSQUE_ADMIN (no mosque)" -Method "GET" -Endpoint "/api/v1/enrollments/{id}" -Role "MOSQUE_ADMIN" -Input "existing" -Expected 403 -Actual 0 -Note "SKIPPED"
    $n++
    Add-TestResult -Number $n -Case "Get existing as STUDENT (other)" -Method "GET" -Endpoint "/api/v1/enrollments/{id}" -Role "STUDENT" -Input "existing" -Expected 403 -Actual 0 -Note "SKIPPED"
}

Write-Case "Step 5 - POST /enrollments (SUPER_ADMIN, MOSQUE_ADMIN)"
$n++
Add-TestResult -Number $n -Case "Create empty body" -Method "POST" -Endpoint "/api/v1/enrollments" -Role "SUPER_ADMIN" -Input "{}" -Expected 400 -Actual (Invoke-ApiRequest -Method "Post" -Path "/api/v1/enrollments" -Token $admin.accessToken -Body (Test-Body @{})).Status
$n++
Add-TestResult -Number $n -Case "Create unknown student" -Method "POST" -Endpoint "/api/v1/enrollments" -Role "SUPER_ADMIN" -Input "random student" -Expected 404 -Actual (Invoke-ApiRequest -Method "Post" -Path "/api/v1/enrollments" -Token $admin.accessToken -Body (Test-Body @{ studentId = [guid]::NewGuid(); circleId = [guid]::NewGuid() })).Status
$n++
Add-TestResult -Number $n -Case "Create unknown student as MOSQUE_ADMIN" -Method "POST" -Endpoint "/api/v1/enrollments" -Role "MOSQUE_ADMIN" -Input "random student" -Expected 404 -Actual (Invoke-ApiRequest -Method "Post" -Path "/api/v1/enrollments" -Token $mosqueAdmin.accessToken -Body (Test-Body @{ studentId = [guid]::NewGuid(); circleId = [guid]::NewGuid() })).Status
$n++
Add-TestResult -Number $n -Case "Create as TEACHER" -Method "POST" -Endpoint "/api/v1/enrollments" -Role "TEACHER" -Input "{}" -Expected 403 -Actual (Invoke-ApiRequest -Method "Post" -Path "/api/v1/enrollments" -Token $teacher.accessToken -Body (Test-Body @{})).Status
$n++
Add-TestResult -Number $n -Case "Create as STUDENT" -Method "POST" -Endpoint "/api/v1/enrollments" -Role "STUDENT" -Input "{}" -Expected 403 -Actual (Invoke-ApiRequest -Method "Post" -Path "/api/v1/enrollments" -Token $student.accessToken -Body (Test-Body @{})).Status
$n++
Add-TestResult -Number $n -Case "Create as PARENT" -Method "POST" -Endpoint "/api/v1/enrollments" -Role "PARENT" -Input "{}" -Expected 403 -Actual (Invoke-ApiRequest -Method "Post" -Path "/api/v1/enrollments" -Token $parent.accessToken -Body (Test-Body @{})).Status
if (-not $ReadOnly -and $studentId -and $circleId) {
    $resp = Invoke-ApiRequest -Method "Post" -Path "/api/v1/enrollments" -Token $admin.accessToken -Body (Test-Body @{
        studentId = $studentId; circleId = $circleId; status = "PENDING"; notes = "manual test"
    })
    $n++
    Add-TestResult -Number $n -Case "Create enrollment success" -Method "POST" -Endpoint "/api/v1/enrollments" -Role "SUPER_ADMIN" -Input "student+circle" -Expected 201 -Actual $resp.Status
    if ($resp.Status -eq 201) {
        $createdEnrollmentId = [string](($resp.Body | ConvertFrom-Json).data.id)
        Write-Host "Created enrollment id: $createdEnrollmentId"
    }
} else {
    $n++
    Add-TestResult -Number $n -Case "Create enrollment success" -Method "POST" -Endpoint "/api/v1/enrollments" -Role "SUPER_ADMIN" -Input "student+circle" -Expected 201 -Actual 0 -Note "SKIPPED"
}

Write-Case "Step 6 - PUT /enrollments/{id} (SUPER_ADMIN, MOSQUE_ADMIN)"
$n++
Add-TestResult -Number $n -Case "Update without token" -Method "PUT" -Endpoint "/api/v1/enrollments/{id}" -Role "none" -Input "{}" -Expected 401 -Actual (Invoke-ApiRequest -Method "Put" -Path "/api/v1/enrollments/$([guid]::NewGuid())" -Body (Test-Body @{})).Status
$n++
Add-TestResult -Number $n -Case "Update without audit reason" -Method "PUT" -Endpoint "/api/v1/enrollments/{id}" -Role "SUPER_ADMIN" -Input "random" -Expected 400 -Actual (Invoke-ApiRequest -Method "Put" -Path "/api/v1/enrollments/$([guid]::NewGuid())" -Token $admin.accessToken -Body (Test-Body @{ status = "ACTIVE" })).Status
$n++
Add-TestResult -Number $n -Case "Update random UUID" -Method "PUT" -Endpoint "/api/v1/enrollments/{id}" -Role "SUPER_ADMIN" -Input "random + audit" -Expected 404 -Actual (Invoke-ApiRequest -Method "Put" -Path "/api/v1/enrollments/$([guid]::NewGuid())" -Token $admin.accessToken -Body (Test-Body @{ status = "ACTIVE"; auditReason = "manual test" })).Status
$n++
Add-TestResult -Number $n -Case "Update invalid UUID" -Method "PUT" -Endpoint "/api/v1/enrollments/{id}" -Role "SUPER_ADMIN" -Input "not-a-uuid" -Expected 400 -Actual (Invoke-ApiRequest -Method "Put" -Path "/api/v1/enrollments/not-a-uuid" -Token $admin.accessToken -Body (Test-Body @{ status = "ACTIVE"; auditReason = "manual test" })).Status
$n++
Add-TestResult -Number $n -Case "Update random as MOSQUE_ADMIN" -Method "PUT" -Endpoint "/api/v1/enrollments/{id}" -Role "MOSQUE_ADMIN" -Input "random" -Expected 404 -Actual (Invoke-ApiRequest -Method "Put" -Path "/api/v1/enrollments/$([guid]::NewGuid())" -Token $mosqueAdmin.accessToken -Body (Test-Body @{ status = "ACTIVE" })).Status
$n++
Add-TestResult -Number $n -Case "Update as TEACHER" -Method "PUT" -Endpoint "/api/v1/enrollments/{id}" -Role "TEACHER" -Input "random" -Expected 403 -Actual (Invoke-ApiRequest -Method "Put" -Path "/api/v1/enrollments/$([guid]::NewGuid())" -Token $teacher.accessToken -Body (Test-Body @{ status = "ACTIVE" })).Status
$n++
Add-TestResult -Number $n -Case "Update as STUDENT" -Method "PUT" -Endpoint "/api/v1/enrollments/{id}" -Role "STUDENT" -Input "random" -Expected 403 -Actual (Invoke-ApiRequest -Method "Put" -Path "/api/v1/enrollments/$([guid]::NewGuid())" -Token $student.accessToken -Body (Test-Body @{ status = "ACTIVE" })).Status
$n++
Add-TestResult -Number $n -Case "Update as PARENT" -Method "PUT" -Endpoint "/api/v1/enrollments/{id}" -Role "PARENT" -Input "random" -Expected 403 -Actual (Invoke-ApiRequest -Method "Put" -Path "/api/v1/enrollments/$([guid]::NewGuid())" -Token $parent.accessToken -Body (Test-Body @{ status = "ACTIVE" })).Status
if ($createdEnrollmentId) {
    $n++
    Add-TestResult -Number $n -Case "Update as MOSQUE_ADMIN (no mosque)" -Method "PUT" -Endpoint "/api/v1/enrollments/{id}" -Role "MOSQUE_ADMIN" -Input "created enrollment" -Expected 403 -Actual (Invoke-ApiRequest -Method "Put" -Path "/api/v1/enrollments/$createdEnrollmentId" -Token $mosqueAdmin.accessToken -Body (Test-Body @{ status = "ACTIVE" })).Status
    $n++
    Add-TestResult -Number $n -Case "Update enrollment success" -Method "PUT" -Endpoint "/api/v1/enrollments/{id}" -Role "SUPER_ADMIN" -Input "created enrollment" -Expected 200 -Actual (Invoke-ApiRequest -Method "Put" -Path "/api/v1/enrollments/$createdEnrollmentId" -Token $admin.accessToken -Body (Test-Body @{ status = "ACTIVE"; auditReason = "manual test" })).Status
    $n++
    Add-TestResult -Number $n -Case "Update status without audit" -Method "PUT" -Endpoint "/api/v1/enrollments/{id}" -Role "SUPER_ADMIN" -Input "created enrollment" -Expected 400 -Actual (Invoke-ApiRequest -Method "Put" -Path "/api/v1/enrollments/$createdEnrollmentId" -Token $admin.accessToken -Body (Test-Body @{ status = "COMPLETED" })).Status
} else {
    $n++
    Add-TestResult -Number $n -Case "Update as MOSQUE_ADMIN (no mosque)" -Method "PUT" -Endpoint "/api/v1/enrollments/{id}" -Role "MOSQUE_ADMIN" -Input "created enrollment" -Expected 403 -Actual 0 -Note "SKIPPED"
    $n++
    Add-TestResult -Number $n -Case "Update enrollment success" -Method "PUT" -Endpoint "/api/v1/enrollments/{id}" -Role "SUPER_ADMIN" -Input "created enrollment" -Expected 200 -Actual 0 -Note "SKIPPED"
    $n++
    Add-TestResult -Number $n -Case "Update status without audit" -Method "PUT" -Endpoint "/api/v1/enrollments/{id}" -Role "SUPER_ADMIN" -Input "created enrollment" -Expected 400 -Actual 0 -Note "SKIPPED"
}

Show-TestResults
