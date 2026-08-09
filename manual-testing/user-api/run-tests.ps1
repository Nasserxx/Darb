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

# Target user IDs: use the student seed as a cross-user target; throwaway user for destructive cases
$my = Invoke-ApiRequest -Method "Get" -Path "/api/v1/users/me" -Token $student.accessToken
$targetId = ($my.Body | ConvertFrom-Json).data.id
Write-Host "Target user id (student@darb.app): $targetId"

$throwawayId = $null
if (-not $ReadOnly) {
    $reg = Invoke-ApiRequest -Method "Post" -Path "/api/v1/auth/register" -Body (Test-Body @{
        fullName = "Throwaway User"; email = "test.user@darb.app"; password = "Test123!";
        role = "STUDENT"
    })
    if ($reg.Status -eq 200 -or $reg.Status -eq 409) {
        $tu = Invoke-ApiLogin -Email "test.user@darb.app" -Password "Test123!"
        $throwawayId = $tu.userId
        Write-Host "Throwaway user id: $throwawayId"
    } else {
        Write-Host "Could not prepare throwaway user (status $($reg.Status)) - destructive cases will be SKIPPED."
    }
}

Write-Case "Step 1 - Auth basics"
$n++
Add-TestResult -Number $n -Case "GET /users without token" -Method "GET" -Endpoint "/api/v1/users" -Role "none" -Input "" -Expected 401 -Actual (Invoke-ApiRequest -Method "Get" -Path "/api/v1/users").Status
$n++
Add-TestResult -Number $n -Case "GET /users with garbage token" -Method "GET" -Endpoint "/api/v1/users" -Role "garbage" -Input "Bearer xyz" -Expected 401 -Actual (Invoke-ApiRequest -Method "Get" -Path "/api/v1/users" -Token "xyz").Status
$n++
Add-TestResult -Number $n -Case "GET /users/me valid token" -Method "GET" -Endpoint "/api/v1/users/me" -Role "STUDENT" -Input "student token" -Expected 200 -Actual (Invoke-ApiRequest -Method "Get" -Path "/api/v1/users/me" -Token $student.accessToken).Status

Write-Case "Step 2 - GET /users (list, paginated, SUPER_ADMIN only)"
$n++
Add-TestResult -Number $n -Case "List users" -Method "GET" -Endpoint "/api/v1/users" -Role "SUPER_ADMIN" -Input "default page" -Expected 200 -Actual (Invoke-ApiRequest -Method "Get" -Path "/api/v1/users" -Token $admin.accessToken).Status
$n++
Add-TestResult -Number $n -Case "List users paginated" -Method "GET" -Endpoint "/api/v1/users?page=0&size=2" -Role "SUPER_ADMIN" -Input "page=0&size=2" -Expected 200 -Actual (Invoke-ApiRequest -Method "Get" -Path "/api/v1/users" -Token $admin.accessToken -Query @{ page = 0; size = 2 }).Status
$n++
Add-TestResult -Number $n -Case "List as MOSQUE_ADMIN" -Method "GET" -Endpoint "/api/v1/users" -Role "MOSQUE_ADMIN" -Input "" -Expected 403 -Actual (Invoke-ApiRequest -Method "Get" -Path "/api/v1/users" -Token $mosqueAdmin.accessToken).Status
$n++
Add-TestResult -Number $n -Case "List as TEACHER" -Method "GET" -Endpoint "/api/v1/users" -Role "TEACHER" -Input "" -Expected 403 -Actual (Invoke-ApiRequest -Method "Get" -Path "/api/v1/users" -Token $teacher.accessToken).Status
$n++
Add-TestResult -Number $n -Case "List as STUDENT" -Method "GET" -Endpoint "/api/v1/users" -Role "STUDENT" -Input "" -Expected 403 -Actual (Invoke-ApiRequest -Method "Get" -Path "/api/v1/users" -Token $student.accessToken).Status
$n++
Add-TestResult -Number $n -Case "List as PARENT" -Method "GET" -Endpoint "/api/v1/users" -Role "PARENT" -Input "" -Expected 403 -Actual (Invoke-ApiRequest -Method "Get" -Path "/api/v1/users" -Token $parent.accessToken).Status

