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

$students = Invoke-ApiRequest -Method "Get" -Path "/api/v1/students" -Token $admin.accessToken
$studentId = Get-FirstId $students.Body
$mosques = Invoke-ApiRequest -Method "Get" -Path "/api/v1/mosques" -Token $admin.accessToken
$mosqueId = Get-FirstId $mosques.Body

$studentProfUserId = $null
try {
    $sp = ($students.Body | ConvertFrom-Json).data.content[0]
    if ($sp) { $studentProfUserId = $sp.userId }
} catch {}
Write-Host "Student id: $studentId  Mosque id: $mosqueId"

$achId = $null
if (-not $ReadOnly -and $studentId -and $mosqueId) {
    $created = Invoke-ApiRequest -Method "Post" -Path "/api/v1/achievements" -Token $admin.accessToken -Body (Test-Body @{
        studentId = $studentId
        mosqueId = $mosqueId
        type = "MILESTONE"
        title = "Throwaway Achievement"
        description = "Automated test achievement"
        awardedDate = "2026-01-01"
    })
    if ($created.Status -eq 201) {
        $achId = Get-FirstId $created.Body
        Write-Host "Throwaway achievement id: $achId"
    }
}

Write-Case "Step 1 - Auth basics"
$n++
Add-TestResult -Number $n -Case "GET /{id} without token" -Method "GET" -Endpoint "/api/v1/achievements/{id}" -Role "none" -Input "random uuid" -Expected 401 -Actual (Invoke-ApiRequest -Method "Get" -Path "/api/v1/achievements/$([guid]::NewGuid())").Status
$n++
Add-TestResult -Number $n -Case "GET /{id} with garbage token" -Method "GET" -Endpoint "/api/v1/achievements/{id}" -Role "garbage" -Input "random uuid" -Expected 401 -Actual (Invoke-ApiRequest -Method "Get" -Path "/api/v1/achievements/$([guid]::NewGuid())" -Token "xyz").Status

Write-Case "Step 2 - GET /{id} (any authenticated)"
if ($achId) {
    $expStu = 200
    if ($studentProfUserId -and $studentProfUserId -ne $student.userId) { $expStu = 403 }
    $n++
    Add-TestResult -Number $n -Case "Get own achievement as SUPER_ADMIN" -Method "GET" -Endpoint "/api/v1/achievements/{id}" -Role "SUPER_ADMIN" -Input "ach=$achId" -Expected 200 -Actual (Invoke-ApiRequest -Method "Get" -Path "/api/v1/achievements/$achId" -Token $admin.accessToken).Status
    $n++
    Add-TestResult -Number $n -Case "Get as STUDENT" -Method "GET" -Endpoint "/api/v1/achievements/{id}" -Role "STUDENT" -Input "ach=$achId" -Expected $expStu -Actual (Invoke-ApiRequest -Method "Get" -Path "/api/v1/achievements/$achId" -Token $student.accessToken).Status
    $n++
    Add-TestResult -Number $n -Case "Get as MOSQUE_ADMIN (no mosque)" -Method "GET" -Endpoint "/api/v1/achievements/{id}" -Role "MOSQUE_ADMIN" -Input "ach=$achId" -Expected 403 -Actual (Invoke-ApiRequest -Method "Get" -Path "/api/v1/achievements/$achId" -Token $mosqueAdmin.accessToken).Status
    $n++
    Add-TestResult -Number $n -Case "Get as TEACHER (no mosque)" -Method "GET" -Endpoint "/api/v1/achievements/{id}" -Role "TEACHER" -Input "ach=$achId" -Expected 403 -Actual (Invoke-ApiRequest -Method "Get" -Path "/api/v1/achievements/$achId" -Token $teacher.accessToken).Status
    $n++
    Add-TestResult -Number $n -Case "Get as PARENT (no link)" -Method "GET" -Endpoint "/api/v1/achievements/{id}" -Role "PARENT" -Input "ach=$achId" -Expected 403 -Actual (Invoke-ApiRequest -Method "Get" -Path "/api/v1/achievements/$achId" -Token $parent.accessToken).Status
} else {
    $n++
    Add-TestResult -Number $n -Case "Get own achievement as SUPER_ADMIN" -Method "GET" -Endpoint "/api/v1/achievements/{id}" -Role "SUPER_ADMIN" -Input "no ach" -Expected 200 -Actual 0 -Note "SKIPPED"
    $n++
    Add-TestResult -Number $n -Case "Get as STUDENT" -Method "GET" -Endpoint "/api/v1/achievements/{id}" -Role "STUDENT" -Input "no ach" -Expected 200 -Actual 0 -Note "SKIPPED"
    $n++
    Add-TestResult -Number $n -Case "Get as MOSQUE_ADMIN (no mosque)" -Method "GET" -Endpoint "/api/v1/achievements/{id}" -Role "MOSQUE_ADMIN" -Input "no ach" -Expected 403 -Actual 0 -Note "SKIPPED"
    $n++
    Add-TestResult -Number $n -Case "Get as TEACHER (no mosque)" -Method "GET" -Endpoint "/api/v1/achievements/{id}" -Role "TEACHER" -Input "no ach" -Expected 403 -Actual 0 -Note "SKIPPED"
    $n++
    Add-TestResult -Number $n -Case "Get as PARENT (no link)" -Method "GET" -Endpoint "/api/v1/achievements/{id}" -Role "PARENT" -Input "no ach" -Expected 403 -Actual 0 -Note "SKIPPED"
}
$n++
Add-TestResult -Number $n -Case "Get nonexistent" -Method "GET" -Endpoint "/api/v1/achievements/{id}" -Role "SUPER_ADMIN" -Input "random uuid" -Expected 404 -Actual (Invoke-ApiRequest -Method "Get" -Path "/api/v1/achievements/$([guid]::NewGuid())" -Token $admin.accessToken).Status
$n++
Add-TestResult -Number $n -Case "Get invalid UUID" -Method "GET" -Endpoint "/api/v1/achievements/{id}" -Role "SUPER_ADMIN" -Input "not-a-uuid" -Expected 400 -Actual (Invoke-ApiRequest -Method "Get" -Path "/api/v1/achievements/not-a-uuid" -Token $admin.accessToken).Status

