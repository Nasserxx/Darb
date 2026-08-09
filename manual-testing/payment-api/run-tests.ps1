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
$mosqueId = $null
$paymentId = $null
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
$res = Invoke-ApiRequest -Method "Get" -Path "/api/v1/mosques" -Token $admin.accessToken
if ($res.Status -eq 200) {
    $items = @(($res.Body | ConvertFrom-Json).data.content)
    if ($items.Count -gt 0) { $mosqueId = $items[0].id }
}
$res = Invoke-ApiRequest -Method "Get" -Path "/api/v1/payments" -Token $admin.accessToken
if ($res.Status -eq 200) {
    $items = @(($res.Body | ConvertFrom-Json).data.content)
    if ($items.Count -gt 0) { $paymentId = $items[0].id }
}

Write-Host "Resolved: student=$anyStudentId circle=$anyCircleId mosque=$mosqueId payment=$paymentId"

Write-Case "Step 1 - Auth basics"
$n++
Add-TestResult -Number $n -Case "GET /payments without token" -Method "GET" -Endpoint "/api/v1/payments" -Role "none" -Input "" -Expected 401 -Actual (Invoke-ApiRequest -Method "Get" -Path "/api/v1/payments").Status
$n++
Add-TestResult -Number $n -Case "GET /payments garbage token" -Method "GET" -Endpoint "/api/v1/payments" -Role "garbage" -Input "Bearer xyz" -Expected 401 -Actual (Invoke-ApiRequest -Method "Get" -Path "/api/v1/payments" -Token "xyz").Status

Write-Case "Step 2 - GET /payments (list: SUPER_ADMIN/MOSQUE_ADMIN)"
$n++
Add-TestResult -Number $n -Case "List payments" -Method "GET" -Endpoint "/api/v1/payments" -Role "SUPER_ADMIN" -Input "" -Expected 200 -Actual (Invoke-ApiRequest -Method "Get" -Path "/api/v1/payments" -Token $admin.accessToken).Status
$n++
Add-TestResult -Number $n -Case "List payments (scoped)" -Method "GET" -Endpoint "/api/v1/payments" -Role "MOSQUE_ADMIN" -Input "" -Expected 200 -Actual (Invoke-ApiRequest -Method "Get" -Path "/api/v1/payments" -Token $mosqueAdmin.accessToken).Status
$n++
Add-TestResult -Number $n -Case "List payments as TEACHER" -Method "GET" -Endpoint "/api/v1/payments" -Role "TEACHER" -Input "" -Expected 403 -Actual (Invoke-ApiRequest -Method "Get" -Path "/api/v1/payments" -Token $teacher.accessToken).Status
$n++
Add-TestResult -Number $n -Case "List payments as STUDENT" -Method "GET" -Endpoint "/api/v1/payments" -Role "STUDENT" -Input "" -Expected 403 -Actual (Invoke-ApiRequest -Method "Get" -Path "/api/v1/payments" -Token $student.accessToken).Status
$n++
Add-TestResult -Number $n -Case "List payments as PARENT" -Method "GET" -Endpoint "/api/v1/payments" -Role "PARENT" -Input "" -Expected 403 -Actual (Invoke-ApiRequest -Method "Get" -Path "/api/v1/payments" -Token $parent.accessToken).Status

Write-Case "Step 3 - POST /payments (create: SUPER_ADMIN/MOSQUE_ADMIN)"
$createReady = (-not $ReadOnly) -and $anyStudentId -and $anyCircleId -and $mosqueId
if ($createReady) {
    $resp = Invoke-ApiRequest -Method "Post" -Path "/api/v1/payments" -Token $admin.accessToken -Body (Test-Body @{
        studentId = $anyStudentId; circleId = $anyCircleId; mosqueId = $mosqueId;
        amount = 150.00; discount = 15.00; amountPaid = 0.00; status = "PENDING";
        method = "CASH"; cycle = "MONTHLY"; dueDate = "2026-09-01"; notes = "manual test"
    })
    $createActual = $resp.Status
    if ($createActual -eq 201) {
        $createdId = ($resp.Body | ConvertFrom-Json).data.id
    }
} else { $createActual = 0 }
$createNote = ""
if (-not $createReady) { $createNote = "SKIPPED" }
$n++
Add-TestResult -Number $n -Case "Create payment as SUPER_ADMIN" -Method "POST" -Endpoint "/api/v1/payments" -Role "SUPER_ADMIN" -Input "student+circle+mosque" -Expected 201 -Actual $createActual -Note $createNote

