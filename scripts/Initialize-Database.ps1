param(
    [ValidatePattern('^[A-Za-z0-9_.:-]+$')]
    [string]$DatabaseHost = 'localhost',
    [ValidateRange(1, 65535)]
    [int]$Port = 3306,
    [ValidatePattern('^[A-Za-z0-9_.@-]+$')]
    [string]$User = 'root',
    [string]$MySqlPath,
    [switch]$ConfigureApplications,
    [switch]$UseEnvironmentPassword,
    [string]$RunId = ([guid]::NewGuid().ToString('N'))
)

$ErrorActionPreference = 'Stop'
$projectRoot = Split-Path -Parent $PSScriptRoot
$statusDirectory = Join-Path $projectRoot '.local/database-init'
if ($RunId -notmatch '^[a-fA-F0-9]{32}$') { throw 'Invalid run identifier.' }
$statusPath = Join-Path $statusDirectory ($RunId + '.json')
$utf8 = New-Object System.Text.UTF8Encoding($false)
$OutputEncoding = $utf8
[Console]::OutputEncoding = $utf8
$oldMySqlPassword = $env:MYSQL_PWD
$securePassword = $null
$plainPassword = $null
$counts = @{}
$newConfigFiles = @()

function Write-Status([string]$State, [string]$Message) {
    New-Item -ItemType Directory -Path $statusDirectory -Force | Out-Null
    $status = [ordered]@{
        state = $State
        message = $Message
        database = 'fangcun_forest'
        counts = $counts
        localConfigurationCreated = @($newConfigFiles)
        updatedAt = (Get-Date).ToString('o')
    }
    [System.IO.File]::WriteAllText($statusPath, ($status | ConvertTo-Json -Depth 5), $utf8)
}

function Invoke-DatabaseSql([string]$Sql) {
    $mysqlArguments = @(
        '--protocol=TCP', "--host=$DatabaseHost", "--port=$Port", "--user=$User",
        '--default-character-set=utf8mb4', '--connect-timeout=5',
        '--batch', '--skip-column-names', '--raw'
    )
    # Windows PowerShell 5.1 can wrap native stderr as ErrorRecord objects.
    # Capture it and inspect the exit code instead of treating warnings as success.
    $previousPreference = $ErrorActionPreference
    try {
        $ErrorActionPreference = 'Continue'
        $output = @($Sql | & $script:ResolvedMySqlPath @mysqlArguments 2>&1)
        $exitCode = $LASTEXITCODE
    } finally {
        $ErrorActionPreference = $previousPreference
    }
    if ($exitCode -ne 0) {
        throw ('MySQL failed: ' + (($output | ForEach-Object { $_.ToString() }) -join [Environment]::NewLine))
    }
    return @($output | ForEach-Object { $_.ToString() } | Where-Object { $_ -ne '' })
}

function Quote-Yaml([string]$Value) {
    return "'" + $Value.Replace("'", "''") + "'"
}