Write-Case "Step 3 - GET /users/search (SUPER_ADMIN, MOSQUE_ADMIN, TEACHER)"
$n++
Add-TestResult -Number $n -Case "Search all seeds" -Method "GET" -Endpoint "/api/v1/users/search?q=@darb.app" -Role "SUPER_ADMIN" -Input "q=@darb.app" -Expected 200 -Actual (Invoke-ApiRequest -Method "Get" -Path "/api/v1/users/search" -Token $admin.accessToken -Query @{ q = "@darb.app" }).Status
$n++
Add-TestResult -Number $n -Case "Search partial name" -Method "GET" -Endpoint "/api/v1/users/search?q=darb" -Role "SUPER_ADMIN" -Input "q=darb" -Expected 200 -Actual (Invoke-ApiRequest -Method "Get" -Path "/api/v1/users/search" -Token $admin.accessToken -Query @{ q = "darb" }).Status
$n++
Add-TestResult -Number $n -Case "Search missing q" -Method "GET" -Endpoint "/api/v1/users/search" -Role "SUPER_ADMIN" -Input "no q" -Expected 400 -Actual (Invoke-ApiRequest -Method "Get" -Path "/api/v1/users/search" -Token $admin.accessToken).Status
$n++
Add-TestResult -Number $n -Case "Search as MOSQUE_ADMIN (no mosque)" -Method "GET" -Endpoint "/api/v1/users/search?q=darb" -Role "MOSQUE_ADMIN" -Input "q=darb" -Expected 200 -Actual (Invoke-ApiRequest -Method "Get" -Path "/api/v1/users/search" -Token $mosqueAdmin.accessToken -Query @{ q = "darb" }).Status
$n++
Add-TestResult -Number $n -Case "Search as TEACHER (no mosque)" -Method "GET" -Endpoint "/api/v1/users/search?q=darb" -Role "TEACHER" -Input "q=darb" -Expected 200 -Actual (Invoke-ApiRequest -Method "Get" -Path "/api/v1/users/search" -Token $teacher.accessToken -Query @{ q = "darb" }).Status
$n++
Add-TestResult -Number $n -Case "Search as STUDENT" -Method "GET" -Endpoint "/api/v1/users/search?q=darb" -Role "STUDENT" -Input "q=darb" -Expected 403 -Actual (Invoke-ApiRequest -Method "Get" -Path "/api/v1/users/search" -Token $student.accessToken -Query @{ q = "darb" }).Status
$n++
Add-TestResult -Number $n -Case "Search as PARENT" -Method "GET" -Endpoint "/api/v1/users/search?q=darb" -Role "PARENT" -Input "q=darb" -Expected 403 -Actual (Invoke-ApiRequest -Method "Get" -Path "/api/v1/users/search" -Token $parent.accessToken -Query @{ q = "darb" }).Status

Write-Case "Step 4 - GET /users/{id} (SUPER_ADMIN, MOSQUE_ADMIN, self)"
$n++
Add-TestResult -Number $n -Case "Get by id as SUPER_ADMIN" -Method "GET" -Endpoint "/api/v1/users/{id}" -Role "SUPER_ADMIN" -Input "student id" -Expected 200 -Actual (Invoke-ApiRequest -Method "Get" -Path "/api/v1/users/$targetId" -Token $admin.accessToken).Status
$n++
Add-TestResult -Number $n -Case "Get self as MOSQUE_ADMIN" -Method "GET" -Endpoint "/api/v1/users/{id}" -Role "MOSQUE_ADMIN" -Input "self id" -Expected 200 -Actual (Invoke-ApiRequest -Method "Get" -Path "/api/v1/users/$($mosqueAdmin.userId)" -Token $mosqueAdmin.accessToken).Status
$n++
Add-TestResult -Number $n -Case "Get other as MOSQUE_ADMIN (no mosque)" -Method "GET" -Endpoint "/api/v1/users/{id}" -Role "MOSQUE_ADMIN" -Input "student id" -Expected 403 -Actual (Invoke-ApiRequest -Method "Get" -Path "/api/v1/users/$targetId" -Token $mosqueAdmin.accessToken).Status
$n++
Add-TestResult -Number $n -Case "Get other as STUDENT" -Method "GET" -Endpoint "/api/v1/users/{id}" -Role "STUDENT" -Input "admin id" -Expected 403 -Actual (Invoke-ApiRequest -Method "Get" -Path "/api/v1/users/$($admin.userId)" -Token $student.accessToken).Status
$n++
Add-TestResult -Number $n -Case "Get self as STUDENT" -Method "GET" -Endpoint "/api/v1/users/{id}" -Role "STUDENT" -Input "self id" -Expected 200 -Actual (Invoke-ApiRequest -Method "Get" -Path "/api/v1/users/$targetId" -Token $student.accessToken).Status
$n++
Add-TestResult -Number $n -Case "Get with invalid UUID" -Method "GET" -Endpoint "/api/v1/users/{id}" -Role "SUPER_ADMIN" -Input "not-a-uuid" -Expected 400 -Actual (Invoke-ApiRequest -Method "Get" -Path "/api/v1/users/not-a-uuid" -Token $admin.accessToken).Status
$n++
Add-TestResult -Number $n -Case "Get with random UUID" -Method "GET" -Endpoint "/api/v1/users/{id}" -Role "SUPER_ADMIN" -Input "random uuid" -Expected 404 -Actual (Invoke-ApiRequest -Method "Get" -Path "/api/v1/users/$([guid]::NewGuid())" -Token $admin.accessToken).Status

Write-Case "Step 5 - GET /users/me (all roles)"
foreach ($roleEntry in @(
    @{ Role = "SUPER_ADMIN"; T = $admin },
    @{ Role = "MOSQUE_ADMIN"; T = $mosqueAdmin },
    @{ Role = "TEACHER"; T = $teacher },
    @{ Role = "STUDENT"; T = $student },
    @{ Role = "PARENT"; T = $parent }
)) {
    $n++
    Add-TestResult -Number $n -Case "GET /users/me" -Method "GET" -Endpoint "/api/v1/users/me" -Role $roleEntry.Role -Input "" -Expected 200 -Actual (Invoke-ApiRequest -Method "Get" -Path "/api/v1/users/me" -Token $roleEntry.T.accessToken).Status
}

