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
# Existing circle, mosque and teacher (needed for success paths)
$circleId = $null
$mosqueId = $null
$teacherId = $null
$res = Invoke-ApiRequest -Method "Get" -Path "/api/v1/circles" -Token $admin.accessToken
if ($res.Status -eq 200) {
    $content = @()
    try { $content = @(($res.Body | ConvertFrom-Json).data.content) } catch { }
    if ($content.Count -gt 0) { $circleId = [string]$content[0].id }
}
$res = Invoke-ApiRequest -Method "Get" -Path "/api/v1/mosques" -Token $admin.accessToken
if ($res.Status -eq 200) {
    $content = @()
    try { $content = @(($res.Body | ConvertFrom-Json).data.content) } catch { }
    if ($content.Count -gt 0) { $mosqueId = [string]$content[0].id }
}
$res = Invoke-ApiRequest -Method "Get" -Path "/api/v1/teachers" -Token $admin.accessToken
if ($res.Status -eq 200) {
    $content = @()
    try { $content = @(($res.Body | ConvertFrom-Json).data.content) } catch { }
    if ($content.Count -gt 0) { $teacherId = [string]$content[0].id }
}
Write-Host "Resolved circle=$circleId mosque=$mosqueId teacher=$teacherId"

$createdCircleId = $null
$createBody = @{ mosqueId = $mosqueId; teacherId = $teacherId; name = "Throwaway Circle"; level = "BEGINNER"; type = "IN_PERSON"; capacity = 10 }

Write-Case "Step 1 - Auth basics"
$n++
Add-TestResult -Number $n -Case "GET list without token" -Method "GET" -Endpoint "/api/v1/circles" -Role "none" -Input "" -Expected 401 -Actual (Invoke-ApiRequest -Method "Get" -Path "/api/v1/circles").Status
$n++
Add-TestResult -Number $n -Case "GET list with garbage token" -Method "GET" -Endpoint "/api/v1/circles" -Role "garbage" -Input "Bearer xyz" -Expected 401 -Actual (Invoke-ApiRequest -Method "Get" -Path "/api/v1/circles" -Token "xyz").Status
$n++
Add-TestResult -Number $n -Case "POST create without token" -Method "POST" -Endpoint "/api/v1/circles" -Role "none" -Input "{}" -Expected 401 -Actual (Invoke-ApiRequest -Method "Post" -Path "/api/v1/circles" -Body (Test-Body @{})).Status

Write-Case "Step 2 - GET /circles (any authenticated)"
$n++
Add-TestResult -Number $n -Case "List as SUPER_ADMIN" -Method "GET" -Endpoint "/api/v1/circles" -Role "SUPER_ADMIN" -Input "default page" -Expected 200 -Actual (Invoke-ApiRequest -Method "Get" -Path "/api/v1/circles" -Token $admin.accessToken).Status
$n++
Add-TestResult -Number $n -Case "List as MOSQUE_ADMIN" -Method "GET" -Endpoint "/api/v1/circles" -Role "MOSQUE_ADMIN" -Input "no mosque" -Expected 200 -Actual (Invoke-ApiRequest -Method "Get" -Path "/api/v1/circles" -Token $mosqueAdmin.accessToken).Status
$teacherListStatus = (Invoke-ApiRequest -Method "Get" -Path "/api/v1/circles" -Token $teacher.accessToken).Status
$teacherListNote = "no teacher profile"
if ($teacherId) { $teacherListNote = "profile exists" }
$n++
Add-TestResult -Number $n -Case "List as TEACHER" -Method "GET" -Endpoint "/api/v1/circles" -Role "TEACHER" -Input "no teacher profile" -Expected 200 -Actual $teacherListStatus -Note $teacherListNote
$n++
Add-TestResult -Number $n -Case "List as STUDENT" -Method "GET" -Endpoint "/api/v1/circles" -Role "STUDENT" -Input "no mosque" -Expected 200 -Actual (Invoke-ApiRequest -Method "Get" -Path "/api/v1/circles" -Token $student.accessToken).Status
$n++
Add-TestResult -Number $n -Case "List as PARENT" -Method "GET" -Endpoint "/api/v1/circles" -Role "PARENT" -Input "no mosque" -Expected 200 -Actual (Invoke-ApiRequest -Method "Get" -Path "/api/v1/circles" -Token $parent.accessToken).Status

