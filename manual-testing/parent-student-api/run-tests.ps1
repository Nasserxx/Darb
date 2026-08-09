param(
    [string]$BaseUrl = "http://localhost:8089",
    [switch]$ReadOnly
)

. (Join-Path $PSScriptRoot "..\lib.ps1")
Set-TestConfig -BaseUrl $BaseUrl

$script:Results = [System.Collections.Generic.List[object]]::new()
$n = 0

# DELETE with X-Audit-Reason header (lib.ps1 has no header support; DELETE reads audit reason from the header only)
function Invoke-ApiDeleteWithAudit {
    param([string]$Id, [string]$Token)
    $headers = @{ Authorization = "Bearer $Token"; "X-Audit-Reason" = "manual test" }
    try {
        $resp = Invoke-WebRequest -Uri "$script:BaseUrl/api/v1/parent-students/$Id" -Method Delete -Headers $headers -UseBasicParsing -TimeoutSec 30
        return [int]$resp.StatusCode
    } catch {
        if ($_.Exception.Response) { return [int]$_.Exception.Response.StatusCode }
        return 0
    }
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

# --- Dynamic data resolution -------------------------------------------------
# Existing student + its parent invite code (needed for the join flow)
$studentId = $null
$inviteCode = $null
$res = Invoke-ApiRequest -Method "Get" -Path "/api/v1/students" -Token $admin.accessToken
if ($res.Status -eq 200) {
    $content = @()
    try { $content = @(($res.Body | ConvertFrom-Json).data.content) } catch { }
    if ($content.Count -gt 0) {
        $studentId = [string]$content[0].id
        $inviteCode = $content[0].parentInviteCode
    }
}
Write-Host "Resolved student id: $studentId (invite code: $inviteCode)"

# If a student exists but has no invite code, assign a throwaway one
if ($studentId -and -not $inviteCode -and -not $ReadOnly) {
    $code = "P" + [guid]::NewGuid().ToString("N").Substring(0, 11)
    $set = Invoke-ApiRequest -Method "Put" -Path "/api/v1/students/$studentId" -Token $admin.accessToken -Body (Test-Body @{ parentInviteCode = $code })
    if ($set.Status -eq 200) {
        $inviteCode = $code
        Write-Host "Assigned throwaway parent invite code: $inviteCode"
    }
}

# Throwaway parent user for admin-created links (never the seed parent)
$throwawayParentId = $null
if (-not $ReadOnly) {
    $reg = Invoke-ApiRequest -Method "Post" -Path "/api/v1/auth/register" -Body (Test-Body @{
        fullName = "Throwaway Parent"; email = "test.parent@darb.app"; password = "ParentTest1!"
        role = "PARENT"
    })
    if ($reg.Status -eq 200 -or $reg.Status -eq 201 -or $reg.Status -eq 409) {
        $tp = Invoke-ApiLogin -Email "test.parent@darb.app" -Password "ParentTest1!"
        if ($tp) { $throwawayParentId = $tp.userId }
    }
    if ($throwawayParentId) { Write-Host "Throwaway parent id: $throwawayParentId" }
}

$createdLinkId = $null   # link between throwaway parent + student (admin POST)
$joinedLinkId = $null    # link between seed parent + student (parent join)

Write-Case "Step 1 - Auth basics"
$n++
Add-TestResult -Number $n -Case "GET list without token" -Method "GET" -Endpoint "/api/v1/parent-students" -Role "none" -Input "" -Expected 401 -Actual (Invoke-ApiRequest -Method "Get" -Path "/api/v1/parent-students").Status
$n++
Add-TestResult -Number $n -Case "GET list with garbage token" -Method "GET" -Endpoint "/api/v1/parent-students" -Role "garbage" -Input "Bearer xyz" -Expected 401 -Actual (Invoke-ApiRequest -Method "Get" -Path "/api/v1/parent-students" -Token "xyz").Status
$n++
Add-TestResult -Number $n -Case "POST create without token" -Method "POST" -Endpoint "/api/v1/parent-students" -Role "none" -Input "{}" -Expected 401 -Actual (Invoke-ApiRequest -Method "Post" -Path "/api/v1/parent-students" -Body (Test-Body @{})).Status

Write-Case "Step 2 - GET /parent-students (SUPER_ADMIN, MOSQUE_ADMIN, PARENT)"
$n++
Add-TestResult -Number $n -Case "List as SUPER_ADMIN" -Method "GET" -Endpoint "/api/v1/parent-students" -Role "SUPER_ADMIN" -Input "default page" -Expected 200 -Actual (Invoke-ApiRequest -Method "Get" -Path "/api/v1/parent-students" -Token $admin.accessToken).Status
$n++
Add-TestResult -Number $n -Case "List as MOSQUE_ADMIN" -Method "GET" -Endpoint "/api/v1/parent-students" -Role "MOSQUE_ADMIN" -Input "no mosque" -Expected 200 -Actual (Invoke-ApiRequest -Method "Get" -Path "/api/v1/parent-students" -Token $mosqueAdmin.accessToken).Status
$n++
Add-TestResult -Number $n -Case "List as PARENT" -Method "GET" -Endpoint "/api/v1/parent-students" -Role "PARENT" -Input "own links" -Expected 200 -Actual (Invoke-ApiRequest -Method "Get" -Path "/api/v1/parent-students" -Token $parent.accessToken).Status
$n++
Add-TestResult -Number $n -Case "List as TEACHER" -Method "GET" -Endpoint "/api/v1/parent-students" -Role "TEACHER" -Input "" -Expected 403 -Actual (Invoke-ApiRequest -Method "Get" -Path "/api/v1/parent-students" -Token $teacher.accessToken).Status
$n++
Add-TestResult -Number $n -Case "List as STUDENT" -Method "GET" -Endpoint "/api/v1/parent-students" -Role "STUDENT" -Input "" -Expected 403 -Actual (Invoke-ApiRequest -Method "Get" -Path "/api/v1/parent-students" -Token $student.accessToken).Status

Write-Case "Step 3 - POST /parent-students (SUPER_ADMIN, MOSQUE_ADMIN)"
$n++
Add-TestResult -Number $n -Case "Create empty body" -Method "POST" -Endpoint "/api/v1/parent-students" -Role "SUPER_ADMIN" -Input "{}" -Expected 400 -Actual (Invoke-ApiRequest -Method "Post" -Path "/api/v1/parent-students" -Token $admin.accessToken -Body (Test-Body @{})).Status
$n++
Add-TestResult -Number $n -Case "Create without audit reason" -Method "POST" -Endpoint "/api/v1/parent-students" -Role "SUPER_ADMIN" -Input "no audit" -Expected 400 -Actual (Invoke-ApiRequest -Method "Post" -Path "/api/v1/parent-students" -Token $admin.accessToken -Body (Test-Body @{ parentUserId = [guid]::NewGuid(); studentId = [guid]::NewGuid() })).Status
$n++
Add-TestResult -Number $n -Case "Create unknown student" -Method "POST" -Endpoint "/api/v1/parent-students" -Role "SUPER_ADMIN" -Input "random student" -Expected 404 -Actual (Invoke-ApiRequest -Method "Post" -Path "/api/v1/parent-students" -Token $admin.accessToken -Body (Test-Body @{ parentUserId = $parent.userId; studentId = [guid]::NewGuid(); auditReason = "manual test" })).Status
$n++
Add-TestResult -Number $n -Case "Create empty body as MOSQUE_ADMIN" -Method "POST" -Endpoint "/api/v1/parent-students" -Role "MOSQUE_ADMIN" -Input "{}" -Expected 400 -Actual (Invoke-ApiRequest -Method "Post" -Path "/api/v1/parent-students" -Token $mosqueAdmin.accessToken -Body (Test-Body @{})).Status
$n++
Add-TestResult -Number $n -Case "Create unknown student as MOSQUE_ADMIN" -Method "POST" -Endpoint "/api/v1/parent-students" -Role "MOSQUE_ADMIN" -Input "random student" -Expected 404 -Actual (Invoke-ApiRequest -Method "Post" -Path "/api/v1/parent-students" -Token $mosqueAdmin.accessToken -Body (Test-Body @{ parentUserId = $parent.userId; studentId = [guid]::NewGuid() })).Status
$n++
Add-TestResult -Number $n -Case "Create as TEACHER" -Method "POST" -Endpoint "/api/v1/parent-students" -Role "TEACHER" -Input "{}" -Expected 403 -Actual (Invoke-ApiRequest -Method "Post" -Path "/api/v1/parent-students" -Token $teacher.accessToken -Body (Test-Body @{})).Status
$n++
Add-TestResult -Number $n -Case "Create as STUDENT" -Method "POST" -Endpoint "/api/v1/parent-students" -Role "STUDENT" -Input "{}" -Expected 403 -Actual (Invoke-ApiRequest -Method "Post" -Path "/api/v1/parent-students" -Token $student.accessToken -Body (Test-Body @{})).Status
$n++
Add-TestResult -Number $n -Case "Create as PARENT" -Method "POST" -Endpoint "/api/v1/parent-students" -Role "PARENT" -Input "{}" -Expected 403 -Actual (Invoke-ApiRequest -Method "Post" -Path "/api/v1/parent-students" -Token $parent.accessToken -Body (Test-Body @{})).Status
if (-not $ReadOnly -and $studentId -and $throwawayParentId) {
    $resp = Invoke-ApiRequest -Method "Post" -Path "/api/v1/parent-students" -Token $admin.accessToken -Body (Test-Body @{
        parentUserId = $throwawayParentId; studentId = $studentId; relationship = "Father"
        isPrimary = $true; auditReason = "manual test"
    })
    $n++
    Add-TestResult -Number $n -Case "Create link (throwaway parent)" -Method "POST" -Endpoint "/api/v1/parent-students" -Role "SUPER_ADMIN" -Input "student=$studentId" -Expected 201 -Actual $resp.Status
    if ($resp.Status -eq 201) {
        $createdLinkId = [string](($resp.Body | ConvertFrom-Json).data.id)
        Write-Host "Created link id: $createdLinkId"
    }
} else {
    $n++
    Add-TestResult -Number $n -Case "Create link (throwaway parent)" -Method "POST" -Endpoint "/api/v1/parent-students" -Role "SUPER_ADMIN" -Input "student=$studentId" -Expected 201 -Actual 0 -Note "SKIPPED"
}

Write-Case "Step 4 - Parent link flow (preview, join, my-children)"
$n++
Add-TestResult -Number $n -Case "Preview invalid code" -Method "GET" -Endpoint "/api/v1/parent-students/join/preview?code=x" -Role "PARENT" -Input "code=zzzzzzzzzz" -Expected 404 -Actual (Invoke-ApiRequest -Method "Get" -Path "/api/v1/parent-students/join/preview" -Token $parent.accessToken -Query @{ code = "zzzzzzzzzz" }).Status
$n++
Add-TestResult -Number $n -Case "Preview missing code" -Method "GET" -Endpoint "/api/v1/parent-students/join/preview" -Role "PARENT" -Input "no code" -Expected 400 -Actual (Invoke-ApiRequest -Method "Get" -Path "/api/v1/parent-students/join/preview" -Token $parent.accessToken).Status
$n++
Add-TestResult -Number $n -Case "Preview as SUPER_ADMIN" -Method "GET" -Endpoint "/api/v1/parent-students/join/preview?code=x" -Role "SUPER_ADMIN" -Input "code=zzzzzzzzzz" -Expected 403 -Actual (Invoke-ApiRequest -Method "Get" -Path "/api/v1/parent-students/join/preview" -Token $admin.accessToken -Query @{ code = "zzzzzzzzzz" }).Status
if ($inviteCode) {
    $n++
    Add-TestResult -Number $n -Case "Preview valid code" -Method "GET" -Endpoint "/api/v1/parent-students/join/preview?code=*" -Role "PARENT" -Input "valid code" -Expected 200 -Actual (Invoke-ApiRequest -Method "Get" -Path "/api/v1/parent-students/join/preview" -Token $parent.accessToken -Query @{ code = $inviteCode }).Status
} else {
    $n++
    Add-TestResult -Number $n -Case "Preview valid code" -Method "GET" -Endpoint "/api/v1/parent-students/join/preview?code=*" -Role "PARENT" -Input "valid code" -Expected 200 -Actual 0 -Note "SKIPPED"
}
$n++
Add-TestResult -Number $n -Case "Join empty body" -Method "POST" -Endpoint "/api/v1/parent-students/join" -Role "PARENT" -Input "{}" -Expected 400 -Actual (Invoke-ApiRequest -Method "Post" -Path "/api/v1/parent-students/join" -Token $parent.accessToken -Body (Test-Body @{})).Status
$n++
Add-TestResult -Number $n -Case "Join invalid code" -Method "POST" -Endpoint "/api/v1/parent-students/join" -Role "PARENT" -Input "code=zzzzzzzzzz" -Expected 404 -Actual (Invoke-ApiRequest -Method "Post" -Path "/api/v1/parent-students/join" -Token $parent.accessToken -Body (Test-Body @{ inviteCode = "zzzzzzzzzz" })).Status
$n++
Add-TestResult -Number $n -Case "Join as SUPER_ADMIN" -Method "POST" -Endpoint "/api/v1/parent-students/join" -Role "SUPER_ADMIN" -Input "zzzzzzzzzz" -Expected 403 -Actual (Invoke-ApiRequest -Method "Post" -Path "/api/v1/parent-students/join" -Token $admin.accessToken -Body (Test-Body @{ inviteCode = "zzzzzzzzzz" })).Status
$n++
Add-TestResult -Number $n -Case "Join as MOSQUE_ADMIN" -Method "POST" -Endpoint "/api/v1/parent-students/join" -Role "MOSQUE_ADMIN" -Input "zzzzzzzzzz" -Expected 403 -Actual (Invoke-ApiRequest -Method "Post" -Path "/api/v1/parent-students/join" -Token $mosqueAdmin.accessToken -Body (Test-Body @{ inviteCode = "zzzzzzzzzz" })).Status
$n++
Add-TestResult -Number $n -Case "Join as TEACHER" -Method "POST" -Endpoint "/api/v1/parent-students/join" -Role "TEACHER" -Input "zzzzzzzzzz" -Expected 403 -Actual (Invoke-ApiRequest -Method "Post" -Path "/api/v1/parent-students/join" -Token $teacher.accessToken -Body (Test-Body @{ inviteCode = "zzzzzzzzzz" })).Status
$n++
Add-TestResult -Number $n -Case "Join as STUDENT" -Method "POST" -Endpoint "/api/v1/parent-students/join" -Role "STUDENT" -Input "zzzzzzzzzz" -Expected 403 -Actual (Invoke-ApiRequest -Method "Post" -Path "/api/v1/parent-students/join" -Token $student.accessToken -Body (Test-Body @{ inviteCode = "zzzzzzzzzz" })).Status
if (-not $ReadOnly -and $inviteCode) {
    $joinResp = Invoke-ApiRequest -Method "Post" -Path "/api/v1/parent-students/join" -Token $parent.accessToken -Body (Test-Body @{ inviteCode = $inviteCode })
    $n++
    Add-TestResult -Number $n -Case "Join valid code" -Method "POST" -Endpoint "/api/v1/parent-students/join" -Role "PARENT" -Input "valid code" -Expected 201 -Actual $joinResp.Status
    if ($joinResp.Status -eq 201) {
        $joinedLinkId = [string](($joinResp.Body | ConvertFrom-Json).data.id)
        Write-Host "Joined link id: $joinedLinkId"
        $n++
        Add-TestResult -Number $n -Case "Join valid code again" -Method "POST" -Endpoint "/api/v1/parent-students/join" -Role "PARENT" -Input "same code" -Expected 400 -Actual (Invoke-ApiRequest -Method "Post" -Path "/api/v1/parent-students/join" -Token $parent.accessToken -Body (Test-Body @{ inviteCode = $inviteCode })).Status
    }
} else {
    $n++
    Add-TestResult -Number $n -Case "Join valid code" -Method "POST" -Endpoint "/api/v1/parent-students/join" -Role "PARENT" -Input "valid code" -Expected 201 -Actual 0 -Note "SKIPPED"
}
$n++
Add-TestResult -Number $n -Case "My children as PARENT" -Method "GET" -Endpoint "/api/v1/parent-students/my-children" -Role "PARENT" -Input "" -Expected 200 -Actual (Invoke-ApiRequest -Method "Get" -Path "/api/v1/parent-students/my-children" -Token $parent.accessToken).Status
$n++
Add-TestResult -Number $n -Case "My children as SUPER_ADMIN" -Method "GET" -Endpoint "/api/v1/parent-students/my-children" -Role "SUPER_ADMIN" -Input "" -Expected 403 -Actual (Invoke-ApiRequest -Method "Get" -Path "/api/v1/parent-students/my-children" -Token $admin.accessToken).Status
$n++
Add-TestResult -Number $n -Case "My children as TEACHER" -Method "GET" -Endpoint "/api/v1/parent-students/my-children" -Role "TEACHER" -Input "" -Expected 403 -Actual (Invoke-ApiRequest -Method "Get" -Path "/api/v1/parent-students/my-children" -Token $teacher.accessToken).Status
$n++
Add-TestResult -Number $n -Case "My children as STUDENT" -Method "GET" -Endpoint "/api/v1/parent-students/my-children" -Role "STUDENT" -Input "" -Expected 403 -Actual (Invoke-ApiRequest -Method "Get" -Path "/api/v1/parent-students/my-children" -Token $student.accessToken).Status

Write-Case "Step 5 - GET /parent-students/{id} (SUPER_ADMIN, MOSQUE_ADMIN, PARENT)"
$n++
Add-TestResult -Number $n -Case "Get invalid UUID" -Method "GET" -Endpoint "/api/v1/parent-students/{id}" -Role "SUPER_ADMIN" -Input "not-a-uuid" -Expected 400 -Actual (Invoke-ApiRequest -Method "Get" -Path "/api/v1/parent-students/not-a-uuid" -Token $admin.accessToken).Status
$n++
Add-TestResult -Number $n -Case "Get random UUID" -Method "GET" -Endpoint "/api/v1/parent-students/{id}" -Role "SUPER_ADMIN" -Input "random" -Expected 404 -Actual (Invoke-ApiRequest -Method "Get" -Path "/api/v1/parent-students/$([guid]::NewGuid())" -Token $admin.accessToken).Status
$n++
Add-TestResult -Number $n -Case "Get random as PARENT" -Method "GET" -Endpoint "/api/v1/parent-students/{id}" -Role "PARENT" -Input "random" -Expected 404 -Actual (Invoke-ApiRequest -Method "Get" -Path "/api/v1/parent-students/$([guid]::NewGuid())" -Token $parent.accessToken).Status
$n++
Add-TestResult -Number $n -Case "Get random as TEACHER" -Method "GET" -Endpoint "/api/v1/parent-students/{id}" -Role "TEACHER" -Input "random" -Expected 403 -Actual (Invoke-ApiRequest -Method "Get" -Path "/api/v1/parent-students/$([guid]::NewGuid())" -Token $teacher.accessToken).Status
$n++
Add-TestResult -Number $n -Case "Get random as STUDENT" -Method "GET" -Endpoint "/api/v1/parent-students/{id}" -Role "STUDENT" -Input "random" -Expected 403 -Actual (Invoke-ApiRequest -Method "Get" -Path "/api/v1/parent-students/$([guid]::NewGuid())" -Token $student.accessToken).Status
$n++
Add-TestResult -Number $n -Case "Get random as MOSQUE_ADMIN" -Method "GET" -Endpoint "/api/v1/parent-students/{id}" -Role "MOSQUE_ADMIN" -Input "random" -Expected 404 -Actual (Invoke-ApiRequest -Method "Get" -Path "/api/v1/parent-students/$([guid]::NewGuid())" -Token $mosqueAdmin.accessToken).Status
if ($joinedLinkId) {
    $n++
    Add-TestResult -Number $n -Case "Get own link as PARENT" -Method "GET" -Endpoint "/api/v1/parent-students/{id}" -Role "PARENT" -Input "own link" -Expected 200 -Actual (Invoke-ApiRequest -Method "Get" -Path "/api/v1/parent-students/$joinedLinkId" -Token $parent.accessToken).Status
} else {
    $n++
    Add-TestResult -Number $n -Case "Get own link as PARENT" -Method "GET" -Endpoint "/api/v1/parent-students/{id}" -Role "PARENT" -Input "own link" -Expected 200 -Actual 0 -Note "SKIPPED"
}
if ($createdLinkId) {
    $n++
    Add-TestResult -Number $n -Case "Get other link as PARENT" -Method "GET" -Endpoint "/api/v1/parent-students/{id}" -Role "PARENT" -Input "other link" -Expected 403 -Actual (Invoke-ApiRequest -Method "Get" -Path "/api/v1/parent-students/$createdLinkId" -Token $parent.accessToken).Status
} else {
    $n++
    Add-TestResult -Number $n -Case "Get other link as PARENT" -Method "GET" -Endpoint "/api/v1/parent-students/{id}" -Role "PARENT" -Input "other link" -Expected 403 -Actual 0 -Note "SKIPPED"
}

Write-Case "Step 6 - PUT /parent-students/{id} (SUPER_ADMIN, MOSQUE_ADMIN)"
$n++
Add-TestResult -Number $n -Case "Update without audit reason" -Method "PUT" -Endpoint "/api/v1/parent-students/{id}" -Role "SUPER_ADMIN" -Input "random" -Expected 400 -Actual (Invoke-ApiRequest -Method "Put" -Path "/api/v1/parent-students/$([guid]::NewGuid())" -Token $admin.accessToken -Body (Test-Body @{ relationship = "Mother" })).Status
$n++
Add-TestResult -Number $n -Case "Update random UUID" -Method "PUT" -Endpoint "/api/v1/parent-students/{id}" -Role "SUPER_ADMIN" -Input "random + audit" -Expected 404 -Actual (Invoke-ApiRequest -Method "Put" -Path "/api/v1/parent-students/$([guid]::NewGuid())" -Token $admin.accessToken -Body (Test-Body @{ relationship = "Mother"; auditReason = "manual test" })).Status
$n++
Add-TestResult -Number $n -Case "Update invalid UUID" -Method "PUT" -Endpoint "/api/v1/parent-students/{id}" -Role "SUPER_ADMIN" -Input "not-a-uuid" -Expected 400 -Actual (Invoke-ApiRequest -Method "Put" -Path "/api/v1/parent-students/not-a-uuid" -Token $admin.accessToken -Body (Test-Body @{ relationship = "Mother"; auditReason = "manual test" })).Status
$n++
Add-TestResult -Number $n -Case "Update random as MOSQUE_ADMIN" -Method "PUT" -Endpoint "/api/v1/parent-students/{id}" -Role "MOSQUE_ADMIN" -Input "random" -Expected 404 -Actual (Invoke-ApiRequest -Method "Put" -Path "/api/v1/parent-students/$([guid]::NewGuid())" -Token $mosqueAdmin.accessToken -Body (Test-Body @{ relationship = "Mother" })).Status
$n++
Add-TestResult -Number $n -Case "Update as TEACHER" -Method "PUT" -Endpoint "/api/v1/parent-students/{id}" -Role "TEACHER" -Input "random" -Expected 403 -Actual (Invoke-ApiRequest -Method "Put" -Path "/api/v1/parent-students/$([guid]::NewGuid())" -Token $teacher.accessToken -Body (Test-Body @{ relationship = "Mother" })).Status
$n++
Add-TestResult -Number $n -Case "Update as STUDENT" -Method "PUT" -Endpoint "/api/v1/parent-students/{id}" -Role "STUDENT" -Input "random" -Expected 403 -Actual (Invoke-ApiRequest -Method "Put" -Path "/api/v1/parent-students/$([guid]::NewGuid())" -Token $student.accessToken -Body (Test-Body @{ relationship = "Mother" })).Status
$n++
Add-TestResult -Number $n -Case "Update as PARENT" -Method "PUT" -Endpoint "/api/v1/parent-students/{id}" -Role "PARENT" -Input "random" -Expected 403 -Actual (Invoke-ApiRequest -Method "Put" -Path "/api/v1/parent-students/$([guid]::NewGuid())" -Token $parent.accessToken -Body (Test-Body @{ relationship = "Mother" })).Status
if ($createdLinkId) {
    $n++
    Add-TestResult -Number $n -Case "Update as MOSQUE_ADMIN (no mosque)" -Method "PUT" -Endpoint "/api/v1/parent-students/{id}" -Role "MOSQUE_ADMIN" -Input "existing link" -Expected 403 -Actual (Invoke-ApiRequest -Method "Put" -Path "/api/v1/parent-students/$createdLinkId" -Token $mosqueAdmin.accessToken -Body (Test-Body @{ relationship = "Mother" })).Status
    $n++
    Add-TestResult -Number $n -Case "Update link success" -Method "PUT" -Endpoint "/api/v1/parent-students/{id}" -Role "SUPER_ADMIN" -Input "existing link" -Expected 200 -Actual (Invoke-ApiRequest -Method "Put" -Path "/api/v1/parent-students/$createdLinkId" -Token $admin.accessToken -Body (Test-Body @{ relationship = "Mother"; isPrimary = $false; auditReason = "manual test" })).Status
} else {
    $n++
    Add-TestResult -Number $n -Case "Update as MOSQUE_ADMIN (no mosque)" -Method "PUT" -Endpoint "/api/v1/parent-students/{id}" -Role "MOSQUE_ADMIN" -Input "existing link" -Expected 403 -Actual 0 -Note "SKIPPED"
    $n++
    Add-TestResult -Number $n -Case "Update link success" -Method "PUT" -Endpoint "/api/v1/parent-students/{id}" -Role "SUPER_ADMIN" -Input "existing link" -Expected 200 -Actual 0 -Note "SKIPPED"
}

Write-Case "Step 7 - DELETE /parent-students/{id} (SUPER_ADMIN only)"
$n++
Add-TestResult -Number $n -Case "Delete without audit reason" -Method "DELETE" -Endpoint "/api/v1/parent-students/{id}" -Role "SUPER_ADMIN" -Input "random" -Expected 400 -Actual (Invoke-ApiRequest -Method "Delete" -Path "/api/v1/parent-students/$([guid]::NewGuid())" -Token $admin.accessToken).Status
$n++
Add-TestResult -Number $n -Case "Delete random UUID" -Method "DELETE" -Endpoint "/api/v1/parent-students/{id}" -Role "SUPER_ADMIN" -Input "random + X-Audit-Reason" -Expected 404 -Actual (Invoke-ApiDeleteWithAudit -Id ([guid]::NewGuid()) -Token $admin.accessToken)
$n++
Add-TestResult -Number $n -Case "Delete invalid UUID" -Method "DELETE" -Endpoint "/api/v1/parent-students/{id}" -Role "SUPER_ADMIN" -Input "not-a-uuid" -Expected 400 -Actual (Invoke-ApiRequest -Method "Delete" -Path "/api/v1/parent-students/not-a-uuid" -Token $admin.accessToken).Status
$n++
Add-TestResult -Number $n -Case "Delete as MOSQUE_ADMIN" -Method "DELETE" -Endpoint "/api/v1/parent-students/{id}" -Role "MOSQUE_ADMIN" -Input "random" -Expected 403 -Actual (Invoke-ApiRequest -Method "Delete" -Path "/api/v1/parent-students/$([guid]::NewGuid())" -Token $mosqueAdmin.accessToken).Status
$n++
Add-TestResult -Number $n -Case "Delete as TEACHER" -Method "DELETE" -Endpoint "/api/v1/parent-students/{id}" -Role "TEACHER" -Input "random" -Expected 403 -Actual (Invoke-ApiRequest -Method "Delete" -Path "/api/v1/parent-students/$([guid]::NewGuid())" -Token $teacher.accessToken).Status
$n++
Add-TestResult -Number $n -Case "Delete as STUDENT" -Method "DELETE" -Endpoint "/api/v1/parent-students/{id}" -Role "STUDENT" -Input "random" -Expected 403 -Actual (Invoke-ApiRequest -Method "Delete" -Path "/api/v1/parent-students/$([guid]::NewGuid())" -Token $student.accessToken).Status
$n++
Add-TestResult -Number $n -Case "Delete as PARENT" -Method "DELETE" -Endpoint "/api/v1/parent-students/{id}" -Role "PARENT" -Input "random" -Expected 403 -Actual (Invoke-ApiRequest -Method "Delete" -Path "/api/v1/parent-students/$([guid]::NewGuid())" -Token $parent.accessToken).Status
if ($createdLinkId -and -not $ReadOnly) {
    $n++
    Add-TestResult -Number $n -Case "Delete created link" -Method "DELETE" -Endpoint "/api/v1/parent-students/{id}" -Role "SUPER_ADMIN" -Input "created link + audit" -Expected 200 -Actual (Invoke-ApiDeleteWithAudit -Id $createdLinkId -Token $admin.accessToken)
} else {
    $n++
    Add-TestResult -Number $n -Case "Delete created link" -Method "DELETE" -Endpoint "/api/v1/parent-students/{id}" -Role "SUPER_ADMIN" -Input "created link + audit" -Expected 200 -Actual 0 -Note "SKIPPED"
}
if ($joinedLinkId -and -not $ReadOnly) {
    $n++
    Add-TestResult -Number $n -Case "Delete joined link" -Method "DELETE" -Endpoint "/api/v1/parent-students/{id}" -Role "SUPER_ADMIN" -Input "joined link + audit" -Expected 200 -Actual (Invoke-ApiDeleteWithAudit -Id $joinedLinkId -Token $admin.accessToken)
} else {
    $n++
    Add-TestResult -Number $n -Case "Delete joined link" -Method "DELETE" -Endpoint "/api/v1/parent-students/{id}" -Role "SUPER_ADMIN" -Input "joined link + audit" -Expected 200 -Actual 0 -Note "SKIPPED"
}

Show-TestResults