Write-Case "Step 6 - PUT /users/me (all roles)"
$n++
Add-TestResult -Number $n -Case "Update own name" -Method "PUT" -Endpoint "/api/v1/users/me" -Role "STUDENT" -Input "fullName=New Name" -Expected 200 -Actual (Invoke-ApiRequest -Method "Put" -Path "/api/v1/users/me" -Token $student.accessToken -Body (Test-Body @{ fullName = "New Name" })).Status
$n++
Add-TestResult -Number $n -Case "Update name >150 chars" -Method "PUT" -Endpoint "/api/v1/users/me" -Role "STUDENT" -Input "fullName=151 chars" -Expected 400 -Actual (Invoke-ApiRequest -Method "Put" -Path "/api/v1/users/me" -Token $student.accessToken -Body (Test-Body @{ fullName = ("x" * 151) })).Status
$n++
Add-TestResult -Number $n -Case "Role change ignored" -Method "PUT" -Endpoint "/api/v1/users/me" -Role "STUDENT" -Input "role=SUPER_ADMIN in body" -Expected 200 -Actual (Invoke-ApiRequest -Method "Put" -Path "/api/v1/users/me" -Token $student.accessToken -Body (Test-Body @{ role = "SUPER_ADMIN" })).Status

Write-Case "Step 7 - PUT /users/{id} (SUPER_ADMIN only)"
$targetForWrite = if ($throwawayId) { $throwawayId } else { $targetId }
$n++
Add-TestResult -Number $n -Case "Admin update user" -Method "PUT" -Endpoint "/api/v1/users/{id}" -Role "SUPER_ADMIN" -Input "target=$targetForWrite" -Expected 200 -Actual (Invoke-ApiRequest -Method "Put" -Path "/api/v1/users/$targetForWrite" -Token $admin.accessToken -Body (Test-Body @{ fullName = "Updated By Admin" })).Status
$n++
Add-TestResult -Number $n -Case "Admin update as TEACHER" -Method "PUT" -Endpoint "/api/v1/users/{id}" -Role "TEACHER" -Input "target=$targetForWrite" -Expected 403 -Actual (Invoke-ApiRequest -Method "Put" -Path "/api/v1/users/$targetForWrite" -Token $teacher.accessToken -Body (Test-Body @{ fullName = "hax" })).Status
$n++
Add-TestResult -Number $n -Case "Admin update nonexistent" -Method "PUT" -Endpoint "/api/v1/users/{id}" -Role "SUPER_ADMIN" -Input "random uuid" -Expected 404 -Actual (Invoke-ApiRequest -Method "Put" -Path "/api/v1/users/$([guid]::NewGuid())" -Token $admin.accessToken -Body (Test-Body @{ fullName = "x" })).Status

Write-Case "Step 8 - DELETE /users/{id} (SUPER_ADMIN only)"
if (-not $ReadOnly -and $throwawayId) {
    $n++
    Add-TestResult -Number $n -Case "Deactivate user" -Method "DELETE" -Endpoint "/api/v1/users/{id}" -Role "SUPER_ADMIN" -Input "throwaway=$throwawayId" -Expected 200 -Actual (Invoke-ApiRequest -Method "Delete" -Path "/api/v1/users/$throwawayId" -Token $admin.accessToken).Status
    $n++
    Add-TestResult -Number $n -Case "Login as deactivated user" -Method "POST" -Endpoint "/api/v1/auth/login" -Role "STUDENT" -Input "test.user@darb.app" -Expected 401 -Actual (Invoke-ApiRequest -Method "Post" -Path "/api/v1/auth/login" -Body (Test-Body @{ email = "test.user@darb.app"; password = "Test123!" })).Status
} else {
    $n++
    Add-TestResult -Number $n -Case "Deactivate user" -Method "DELETE" -Endpoint "/api/v1/users/{id}" -Role "SUPER_ADMIN" -Input "throwaway" -Expected 200 -Actual 0 -Note "SKIPPED"
}
$n++
Add-TestResult -Number $n -Case "Deactivate as MOSQUE_ADMIN" -Method "DELETE" -Endpoint "/api/v1/users/{id}" -Role "MOSQUE_ADMIN" -Input "target=$targetId" -Expected 403 -Actual (Invoke-ApiRequest -Method "Delete" -Path "/api/v1/users/$targetId" -Token $mosqueAdmin.accessToken).Status
$n++
Add-TestResult -Number $n -Case "Deactivate nonexistent" -Method "DELETE" -Endpoint "/api/v1/users/{id}" -Role "SUPER_ADMIN" -Input "random uuid" -Expected 404 -Actual (Invoke-ApiRequest -Method "Delete" -Path "/api/v1/users/$([guid]::NewGuid())" -Token $admin.accessToken).Status

Show-TestResults