Write-Case "Step 3 - GET /circles/{id} (any authenticated, mosque-scoped)"
$n++
Add-TestResult -Number $n -Case "Get invalid UUID" -Method "GET" -Endpoint "/api/v1/circles/{id}" -Role "SUPER_ADMIN" -Input "not-a-uuid" -Expected 400 -Actual (Invoke-ApiRequest -Method "Get" -Path "/api/v1/circles/not-a-uuid" -Token $admin.accessToken).Status
$n++
Add-TestResult -Number $n -Case "Get random UUID" -Method "GET" -Endpoint "/api/v1/circles/{id}" -Role "SUPER_ADMIN" -Input "random" -Expected 404 -Actual (Invoke-ApiRequest -Method "Get" -Path "/api/v1/circles/$([guid]::NewGuid())" -Token $admin.accessToken).Status
$n++
Add-TestResult -Number $n -Case "Get random as PARENT" -Method "GET" -Endpoint "/api/v1/circles/{id}" -Role "PARENT" -Input "random" -Expected 404 -Actual (Invoke-ApiRequest -Method "Get" -Path "/api/v1/circles/$([guid]::NewGuid())" -Token $parent.accessToken).Status
$n++
Add-TestResult -Number $n -Case "Get random as TEACHER" -Method "GET" -Endpoint "/api/v1/circles/{id}" -Role "TEACHER" -Input "random" -Expected 404 -Actual (Invoke-ApiRequest -Method "Get" -Path "/api/v1/circles/$([guid]::NewGuid())" -Token $teacher.accessToken).Status
$n++
Add-TestResult -Number $n -Case "Get random as STUDENT" -Method "GET" -Endpoint "/api/v1/circles/{id}" -Role "STUDENT" -Input "random" -Expected 404 -Actual (Invoke-ApiRequest -Method "Get" -Path "/api/v1/circles/$([guid]::NewGuid())" -Token $student.accessToken).Status
$n++
Add-TestResult -Number $n -Case "Get random as MOSQUE_ADMIN" -Method "GET" -Endpoint "/api/v1/circles/{id}" -Role "MOSQUE_ADMIN" -Input "random" -Expected 404 -Actual (Invoke-ApiRequest -Method "Get" -Path "/api/v1/circles/$([guid]::NewGuid())" -Token $mosqueAdmin.accessToken).Status
if ($circleId) {
    $n++
    Add-TestResult -Number $n -Case "Get existing as SUPER_ADMIN" -Method "GET" -Endpoint "/api/v1/circles/{id}" -Role "SUPER_ADMIN" -Input "existing" -Expected 200 -Actual (Invoke-ApiRequest -Method "Get" -Path "/api/v1/circles/$circleId" -Token $admin.accessToken).Status
    $n++
    Add-TestResult -Number $n -Case "Get existing as MOSQUE_ADMIN" -Method "GET" -Endpoint "/api/v1/circles/{id}" -Role "MOSQUE_ADMIN" -Input "existing" -Expected 403 -Actual (Invoke-ApiRequest -Method "Get" -Path "/api/v1/circles/$circleId" -Token $mosqueAdmin.accessToken).Status
    $n++
    Add-TestResult -Number $n -Case "Get existing as TEACHER" -Method "GET" -Endpoint "/api/v1/circles/{id}" -Role "TEACHER" -Input "existing" -Expected 403 -Actual (Invoke-ApiRequest -Method "Get" -Path "/api/v1/circles/$circleId" -Token $teacher.accessToken).Status
    $n++
    Add-TestResult -Number $n -Case "Get existing as STUDENT" -Method "GET" -Endpoint "/api/v1/circles/{id}" -Role "STUDENT" -Input "existing" -Expected 403 -Actual (Invoke-ApiRequest -Method "Get" -Path "/api/v1/circles/$circleId" -Token $student.accessToken).Status
    $n++
    Add-TestResult -Number $n -Case "Get existing as PARENT" -Method "GET" -Endpoint "/api/v1/circles/{id}" -Role "PARENT" -Input "existing" -Expected 403 -Actual (Invoke-ApiRequest -Method "Get" -Path "/api/v1/circles/$circleId" -Token $parent.accessToken).Status
} else {
    $n++
    Add-TestResult -Number $n -Case "Get existing as SUPER_ADMIN" -Method "GET" -Endpoint "/api/v1/circles/{id}" -Role "SUPER_ADMIN" -Input "existing" -Expected 200 -Actual 0 -Note "SKIPPED"
    $n++
    Add-TestResult -Number $n -Case "Get existing as MOSQUE_ADMIN" -Method "GET" -Endpoint "/api/v1/circles/{id}" -Role "MOSQUE_ADMIN" -Input "existing" -Expected 403 -Actual 0 -Note "SKIPPED"
    $n++
    Add-TestResult -Number $n -Case "Get existing as TEACHER" -Method "GET" -Endpoint "/api/v1/circles/{id}" -Role "TEACHER" -Input "existing" -Expected 403 -Actual 0 -Note "SKIPPED"
    $n++
    Add-TestResult -Number $n -Case "Get existing as STUDENT" -Method "GET" -Endpoint "/api/v1/circles/{id}" -Role "STUDENT" -Input "existing" -Expected 403 -Actual 0 -Note "SKIPPED"
    $n++
    Add-TestResult -Number $n -Case "Get existing as PARENT" -Method "GET" -Endpoint "/api/v1/circles/{id}" -Role "PARENT" -Input "existing" -Expected 403 -Actual 0 -Note "SKIPPED"
}

