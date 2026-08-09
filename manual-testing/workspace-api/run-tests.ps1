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

# ---------------------------------------------------------------------------
# Throwaway wiring (not ReadOnly): throwaway mosque + one throwaway user per
# non-super role, wired via the admin endpoints. Uses unique emails per run.
# ---------------------------------------------------------------------------
$suffix = ([guid]::NewGuid().ToString("N")).Substring(0, 8)
$mosqueId = $null
$wsStudent = $null
$wsTeacher = $null
$wsAdmin = $null
$wsParent = $null

if (-not $ReadOnly) {
    $mc = Invoke-ApiRequest -Method "Post" -Path "/api/v1/mosques" -Token $admin.accessToken -Body (Test-Body @{
        name = "Workspace Test Mosque $suffix"; city = "Test City"
    })
    if ($mc.Status -eq 201) {
        $mosqueId = ($mc.Body | ConvertFrom-Json).data.id
        Write-Host "Throwaway mosque id: $mosqueId"
    } else {
        Write-Host "Could not create throwaway mosque (status $($mc.Status)) - wiring cases will be SKIPPED."
    }
}

if (-not $ReadOnly -and $mosqueId) {
    $regStudent = Invoke-ApiRequest -Method "Post" -Path "/api/v1/auth/register" -Body (Test-Body @{
        fullName = "Workspace Student"; email = "ws.student.$suffix@darb.app"; password = "WsPass123!"; role = "STUDENT"
    })
    if ($regStudent.Status -eq 201 -or $regStudent.Status -eq 409) {
        $wsStudent = Invoke-ApiLogin -Email "ws.student.$suffix@darb.app" -Password "WsPass123!"
    }
    if ($wsStudent) {
        $sc = Invoke-ApiRequest -Method "Post" -Path "/api/v1/students" -Token $admin.accessToken -Body (Test-Body @{
            userId = $wsStudent.userId; mosqueId = $mosqueId
        })
        if ($sc.Status -eq 201) {
            $studentProfileId = ($sc.Body | ConvertFrom-Json).data.id
            Write-Host "Wired throwaway STUDENT (profile $studentProfileId)"
        } else {
            $wsStudent = $null
            Write-Host "Could not create student profile (status $($sc.Status))."
        }
    } else {
        Write-Host "Could not login throwaway student."
    }
}

if (-not $ReadOnly -and $mosqueId) {
    $regTeacher = Invoke-ApiRequest -Method "Post" -Path "/api/v1/auth/register" -Body (Test-Body @{
        fullName = "Workspace Teacher"; email = "ws.teacher.$suffix@darb.app"; password = "WsPass123!"; role = "TEACHER"
    })
    if ($regTeacher.Status -eq 201 -or $regTeacher.Status -eq 409) {
        $wsTeacher = Invoke-ApiLogin -Email "ws.teacher.$suffix@darb.app" -Password "WsPass123!"
    }
    if ($wsTeacher) {
        $tc = Invoke-ApiRequest -Method "Post" -Path "/api/v1/teachers" -Token $admin.accessToken -Body (Test-Body @{
            userId = $wsTeacher.userId; mosqueId = $mosqueId
        })
        if ($tc.Status -ne 201) {
            $wsTeacher = $null
            Write-Host "Could not create teacher profile (status $($tc.Status))."
        }
    } else {
        Write-Host "Could not login throwaway teacher."
    }
}

if (-not $ReadOnly -and $mosqueId) {
    $regAdmin = Invoke-ApiRequest -Method "Post" -Path "/api/v1/auth/register" -Body (Test-Body @{
        fullName = "Workspace Admin"; email = "ws.admin.$suffix@darb.app"; password = "WsPass123!"; role = "MOSQUE_ADMIN"
    })
    if ($regAdmin.Status -eq 201 -or $regAdmin.Status -eq 409) {
        $wsAdmin = Invoke-ApiLogin -Email "ws.admin.$suffix@darb.app" -Password "WsPass123!"
    }
    if ($wsAdmin) {
        $ac = Invoke-ApiRequest -Method "Post" -Path "/api/v1/mosque-admins" -Token $admin.accessToken -Body (Test-Body @{
            userId = $wsAdmin.userId; mosqueId = $mosqueId; permission = "FULL_ACCESS"; auditReason = "Wiring throwaway workspace admin"
        })
        if ($ac.Status -ne 201) {
            $wsAdmin = $null
            Write-Host "Could not assign mosque admin (status $($ac.Status))."
        }
    } else {
        Write-Host "Could not login throwaway mosque admin."
    }
}

if (-not $ReadOnly -and $mosqueId -and $wsStudent) {
    $regParent = Invoke-ApiRequest -Method "Post" -Path "/api/v1/auth/register" -Body (Test-Body @{
        fullName = "Workspace Parent"; email = "ws.parent.$suffix@darb.app"; password = "WsPass123!"; role = "PARENT"
    })
    if ($regParent.Status -eq 201 -or $regParent.Status -eq 409) {
        $wsParent = Invoke-ApiLogin -Email "ws.parent.$suffix@darb.app" -Password "WsPass123!"
    }
    if ($wsParent -and $studentProfileId) {
        $pc = Invoke-ApiRequest -Method "Post" -Path "/api/v1/parent-students" -Token $admin.accessToken -Body (Test-Body @{
            parentUserId = $wsParent.userId; studentId = $studentProfileId; relationship = "Father"; auditReason = "Wiring throwaway workspace parent"
        })
        if ($pc.Status -ne 201) {
            $wsParent = $null
            Write-Host "Could not link parent (status $($pc.Status))."
        }
    } else {
        $wsParent = $null
        Write-Host "Could not login throwaway parent."
    }
}

