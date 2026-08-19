[CmdletBinding()]
param(
    [string]$KeycloakBaseUrl = "http://localhost:8081",
    [string]$ApiBaseUrl = "http://localhost:8080/ProjetoSpringBoot",
    [string]$Realm = "projeto-springboot",
    [string]$ClientId = "projeto-springboot-api",
    [string]$UserName = "ricardo",
    [string]$AdminUserName = "admin-api",
    [SecureString]$UserPassword,
    [SecureString]$AdminPassword,
    [switch]$NoLog
)

$ErrorActionPreference = "Stop"
$script:Passed = 0
$script:Failed = 0
$script:LogStarted = $false

function Write-Section {
    param([string]$Title)

    Write-Host ""
    Write-Host ("=" * 64) -ForegroundColor Cyan
    Write-Host $Title -ForegroundColor Cyan
    Write-Host ("=" * 64) -ForegroundColor Cyan
}

function Write-TestResult {
    param(
        [string]$Name,
        [bool]$Success,
        [string]$Details
    )

    if ($Success) {
        $script:Passed++
        Write-Host "[OK]    $Name" -ForegroundColor Green
    }
    else {
        $script:Failed++
        Write-Host "[FALHA] $Name" -ForegroundColor Red
    }

    if ($Details) {
        Write-Host "        $Details" -ForegroundColor DarkGray
    }
}

function ConvertTo-PlainText {
    param([SecureString]$SecureValue)

    return [System.Net.NetworkCredential]::new(
        "",
        $SecureValue
    ).Password
}

function Invoke-TestRequest {
    param(
        [string]$Uri,
        [hashtable]$Headers = @{}
    )

    try {
        $response = Invoke-WebRequest `
            -Method Get `
            -Uri $Uri `
            -Headers $Headers `
            -UseBasicParsing

        return [pscustomobject]@{
            StatusCode = [int]$response.StatusCode
            Content = [string]$response.Content
            Error = $null
        }
    }
    catch {
        $statusCode = 0
        $content = $_.ErrorDetails.Message

        if ($_.Exception.Response) {
            try {
                $statusCode = [int]$_.Exception.Response.StatusCode
            }
            catch {
                $statusCode = 0
            }
        }

        return [pscustomobject]@{
            StatusCode = $statusCode
            Content = [string]$content
            Error = $_.Exception.Message
        }
    }
}

function Get-KeycloakToken {
    param(
        [string]$Username,
        [SecureString]$Password
    )

    $plainPassword = ConvertTo-PlainText $Password

    try {
        $response = Invoke-RestMethod `
            -Method Post `
            -Uri "$KeycloakBaseUrl/realms/$Realm/protocol/openid-connect/token" `
            -ContentType "application/x-www-form-urlencoded" `
            -Body @{
                grant_type = "password"
                client_id = $ClientId
                username = $Username
                password = $plainPassword
            }

        return [string]$response.access_token
    }
    finally {
        $plainPassword = $null
    }
}