Write-Case "Step 4 - POST /circles (SUPER_ADMIN, MOSQUE_ADMIN)"
$n++
Add-TestResult -Number $n -Case "Create empty body" -Method "POST" -Endpoint "/api/v1/circles" -Role "SUPER_ADMIN" -Input "{}" -Expected 400 -Actual (Invoke-ApiRequest -Method "Post" -Path "/api/v1/circles" -Token $admin.accessToken -Body (Test-Body @{})).Status
$n++
Add-TestResult -Number $n -Case "Create unknown mosque" -Method "POST" -Endpoint "/api/v1/circles" -Role "SUPER_ADMIN" -Input "random mosque" -Expected 404 -Actual (Invoke-ApiRequest -Method "Post" -Path "/api/v1/circles" -Token $admin.accessToken -Body (Test-Body @{ mosqueId = [guid]::NewGuid(); teacherId = [guid]::NewGuid(); name = "Test"; level = "BEGINNER"; type = "IN_PERSON" })).Status
$n++
Add-TestResult -Number $n -Case "Create as MOSQUE_ADMIN (unknown mosque)" -Method "POST" -Endpoint "/api/v1/circles" -Role "MOSQUE_ADMIN" -Input "random mosque" -Expected 404 -Actual (Invoke-ApiRequest -Method "Post" -Path "/api/v1/circles" -Token $mosqueAdmin.accessToken -Body (Test-Body @{ mosqueId = [guid]::NewGuid(); teacherId = [guid]::NewGuid(); name = "Test"; level = "BEGINNER"; type = "IN_PERSON" })).Status
$n++
Add-TestResult -Number $n -Case "Create as TEACHER" -Method "POST" -Endpoint "/api/v1/circles" -Role "TEACHER" -Input "{}" -Expected 403 -Actual (Invoke-ApiRequest -Method "Post" -Path "/api/v1/circles" -Token $teacher.accessToken -Body (Test-Body @{})).Status
$n++
Add-TestResult -Number $n -Case "Create as STUDENT" -Method "POST" -Endpoint "/api/v1/circles" -Role "STUDENT" -Input "{}" -Expected 403 -Actual (Invoke-ApiRequest -Method "Post" -Path "/api/v1/circles" -Token $student.accessToken -Body (Test-Body @{})).Status
$n++
Add-TestResult -Number $n -Case "Create as PARENT" -Method "POST" -Endpoint "/api/v1/circles" -Role "PARENT" -Input "{}" -Expected 403 -Actual (Invoke-ApiRequest -Method "Post" -Path "/api/v1/circles" -Token $parent.accessToken -Body (Test-Body @{})).Status
if ($mosqueId) {
    $n++
    Add-TestResult -Number $n -Case "Create as MOSQUE_ADMIN (no assignment)" -Method "POST" -Endpoint "/api/v1/circles" -Role "MOSQUE_ADMIN" -Input "existing mosque" -Expected 403 -Actual (Invoke-ApiRequest -Method "Post" -Path "/api/v1/circles" -Token $mosqueAdmin.accessToken -Body (Test-Body $createBody)).Status
} else {
    $n++
    Add-TestResult -Number $n -Case "Create as MOSQUE_ADMIN (no assignment)" -Method "POST" -Endpoint "/api/v1/circles" -Role "MOSQUE_ADMIN" -Input "existing mosque" -Expected 403 -Actual 0 -Note "SKIPPED"
}
if (-not $ReadOnly -and $mosqueId -and $teacherId) {
    $resp = Invoke-ApiRequest -Method "Post" -Path "/api/v1/circles" -Token $admin.accessToken -Body (Test-Body $createBody)
    $n++
    Add-TestResult -Number $n -Case "Create circle success" -Method "POST" -Endpoint "/api/v1/circles" -Role "SUPER_ADMIN" -Input "mosque+teacher" -Expected 201 -Actual $resp.Status
    if ($resp.Status -eq 201) {
        $createdCircleId = [string](($resp.Body | ConvertFrom-Json).data.id)
        Write-Host "Created circle id: $createdCircleId"
    }
} else {
    $n++
    Add-TestResult -Number $n -Case "Create circle success" -Method "POST" -Endpoint "/api/v1/circles" -Role "SUPER_ADMIN" -Input "mosque+teacher" -Expected 201 -Actual 0 -Note "SKIPPED"
}