try {
    if ($MySqlPath) {
        $script:ResolvedMySqlPath = (Get-Item -LiteralPath $MySqlPath).FullName
    } else {
        $mysqlCommand = Get-Command mysql.exe -ErrorAction SilentlyContinue
        if ($mysqlCommand) {
            $script:ResolvedMySqlPath = $mysqlCommand.Source
        } else {
            $mysqlRoot = Join-Path ${env:ProgramFiles} 'MySQL'
            $mysqlCandidate = Get-ChildItem -Path "$mysqlRoot/MySQL Server */bin/mysql.exe" -ErrorAction SilentlyContinue |
                Sort-Object FullName -Descending | Select-Object -First 1
            if (-not $mysqlCandidate) { throw 'mysql.exe was not found. Supply -MySqlPath with its full path.' }
            $script:ResolvedMySqlPath = $mysqlCandidate.FullName
        }
    }

    Write-Host 'Fangcun Forest database initialization' -ForegroundColor Green
    Write-Host "Server: ${DatabaseHost}:$Port   User: $User"
    Write-Host 'Existing data will be preserved. No DROP or TRUNCATE is executed.'
    if ($ConfigureApplications) {
        Write-Host 'New Git-ignored application-local.yml files will store the connection locally.'
        Write-Host 'Existing local configuration files will not be overwritten.'
    }
    Write-Host ''
    if ($UseEnvironmentPassword) {
        if ([string]::IsNullOrEmpty($env:DB_PASSWORD)) {
            throw 'Set DB_PASSWORD before using -UseEnvironmentPassword.'
        }
        $plainPassword = $env:DB_PASSWORD
    } else {
        Write-Status 'waiting_for_password' 'Enter the MySQL password in the local PowerShell window.'
        $securePassword = Read-Host 'MySQL database password (hidden)' -AsSecureString
        $credential = New-Object System.Management.Automation.PSCredential($User, $securePassword)
        $plainPassword = $credential.GetNetworkCredential().Password
    }
    # Password is passed only via the child-process environment, never CLI arguments.
    $env:MYSQL_PWD = $plainPassword
    Write-Status 'running' 'Connecting to MySQL and checking existing tables.'
    $null = Invoke-DatabaseSql 'SELECT 1;'

    $expectedTables = @('cart_item', 'category', 'product', 'product_image', 'sales_order', 'sales_order_item', 'store_user')
    $existingTables = @(Invoke-DatabaseSql "SELECT TABLE_NAME FROM information_schema.TABLES WHERE TABLE_SCHEMA = 'fangcun_forest' ORDER BY TABLE_NAME;")
    if ($existingTables.Count -eq 0) {
        $schemaPath = Join-Path $projectRoot 'database/01-schema.sql'
        $null = Invoke-DatabaseSql ([System.IO.File]::ReadAllText($schemaPath, $utf8))
        Write-Host 'Database and seven tables created.' -ForegroundColor Green
    } elseif (@(Compare-Object $expectedTables $existingTables).Count -gt 0) {
        throw 'The database contains an unexpected or partial schema. Nothing was deleted. Review it before continuing.'
    } else {
        Write-Host 'All seven tables already exist; preserving their structure and data.'
    }

    $countQuery = ($expectedTables | ForEach-Object {
        "SELECT '$_', COUNT(*) FROM fangcun_forest.$_"
    }) -join ' UNION ALL '
    $countQuery += ';'
    $beforeCounts = @(Invoke-DatabaseSql $countQuery)
    $totalRows = 0L
    foreach ($line in $beforeCounts) {
        $parts = $line -split "`t"
        $totalRows += [long]$parts[1]
    }
    $insertedSeed = $false
    if ($totalRows -eq 0) {
        $seedPath = Join-Path $projectRoot 'database/02-seed.sql'
        $null = Invoke-DatabaseSql ([System.IO.File]::ReadAllText($seedPath, $utf8))
        $insertedSeed = $true
        Write-Host 'Example categories, products and images imported.' -ForegroundColor Green
    } else {
        Write-Host 'Existing rows detected; sample data import skipped.'
    }

    $accountSeedPath = Join-Path $projectRoot 'database/03-demo-account.sql'
    if (Test-Path -LiteralPath $accountSeedPath) {
        $null = Invoke-DatabaseSql ([System.IO.File]::ReadAllText($accountSeedPath, $utf8))
        Write-Host 'Teaching login account prepared; existing account passwords were preserved.'
    }
    $adminSeedPath = Join-Path $projectRoot 'database/04-demo-admin.sql'
    if (Test-Path -LiteralPath $adminSeedPath) {
        $null = Invoke-DatabaseSql ([System.IO.File]::ReadAllText($adminSeedPath, $utf8))
        Write-Host 'Teaching administrator prepared; existing accounts and roles were preserved.'
    }

    $productImageMigration = Join-Path $projectRoot 'database/05-product-images.sql'
    if (Test-Path -LiteralPath $productImageMigration) {
        $null = Invoke-DatabaseSql ([System.IO.File]::ReadAllText($productImageMigration, $utf8))
        Write-Host 'Catalog placeholder images upgraded; custom covers and business data were preserved.'
    }

    foreach ($line in @(Invoke-DatabaseSql $countQuery)) {
        $parts = $line -split "`t"
        $counts[$parts[0]] = [long]$parts[1]
    }
    if ($counts.Count -ne 7) { throw 'Could not verify all seven tables.' }
    if ($insertedSeed -and ($counts.category -ne 12 -or $counts.product -ne 9 -or $counts.product_image -ne 2)) {
        throw 'Sample row counts do not match the expected values.'
    }

    if ($ConfigureApplications) {
        $url = "jdbc:mysql://${DatabaseHost}:$Port/fangcun_forest?useUnicode=true&characterEncoding=UTF-8&serverTimezone=Asia/Shanghai"
        $configuration = "spring:`n  datasource:`n    url: $(Quote-Yaml $url)`n    username: $(Quote-Yaml $User)`n    password: $(Quote-Yaml $plainPassword)`n"
        foreach ($module in @('forest-storefront', 'forest-admin')) {
            $configurationPath = Join-Path $projectRoot "$module/src/main/resources/application-local.yml"
            if (-not (Test-Path -LiteralPath $configurationPath)) {
                [System.IO.File]::WriteAllText($configurationPath, $configuration, $utf8)
                $newConfigFiles += "$module/src/main/resources/application-local.yml"
                Write-Host "Local database connection configured for $module."
            } else {
                Write-Host "Preserved existing local configuration for $module."
            }
        }
    }
    Write-Status 'completed' 'Database initialization and verification completed.'
    Write-Host ''
    Write-Host 'Database is ready. Verified row counts:' -ForegroundColor Green
    foreach ($table in $expectedTables) { Write-Host ('  {0}: {1}' -f $table, $counts[$table]) }
    Write-Host 'No Maven build or application startup was executed.'
} catch {
    $message = $_.Exception.Message
    Write-Status 'failed' $message
    Write-Host $message -ForegroundColor Red
    Write-Host 'No existing data was deleted.'
    exit 1
} finally {
    if ($null -eq $oldMySqlPassword) { Remove-Item Env:MYSQL_PWD -ErrorAction SilentlyContinue }
    else { $env:MYSQL_PWD = $oldMySqlPassword }
    if ($null -ne $securePassword) { $securePassword.Dispose() }
    $plainPassword = $null
    $credential = $null
    $configuration = $null
}