$maDenyNote = ""
if (-not $mosqueId) { $maDenyNote = "SKIPPED" }
if ($mosqueId) {
    $maActual = (Invoke-ApiRequest -Method "Post" -Path "/api/v1/payments" -Token $mosqueAdmin.accessToken -Body (Test-Body @{
        studentId = $anyStudentId; circleId = $anyCircleId; mosqueId = $mosqueId;
        amount = 100.00; status = "PENDING"; method = "CASH"; cycle = "MONTHLY"; dueDate = "2026-09-01"
    })).Status
} else { $maActual = 0 }
$n++
Add-TestResult -Number $n -Case "Create payment as MOSQUE_ADMIN (no assignment)" -Method "POST" -Endpoint "/api/v1/payments" -Role "MOSQUE_ADMIN" -Input "mosque=$mosqueId" -Expected 403 -Actual $maActual -Note $maDenyNote
$n++
Add-TestResult -Number $n -Case "Create payment as TEACHER" -Method "POST" -Endpoint "/api/v1/payments" -Role "TEACHER" -Input "valid body" -Expected 403 -Actual (Invoke-ApiRequest -Method "Post" -Path "/api/v1/payments" -Token $teacher.accessToken -Body (Test-Body @{ studentId = [guid]::NewGuid(); circleId = [guid]::NewGuid(); mosqueId = [guid]::NewGuid(); amount = 10.00; dueDate = "2026-09-01" })).Status
$n++
Add-TestResult -Number $n -Case "Create payment as STUDENT" -Method "POST" -Endpoint "/api/v1/payments" -Role "STUDENT" -Input "valid body" -Expected 403 -Actual (Invoke-ApiRequest -Method "Post" -Path "/api/v1/payments" -Token $student.accessToken -Body (Test-Body @{ studentId = [guid]::NewGuid(); circleId = [guid]::NewGuid(); mosqueId = [guid]::NewGuid(); amount = 10.00; dueDate = "2026-09-01" })).Status
$n++
Add-TestResult -Number $n -Case "Create payment as PARENT" -Method "POST" -Endpoint "/api/v1/payments" -Role "PARENT" -Input "valid body" -Expected 403 -Actual (Invoke-ApiRequest -Method "Post" -Path "/api/v1/payments" -Token $parent.accessToken -Body (Test-Body @{ studentId = [guid]::NewGuid(); circleId = [guid]::NewGuid(); mosqueId = [guid]::NewGuid(); amount = 10.00; dueDate = "2026-09-01" })).Status
$n++
Add-TestResult -Number $n -Case "Create payment empty body" -Method "POST" -Endpoint "/api/v1/payments" -Role "SUPER_ADMIN" -Input "{}" -Expected 400 -Actual (Invoke-ApiRequest -Method "Post" -Path "/api/v1/payments" -Token $admin.accessToken -Body "{}").Status
$n++
Add-TestResult -Number $n -Case "Create payment invalid status enum" -Method "POST" -Endpoint "/api/v1/payments" -Role "SUPER_ADMIN" -Input "status=REFUNDED_X" -Expected 400 -Actual (Invoke-ApiRequest -Method "Post" -Path "/api/v1/payments" -Token $admin.accessToken -Body (Test-Body @{ studentId = [guid]::NewGuid(); circleId = [guid]::NewGuid(); mosqueId = [guid]::NewGuid(); amount = 10.00; status = "REFUNDED_X"; dueDate = "2026-09-01" })).Status
$n++
Add-TestResult -Number $n -Case "Create payment nonexistent student" -Method "POST" -Endpoint "/api/v1/payments" -Role "SUPER_ADMIN" -Input "random studentId" -Expected 404 -Actual (Invoke-ApiRequest -Method "Post" -Path "/api/v1/payments" -Token $admin.accessToken -Body (Test-Body @{ studentId = [guid]::NewGuid(); circleId = "00000000-0000-0000-0000-000000000001"; mosqueId = "00000000-0000-0000-0000-000000000002"; amount = 10.00; dueDate = "2026-09-01" })).Status