Write-Case "Step 5 - PUT /circles/{id} (SUPER_ADMIN, MOSQUE_ADMIN)"
$n++
Add-TestResult -Number $n -Case "Update name too long" -Method "PUT" -Endpoint "/api/v1/circles/{id}" -Role "SUPER_ADMIN" -Input "name=201 chars" -Expected 400 -Actual (Invoke-ApiRequest -Method "Put" -Path "/api/v1/circles/$([guid]::NewGuid())" -Token $admin.accessToken -Body (Test-Body @{ name = ("x" * 201) })).Status
$n++
Add-TestResult -Number $n -Case "Update invalid UUID" -Method "PUT" -Endpoint "/api/v1/circles/{id}" -Role "SUPER_ADMIN" -Input "not-a-uuid" -Expected 400 -Actual (Invoke-ApiRequest -Method "Put" -Path "/api/v1/circles/not-a-uuid" -Token $admin.accessToken -Body (Test-Body @{ name = "Renamed" })).Status
$n++
Add-TestResult -Number $n -Case "Update random UUID" -Method "PUT" -Endpoint "/api/v1/circles/{id}" -Role "SUPER_ADMIN" -Input "random" -Expected 404 -Actual (Invoke-ApiRequest -Method "Put" -Path "/api/v1/circles/$([guid]::NewGuid())" -Token $admin.accessToken -Body (Test-Body @{ name = "Renamed" })).Status
$n++
Add-TestResult -Number $n -Case "Update as TEACHER" -Method "PUT" -Endpoint "/api/v1/circles/{id}" -Role "TEACHER" -Input "random" -Expected 403 -Actual (Invoke-ApiRequest -Method "Put" -Path "/api/v1/circles/$([guid]::NewGuid())" -Token $teacher.accessToken -Body (Test-Body @{ name = "Renamed" })).Status
$n++
Add-TestResult -Number $n -Case "Update as STUDENT" -Method "PUT" -Endpoint "/api/v1/circles/{id}" -Role "STUDENT" -Input "random" -Expected 403 -Actual (Invoke-ApiRequest -Method "Put" -Path "/api/v1/circles/$([guid]::NewGuid())" -Token $student.accessToken -Body (Test-Body @{ name = "Renamed" })).Status
$n++
Add-TestResult -Number $n -Case "Update as PARENT" -Method "PUT" -Endpoint "/api/v1/circles/{id}" -Role "PARENT" -Input "random" -Expected 403 -Actual (Invoke-ApiRequest -Method "Put" -Path "/api/v1/circles/$([guid]::NewGuid())" -Token $parent.accessToken -Body (Test-Body @{ name = "Renamed" })).Status
$targetCircle = if ($createdCircleId) { $createdCircleId } else { $circleId }
if ($targetCircle) {
    $n++
    Add-TestResult -Number $n -Case "Update as MOSQUE_ADMIN (no mosque)" -Method "PUT" -Endpoint "/api/v1/circles/{id}" -Role "MOSQUE_ADMIN" -Input "existing circle" -Expected 403 -Actual (Invoke-ApiRequest -Method "Put" -Path "/api/v1/circles/$targetCircle" -Token $mosqueAdmin.accessToken -Body (Test-Body @{ name = "Renamed" })).Status
    if ($createdCircleId) {
        $n++
        Add-TestResult -Number $n -Case "Update circle success" -Method "PUT" -Endpoint "/api/v1/circles/{id}" -Role "SUPER_ADMIN" -Input "created circle" -Expected 200 -Actual (Invoke-ApiRequest -Method "Put" -Path "/api/v1/circles/$createdCircleId" -Token $admin.accessToken -Body (Test-Body @{ status = "ACTIVE"; capacity = 20 })).Status
    } else {
        $n++
        Add-TestResult -Number $n -Case "Update circle success" -Method "PUT" -Endpoint "/api/v1/circles/{id}" -Role "SUPER_ADMIN" -Input "created circle" -Expected 200 -Actual 0 -Note "SKIPPED"
    }
} else {
    $n++
    Add-TestResult -Number $n -Case "Update as MOSQUE_ADMIN (no mosque)" -Method "PUT" -Endpoint "/api/v1/circles/{id}" -Role "MOSQUE_ADMIN" -Input "existing circle" -Expected 403 -Actual 0 -Note "SKIPPED"
    $n++
    Add-TestResult -Number $n -Case "Update circle success" -Method "PUT" -Endpoint "/api/v1/circles/{id}" -Role "SUPER_ADMIN" -Input "created circle" -Expected 200 -Actual 0 -Note "SKIPPED"
}