function Test-ExpectedStatus {
    param(
        [string]$Name,
        [pscustomobject]$Response,
        [int]$ExpectedStatus
    )

    $success = $Response.StatusCode -eq $ExpectedStatus
    $details = "Esperado: $ExpectedStatus | Recebido: $($Response.StatusCode)"

    Write-TestResult `
        -Name $Name `
        -Success $success `
        -Details $details

    if (-not $success -and $Response.Content) {
        Write-Host "        Corpo: $($Response.Content)" -ForegroundColor Yellow
    }
}

try {
    Write-Section "TESTES OAUTH 2.0 + KEYCLOAK"

    Write-Host "Keycloak: $KeycloakBaseUrl"
    Write-Host "Realm:    $Realm"
    Write-Host "Client:   $ClientId"
    Write-Host "API:      $ApiBaseUrl"

    if (-not $NoLog) {
        $logDirectory = Join-Path $PSScriptRoot "logs"
        New-Item -ItemType Directory -Path $logDirectory -Force | Out-Null

        $timestamp = Get-Date -Format "yyyyMMdd-HHmmss"
        $logFile = Join-Path $logDirectory "oauth2-$timestamp.log"

        Start-Transcript -Path $logFile | Out-Null
        $script:LogStarted = $true
        Write-Host "Log:      $logFile"
    }

    if (-not $UserPassword) {
        $UserPassword = Read-Host `
            "Senha do usuario '$UserName'" `
            -AsSecureString
    }

    if (-not $AdminPassword) {
        $AdminPassword = Read-Host `
            "Senha do usuario '$AdminUserName'" `
            -AsSecureString
    }

    Write-Section "1. DISPONIBILIDADE"

    $keycloakMetadata = Invoke-TestRequest `
        -Uri "$KeycloakBaseUrl/realms/$Realm/.well-known/openid-configuration"

    Test-ExpectedStatus `
        -Name "Keycloak e realm estao respondendo" `
        -Response $keycloakMetadata `
        -ExpectedStatus 200

    $apiWithoutToken = Invoke-TestRequest `
        -Uri "$ApiBaseUrl/clientes"

    Test-ExpectedStatus `
        -Name "Sem token em /clientes" `
        -Response $apiWithoutToken `
        -ExpectedStatus 401

    Write-Section "2. USUARIO COM ROLE USER"

    $userToken = $null

    try {
        $userToken = Get-KeycloakToken `
            -Username $UserName `
            -Password $UserPassword

        Write-TestResult `
            -Name "Token USER obtido" `
            -Success (-not [string]::IsNullOrWhiteSpace($userToken)) `
            -Details "O token nao sera exibido nem gravado no log."
    }
    catch {
        Write-TestResult `
            -Name "Token USER obtido" `
            -Success $false `
            -Details $_.Exception.Message
    }

    if ($userToken) {
        $userHeaders = @{
            Authorization = "Bearer $userToken"
        }

        $userClientes = Invoke-TestRequest `
            -Uri "$ApiBaseUrl/clientes" `
            -Headers $userHeaders

        Test-ExpectedStatus `
            -Name "USER em /clientes" `
            -Response $userClientes `
            -ExpectedStatus 200

        $userAdmin = Invoke-TestRequest `
            -Uri "$ApiBaseUrl/admin" `
            -Headers $userHeaders

        Test-ExpectedStatus `
            -Name "USER em /admin" `
            -Response $userAdmin `
            -ExpectedStatus 403
    }

    Write-Section "3. USUARIO COM ROLE ADMIN"

    $adminToken = $null

    try {
        $adminToken = Get-KeycloakToken `
            -Username $AdminUserName `
            -Password $AdminPassword

        Write-TestResult `
            -Name "Token ADMIN obtido" `
            -Success (-not [string]::IsNullOrWhiteSpace($adminToken)) `
            -Details "O token nao sera exibido nem gravado no log."
    }
    catch {
        Write-TestResult `
            -Name "Token ADMIN obtido" `
            -Success $false `
            -Details $_.Exception.Message
    }

    if ($adminToken) {
        $adminHeaders = @{
            Authorization = "Bearer $adminToken"
        }

        $adminEndpoint = Invoke-TestRequest `
            -Uri "$ApiBaseUrl/admin" `
            -Headers $adminHeaders

        Test-ExpectedStatus `
            -Name "ADMIN em /admin" `
            -Response $adminEndpoint `
            -ExpectedStatus 200

        if ($adminEndpoint.StatusCode -eq 200) {
            Write-Host "        Resposta: $($adminEndpoint.Content)" -ForegroundColor DarkGray
        }
    }

    Write-Section "4. TOKEN INVALIDO"

    $invalidToken = Invoke-TestRequest `
        -Uri "$ApiBaseUrl/clientes" `
        -Headers @{
            Authorization = "Bearer token-invalido"
        }

    Test-ExpectedStatus `
        -Name "Token invalido em /clientes" `
        -Response $invalidToken `
        -ExpectedStatus 401

    Write-Section "RESUMO"

    Write-Host "Testes aprovados: $script:Passed" -ForegroundColor Green

    if ($script:Failed -eq 0) {
        Write-Host "Testes com falha: 0" -ForegroundColor Green
        Write-Host "Resultado final: TODOS OS TESTES PASSARAM" -ForegroundColor Green
    }
    else {
        Write-Host "Testes com falha: $script:Failed" -ForegroundColor Red
        Write-Host "Resultado final: EXISTEM FALHAS PARA ANALISAR" -ForegroundColor Red
    }
}
finally {
    $userToken = $null
    $adminToken = $null
    $UserPassword = $null
    $AdminPassword = $null

    if ($script:LogStarted) {
        Stop-Transcript | Out-Null
    }
}

if ($script:Failed -gt 0) {
    exit 1
}

exit 0