$missMosqueNote = ""
if (-not ($anyStudentId -and $anyCircleId)) { $missMosqueNote = "SKIPPED" }
if ($anyStudentId -and $anyCircleId) {
    $missMosqueActual = (Invoke-ApiRequest -Method "Post" -Path "/api/v1/payments" -Token $admin.accessToken -Body (Test-Body @{
        studentId = $anyStudentId; circleId = $anyCircleId; mosqueId = [guid]::NewGuid();
        amount = 10.00; status = "PENDING"; method = "CASH"; cycle = "MONTHLY"; dueDate = "2026-09-01"
    })).Status
} else { $missMosqueActual = 0 }
$n++
Add-TestResult -Number $n -Case "Create payment nonexistent mosque" -Method "POST" -Endpoint "/api/v1/payments" -Role "SUPER_ADMIN" -Input "random mosqueId" -Expected 404 -Actual $missMosqueActual -Note $missMosqueNote

# Target payment for GET/PUT: prefer existing, else the one just created
$targetId = $paymentId
if (-not $targetId) { $targetId = $createdId }

Write-Case "Step 4 - GET /payments/{id}"
$targetReady = [bool]$targetId
$targetNote = ""
if (-not $targetReady) { $targetNote = "SKIPPED" }
if ($targetReady) {
    $getActual = (Invoke-ApiRequest -Method "Get" -Path "/api/v1/payments/$targetId" -Token $admin.accessToken).Status
} else { $getActual = 0 }
$n++
Add-TestResult -Number $n -Case "Get payment by id" -Method "GET" -Endpoint "/api/v1/payments/{id}" -Role "SUPER_ADMIN" -Input "id=$targetId" -Expected 200 -Actual $getActual -Note $targetNote
if ($targetReady) {
    $getStudentActual = (Invoke-ApiRequest -Method "Get" -Path "/api/v1/payments/$targetId" -Token $student.accessToken).Status
} else { $getStudentActual = 0 }
$n++
Add-TestResult -Number $n -Case "Get payment as STUDENT (no mosque)" -Method "GET" -Endpoint "/api/v1/payments/{id}" -Role "STUDENT" -Input "id=$targetId" -Expected 403 -Actual $getStudentActual -Note $targetNote
$n++
Add-TestResult -Number $n -Case "Get payment random id" -Method "GET" -Endpoint "/api/v1/payments/{id}" -Role "SUPER_ADMIN" -Input "random uuid" -Expected 404 -Actual (Invoke-ApiRequest -Method "Get" -Path "/api/v1/payments/$([guid]::NewGuid())" -Token $admin.accessToken).Status
$n++
Add-TestResult -Number $n -Case "Get payment invalid uuid" -Method "GET" -Endpoint "/api/v1/payments/{id}" -Role "SUPER_ADMIN" -Input "not-a-uuid" -Expected 400 -Actual (Invoke-ApiRequest -Method "Get" -Path "/api/v1/payments/not-a-uuid" -Token $admin.accessToken).Status