Write-Case "Step 1 - Auth basics"
$n++
Add-TestResult -Number $n -Case "GET /me/profile without token" -Method "GET" -Endpoint "/api/v1/me/profile" -Role "none" -Input "" -Expected 401 -Actual (Invoke-ApiRequest -Method "Get" -Path "/api/v1/me/profile").Status
$n++
Add-TestResult -Number $n -Case "GET /me/profile garbage token" -Method "GET" -Endpoint "/api/v1/me/profile" -Role "garbage" -Input "Bearer xyz" -Expected 401 -Actual (Invoke-ApiRequest -Method "Get" -Path "/api/v1/me/profile" -Token "xyz").Status

Write-Case "Step 2 - Seed role resolution"
$n++
Add-TestResult -Number $n -Case "Profile as SUPER_ADMIN" -Method "GET" -Endpoint "/api/v1/me/profile" -Role "SUPER_ADMIN" -Input "seed" -Expected 200 -Actual (Invoke-ApiRequest -Method "Get" -Path "/api/v1/me/profile" -Token $admin.accessToken).Status
$n++
Add-TestResult -Number $n -Case "Profile as seed MOSQUE_ADMIN (unwired)" -Method "GET" -Endpoint "/api/v1/me/profile" -Role "MOSQUE_ADMIN" -Input "no mosque_admins row" -Expected 404 -Actual (Invoke-ApiRequest -Method "Get" -Path "/api/v1/me/profile" -Token $mosqueAdmin.accessToken).Status -Note "gap: no profile wiring in seed"
$n++
Add-TestResult -Number $n -Case "Profile as seed TEACHER (unwired)" -Method "GET" -Endpoint "/api/v1/me/profile" -Role "TEACHER" -Input "no teachers row" -Expected 404 -Actual (Invoke-ApiRequest -Method "Get" -Path "/api/v1/me/profile" -Token $teacher.accessToken).Status -Note "gap: no profile wiring in seed"
$n++
Add-TestResult -Number $n -Case "Profile as seed STUDENT (unwired)" -Method "GET" -Endpoint "/api/v1/me/profile" -Role "STUDENT" -Input "no students row" -Expected 404 -Actual (Invoke-ApiRequest -Method "Get" -Path "/api/v1/me/profile" -Token $student.accessToken).Status -Note "gap: no profile wiring in seed"
$n++
Add-TestResult -Number $n -Case "Profile as seed PARENT (unwired)" -Method "GET" -Endpoint "/api/v1/me/profile" -Role "PARENT" -Input "no parent_student rows" -Expected 404 -Actual (Invoke-ApiRequest -Method "Get" -Path "/api/v1/me/profile" -Token $parent.accessToken).Status -Note "gap: no profile wiring in seed"

Write-Case "Step 3 - Wired throwaway roles (200)"
if ($wsStudent) {
    $n++
    Add-TestResult -Number $n -Case "Profile as wired STUDENT" -Method "GET" -Endpoint "/api/v1/me/profile" -Role "STUDENT" -Input "throwaway wired" -Expected 200 -Actual (Invoke-ApiRequest -Method "Get" -Path "/api/v1/me/profile" -Token $wsStudent.accessToken).Status
} else {
    $n++
    Add-TestResult -Number $n -Case "Profile as wired STUDENT" -Method "GET" -Endpoint "/api/v1/me/profile" -Role "STUDENT" -Input "throwaway wired" -Expected 200 -Actual 0 -Note "SKIPPED"
}
if ($wsTeacher) {
    $n++
    Add-TestResult -Number $n -Case "Profile as wired TEACHER" -Method "GET" -Endpoint "/api/v1/me/profile" -Role "TEACHER" -Input "throwaway wired" -Expected 200 -Actual (Invoke-ApiRequest -Method "Get" -Path "/api/v1/me/profile" -Token $wsTeacher.accessToken).Status
} else {
    $n++
    Add-TestResult -Number $n -Case "Profile as wired TEACHER" -Method "GET" -Endpoint "/api/v1/me/profile" -Role "TEACHER" -Input "throwaway wired" -Expected 200 -Actual 0 -Note "SKIPPED"
}
if ($wsAdmin) {
    $n++
    Add-TestResult -Number $n -Case "Profile as wired MOSQUE_ADMIN" -Method "GET" -Endpoint "/api/v1/me/profile" -Role "MOSQUE_ADMIN" -Input "throwaway wired" -Expected 200 -Actual (Invoke-ApiRequest -Method "Get" -Path "/api/v1/me/profile" -Token $wsAdmin.accessToken).Status
} else {
    $n++
    Add-TestResult -Number $n -Case "Profile as wired MOSQUE_ADMIN" -Method "GET" -Endpoint "/api/v1/me/profile" -Role "MOSQUE_ADMIN" -Input "throwaway wired" -Expected 200 -Actual 0 -Note "SKIPPED"
}
if ($wsParent) {
    $n++
    Add-TestResult -Number $n -Case "Profile as wired PARENT" -Method "GET" -Endpoint "/api/v1/me/profile" -Role "PARENT" -Input "throwaway wired" -Expected 200 -Actual (Invoke-ApiRequest -Method "Get" -Path "/api/v1/me/profile" -Token $wsParent.accessToken).Status
} else {
    $n++
    Add-TestResult -Number $n -Case "Profile as wired PARENT" -Method "GET" -Endpoint "/api/v1/me/profile" -Role "PARENT" -Input "throwaway wired" -Expected 200 -Actual 0 -Note "SKIPPED"
}

Show-TestResults