Write-Case "Step 3 - GET /student/{studentId} (any authenticated)"
if ($studentId) {
    foreach ($roleEntry in @(
        @{ Role = "SUPER_ADMIN"; T = $admin },
        @{ Role = "MOSQUE_ADMIN"; T = $mosqueAdmin },
        @{ Role = "TEACHER"; T = $teacher },
        @{ Role = "STUDENT"; T = $student },
        @{ Role = "PARENT"; T = $parent }
    )) {
        $n++
        Add-TestResult -Number $n -Case "List by student" -Method "GET" -Endpoint "/api/v1/achievements/student/{studentId}" -Role $roleEntry.Role -Input "student=$studentId" -Expected 200 -Actual (Invoke-ApiRequest -Method "Get" -Path "/api/v1/achievements/student/$studentId" -Token $roleEntry.T.accessToken).Status -Note "no access check - verify"
    }
} else {
    $n++
    Add-TestResult -Number $n -Case "List by student (all roles)" -Method "GET" -Endpoint "/api/v1/achievements/student/{studentId}" -Role "SUPER_ADMIN" -Input "no student" -Expected 200 -Actual 0 -Note "SKIPPED"
}
$n++
Add-TestResult -Number $n -Case "List by nonexistent student" -Method "GET" -Endpoint "/api/v1/achievements/student/{studentId}" -Role "SUPER_ADMIN" -Input "random uuid" -Expected 200 -Actual (Invoke-ApiRequest -Method "Get" -Path "/api/v1/achievements/student/$([guid]::NewGuid())" -Token $admin.accessToken).Status -Note "docs claim 404 - verify"
$n++
Add-TestResult -Number $n -Case "List by invalid student UUID" -Method "GET" -Endpoint "/api/v1/achievements/student/{studentId}" -Role "SUPER_ADMIN" -Input "not-a-uuid" -Expected 400 -Actual (Invoke-ApiRequest -Method "Get" -Path "/api/v1/achievements/student/not-a-uuid" -Token $admin.accessToken).Status