Write-Case "Step 5 - GET /payments/mosque/{mosqueId}"
$mosqueCasesReady = [bool]$mosqueId
$mosqueNote = ""
if (-not $mosqueCasesReady) { $mosqueNote = "SKIPPED" }
if ($mosqueCasesReady) {
    $mosqActual = (Invoke-ApiRequest -Method "Get" -Path "/api/v1/payments/mosque/$mosqueId" -Token $admin.accessToken).Status
} else { $mosqActual = 0 }
$n++
Add-TestResult -Number $n -Case "Mosque payments as SUPER_ADMIN" -Method "GET" -Endpoint "/api/v1/payments/mosque/{mosqueId}" -Role "SUPER_ADMIN" -Input "mosque=$mosqueId" -Expected 200 -Actual $mosqActual -Note $mosqueNote
if ($mosqueCasesReady) {
    $mosqMaActual = (Invoke-ApiRequest -Method "Get" -Path "/api/v1/payments/mosque/$mosqueId" -Token $mosqueAdmin.accessToken).Status
} else { $mosqMaActual = 0 }
$n++
Add-TestResult -Number $n -Case "Mosque payments as MOSQUE_ADMIN (no assignment)" -Method "GET" -Endpoint "/api/v1/payments/mosque/{mosqueId}" -Role "MOSQUE_ADMIN" -Input "mosque=$mosqueId" -Expected 403 -Actual $mosqMaActual -Note $mosqueNote
$n++
Add-TestResult -Number $n -Case "Mosque payments random mosque (no check)" -Method "GET" -Endpoint "/api/v1/payments/mosque/{mosqueId}" -Role "SUPER_ADMIN" -Input "random uuid" -Expected 200 -Actual (Invoke-ApiRequest -Method "Get" -Path "/api/v1/payments/mosque/$([guid]::NewGuid())" -Token $admin.accessToken).Status
$n++
Add-TestResult -Number $n -Case "Mosque payments invalid uuid" -Method "GET" -Endpoint "/api/v1/payments/mosque/{mosqueId}" -Role "SUPER_ADMIN" -Input "not-a-uuid" -Expected 400 -Actual (Invoke-ApiRequest -Method "Get" -Path "/api/v1/payments/mosque/not-a-uuid" -Token $admin.accessToken).Status

Write-Case "Step 6 - PUT /payments/{id} (SUPER_ADMIN/MOSQUE_ADMIN)"
if ($targetReady) {
    $putAdminActual = (Invoke-ApiRequest -Method "Put" -Path "/api/v1/payments/$targetId" -Token $admin.accessToken -Body (Test-Body @{ status = "PAID"; amountPaid = 150.00; paidDate = "2026-08-09" })).Status
} else { $putAdminActual = 0 }
$n++
Add-TestResult -Number $n -Case "Update payment as SUPER_ADMIN" -Method "PUT" -Endpoint "/api/v1/payments/{id}" -Role "SUPER_ADMIN" -Input "status=PAID" -Expected 200 -Actual $putAdminActual -Note $targetNote
if ($targetReady) {
    $putMaActual = (Invoke-ApiRequest -Method "Put" -Path "/api/v1/payments/$targetId" -Token $mosqueAdmin.accessToken -Body (Test-Body @{ status = "PENDING" })).Status
} else { $putMaActual = 0 }
$n++
Add-TestResult -Number $n -Case "Update payment as MOSQUE_ADMIN (no assignment)" -Method "PUT" -Endpoint "/api/v1/payments/{id}" -Role "MOSQUE_ADMIN" -Input "id=$targetId" -Expected 403 -Actual $putMaActual -Note $targetNote
$n++
Add-TestResult -Number $n -Case "Update payment as TEACHER" -Method "PUT" -Endpoint "/api/v1/payments/{id}" -Role "TEACHER" -Input "valid body" -Expected 403 -Actual (Invoke-ApiRequest -Method "Put" -Path "/api/v1/payments/$([guid]::NewGuid())" -Token $teacher.accessToken -Body (Test-Body @{ status = "PAID" })).Status
$n++
Add-TestResult -Number $n -Case "Update payment invalid status enum" -Method "PUT" -Endpoint "/api/v1/payments/{id}" -Role "SUPER_ADMIN" -Input "status=REFUNDED_X" -Expected 400 -Actual (Invoke-ApiRequest -Method "Put" -Path "/api/v1/payments/$([guid]::NewGuid())" -Token $admin.accessToken -Body (Test-Body @{ status = "REFUNDED_X" })).Status
$n++
Add-TestResult -Number $n -Case "Update nonexistent payment" -Method "PUT" -Endpoint "/api/v1/payments/{id}" -Role "SUPER_ADMIN" -Input "random uuid" -Expected 404 -Actual (Invoke-ApiRequest -Method "Put" -Path "/api/v1/payments/$([guid]::NewGuid())" -Token $admin.accessToken -Body (Test-Body @{ status = "PAID" })).Status

Show-TestResults