Write-Case "Step 6 - DELETE /circles/{id} (SUPER_ADMIN, MOSQUE_ADMIN)"
$n++
Add-TestResult -Number $n -Case "Delete random UUID" -Method "DELETE" -Endpoint "/api/v1/circles/{id}" -Role "SUPER_ADMIN" -Input "random" -Expected 404 -Actual (Invoke-ApiRequest -Method "Delete" -Path "/api/v1/circles/$([guid]::NewGuid())" -Token $admin.accessToken).Status
$n++
Add-TestResult -Number $n -Case "Delete invalid UUID" -Method "DELETE" -Endpoint "/api/v1/circles/{id}" -Role "SUPER_ADMIN" -Input "not-a-uuid" -Expected 400 -Actual (Invoke-ApiRequest -Method "Delete" -Path "/api/v1/circles/not-a-uuid" -Token $admin.accessToken).Status
$n++
Add-TestResult -Number $n -Case "Delete as TEACHER" -Method "DELETE" -Endpoint "/api/v1/circles/{id}" -Role "TEACHER" -Input "random" -Expected 403 -Actual (Invoke-ApiRequest -Method "Delete" -Path "/api/v1/circles/$([guid]::NewGuid())" -Token $teacher.accessToken).Status
$n++
Add-TestResult -Number $n -Case "Delete as STUDENT" -Method "DELETE" -Endpoint "/api/v1/circles/{id}" -Role "STUDENT" -Input "random" -Expected 403 -Actual (Invoke-ApiRequest -Method "Delete" -Path "/api/v1/circles/$([guid]::NewGuid())" -Token $student.accessToken).Status
$n++
Add-TestResult -Number $n -Case "Delete as PARENT" -Method "DELETE" -Endpoint "/api/v1/circles/{id}" -Role "PARENT" -Input "random" -Expected 403 -Actual (Invoke-ApiRequest -Method "Delete" -Path "/api/v1/circles/$([guid]::NewGuid())" -Token $parent.accessToken).Status
if ($targetCircle) {
    $n++
    Add-TestResult -Number $n -Case "Delete as MOSQUE_ADMIN (no mosque)" -Method "DELETE" -Endpoint "/api/v1/circles/{id}" -Role "MOSQUE_ADMIN" -Input "existing circle" -Expected 403 -Actual (Invoke-ApiRequest -Method "Delete" -Path "/api/v1/circles/$targetCircle" -Token $mosqueAdmin.accessToken).Status
    if ($createdCircleId -and -not $ReadOnly) {
        $n++
        Add-TestResult -Number $n -Case "Delete created circle" -Method "DELETE" -Endpoint "/api/v1/circles/{id}" -Role "SUPER_ADMIN" -Input "created circle" -Expected 200 -Actual (Invoke-ApiRequest -Method "Delete" -Path "/api/v1/circles/$createdCircleId" -Token $admin.accessToken).Status
    } else {
        $n++
        Add-TestResult -Number $n -Case "Delete created circle" -Method "DELETE" -Endpoint "/api/v1/circles/{id}" -Role "SUPER_ADMIN" -Input "created circle" -Expected 200 -Actual 0 -Note "SKIPPED"
    }
} else {
    $n++
    Add-TestResult -Number $n -Case "Delete as MOSQUE_ADMIN (no mosque)" -Method "DELETE" -Endpoint "/api/v1/circles/{id}" -Role "MOSQUE_ADMIN" -Input "existing circle" -Expected 403 -Actual 0 -Note "SKIPPED"
    $n++
    Add-TestResult -Number $n -Case "Delete created circle" -Method "DELETE" -Endpoint "/api/v1/circles/{id}" -Role "SUPER_ADMIN" -Input "created circle" -Expected 200 -Actual 0 -Note "SKIPPED"
}

Show-TestResults