Write-Case "Step 4 - GET /mosque/{mosqueId} (SUPER_ADMIN, MOSQUE_ADMIN)"
$n++
Add-TestResult -Number $n -Case "List by mosque as SUPER_ADMIN" -Method "GET" -Endpoint "/api/v1/achievements/mosque/{mosqueId}" -Role "SUPER_ADMIN" -Input "mosque=$mosqueId" -Expected 200 -Actual (Invoke-ApiRequest -Method "Get" -Path "/api/v1/achievements/mosque/$mosqueId" -Token $admin.accessToken).Status
$n++
Add-TestResult -Number $n -Case "List by mosque as MOSQUE_ADMIN" -Method "GET" -Endpoint "/api/v1/achievements/mosque/{mosqueId}" -Role "MOSQUE_ADMIN" -Input "mosque=$mosqueId" -Expected 200 -Actual (Invoke-ApiRequest -Method "Get" -Path "/api/v1/achievements/mosque/$mosqueId" -Token $mosqueAdmin.accessToken).Status
$n++
Add-TestResult -Number $n -Case "List by mosque as TEACHER" -Method "GET" -Endpoint "/api/v1/achievements/mosque/{mosqueId}" -Role "TEACHER" -Input "mosque=$mosqueId" -Expected 403 -Actual (Invoke-ApiRequest -Method "Get" -Path "/api/v1/achievements/mosque/$mosqueId" -Token $teacher.accessToken).Status
$n++
Add-TestResult -Number $n -Case "List by mosque as STUDENT" -Method "GET" -Endpoint "/api/v1/achievements/mosque/{mosqueId}" -Role "STUDENT" -Input "mosque=$mosqueId" -Expected 403 -Actual (Invoke-ApiRequest -Method "Get" -Path "/api/v1/achievements/mosque/$mosqueId" -Token $student.accessToken).Status
$n++
Add-TestResult -Number $n -Case "List by mosque as PARENT" -Method "GET" -Endpoint "/api/v1/achievements/mosque/{mosqueId}" -Role "PARENT" -Input "mosque=$mosqueId" -Expected 403 -Actual (Invoke-ApiRequest -Method "Get" -Path "/api/v1/achievements/mosque/$mosqueId" -Token $parent.accessToken).Status
$n++
Add-TestResult -Number $n -Case "List by nonexistent mosque" -Method "GET" -Endpoint "/api/v1/achievements/mosque/{mosqueId}" -Role "SUPER_ADMIN" -Input "random uuid" -Expected 200 -Actual (Invoke-ApiRequest -Method "Get" -Path "/api/v1/achievements/mosque/$([guid]::NewGuid())" -Token $admin.accessToken).Status -Note "docs claim 404 - verify"
$n++
Add-TestResult -Number $n -Case "List by invalid mosque UUID" -Method "GET" -Endpoint "/api/v1/achievements/mosque/{mosqueId}" -Role "SUPER_ADMIN" -Input "not-a-uuid" -Expected 400 -Actual (Invoke-ApiRequest -Method "Get" -Path "/api/v1/achievements/mosque/not-a-uuid" -Token $admin.accessToken).Status

Write-Case "Step 5 - POST /achievements (SUPER_ADMIN, MOSQUE_ADMIN, TEACHER)"
if ($studentId -and $mosqueId) {
    $n++
    Add-TestResult -Number $n -Case "Award as SUPER_ADMIN" -Method "POST" -Endpoint "/api/v1/achievements" -Role "SUPER_ADMIN" -Input "student+mosque" -Expected 201 -Actual (Invoke-ApiRequest -Method "Post" -Path "/api/v1/achievements" -Token $admin.accessToken -Body (Test-Body @{ studentId = $studentId; mosqueId = $mosqueId; type = "MEMORIZATION"; title = "Test Achievement A"; awardedDate = "2026-02-01" })).Status
    $n++
    Add-TestResult -Number $n -Case "Award as MOSQUE_ADMIN" -Method "POST" -Endpoint "/api/v1/achievements" -Role "MOSQUE_ADMIN" -Input "student+mosque" -Expected 201 -Actual (Invoke-ApiRequest -Method "Post" -Path "/api/v1/achievements" -Token $mosqueAdmin.accessToken -Body (Test-Body @{ studentId = $studentId; mosqueId = $mosqueId; type = "ATTENDANCE"; title = "Test Achievement B" })).Status
    $n++
    Add-TestResult -Number $n -Case "Award as TEACHER" -Method "POST" -Endpoint "/api/v1/achievements" -Role "TEACHER" -Input "student+mosque" -Expected 201 -Actual (Invoke-ApiRequest -Method "Post" -Path "/api/v1/achievements" -Token $teacher.accessToken -Body (Test-Body @{ studentId = $studentId; mosqueId = $mosqueId; type = "RECITATION"; title = "Test Achievement C" })).Status -Note "no mosque assertion - verify"
} else {
    $n++
    Add-TestResult -Number $n -Case "Award as SUPER_ADMIN" -Method "POST" -Endpoint "/api/v1/achievements" -Role "SUPER_ADMIN" -Input "no student/mosque" -Expected 201 -Actual 0 -Note "SKIPPED"
    $n++
    Add-TestResult -Number $n -Case "Award as MOSQUE_ADMIN" -Method "POST" -Endpoint "/api/v1/achievements" -Role "MOSQUE_ADMIN" -Input "no student/mosque" -Expected 201 -Actual 0 -Note "SKIPPED"
    $n++
    Add-TestResult -Number $n -Case "Award as TEACHER" -Method "POST" -Endpoint "/api/v1/achievements" -Role "TEACHER" -Input "no student/mosque" -Expected 201 -Actual 0 -Note "SKIPPED"
}
$n++
Add-TestResult -Number $n -Case "Award as STUDENT" -Method "POST" -Endpoint "/api/v1/achievements" -Role "STUDENT" -Input "valid body" -Expected 403 -Actual (Invoke-ApiRequest -Method "Post" -Path "/api/v1/achievements" -Token $student.accessToken -Body (Test-Body @{ studentId = $studentId; mosqueId = $mosqueId; type = "MILESTONE"; title = "hax" })).Status
$n++
Add-TestResult -Number $n -Case "Award as PARENT" -Method "POST" -Endpoint "/api/v1/achievements" -Role "PARENT" -Input "valid body" -Expected 403 -Actual (Invoke-ApiRequest -Method "Post" -Path "/api/v1/achievements" -Token $parent.accessToken -Body (Test-Body @{ studentId = $studentId; mosqueId = $mosqueId; type = "MILESTONE"; title = "hax" })).Status
$n++
Add-TestResult -Number $n -Case "Award empty body" -Method "POST" -Endpoint "/api/v1/achievements" -Role "SUPER_ADMIN" -Input "{}" -Expected 400 -Actual (Invoke-ApiRequest -Method "Post" -Path "/api/v1/achievements" -Token $admin.accessToken -Body "{}").Status
$n++
Add-TestResult -Number $n -Case "Award bad type enum" -Method "POST" -Endpoint "/api/v1/achievements" -Role "SUPER_ADMIN" -Input "type=HACKATHON" -Expected 400 -Actual (Invoke-ApiRequest -Method "Post" -Path "/api/v1/achievements" -Token $admin.accessToken -Body (Test-Body @{ studentId = $studentId; mosqueId = $mosqueId; type = "HACKATHON"; title = "x" })).Status
$n++
Add-TestResult -Number $n -Case "Award title >200 chars" -Method "POST" -Endpoint "/api/v1/achievements" -Role "SUPER_ADMIN" -Input "title=201 chars" -Expected 400 -Actual (Invoke-ApiRequest -Method "Post" -Path "/api/v1/achievements" -Token $admin.accessToken -Body (Test-Body @{ studentId = $studentId; mosqueId = $mosqueId; type = "MILESTONE"; title = ("x" * 201) })).Status
$n++
Add-TestResult -Number $n -Case "Award nonexistent student" -Method "POST" -Endpoint "/api/v1/achievements" -Role "SUPER_ADMIN" -Input "random student uuid" -Expected 404 -Actual (Invoke-ApiRequest -Method "Post" -Path "/api/v1/achievements" -Token $admin.accessToken -Body (Test-Body @{ studentId = ([guid]::NewGuid().ToString()); mosqueId = $(if ($mosqueId) { $mosqueId } else { ([guid]::NewGuid().ToString()) }); type = "MILESTONE"; title = "x" })).Status
$n++
Add-TestResult -Number $n -Case "Award nonexistent mosque" -Method "POST" -Endpoint "/api/v1/achievements" -Role "SUPER_ADMIN" -Input "random mosque uuid" -Expected 404 -Actual (Invoke-ApiRequest -Method "Post" -Path "/api/v1/achievements" -Token $admin.accessToken -Body (Test-Body @{ studentId = $studentId; mosqueId = ([guid]::NewGuid().ToString()); type = "MILESTONE"; title = "x" })).Status

Show-TestResults
