<#
.SYNOPSIS
  Creates the ANIPLATO database and its first admin in a MySQL you control (a developer tool; the app's own setup screen does this for end users).
.DESCRIPTION
  Needs: MySQL 8+ installed and running, and your password set in
  src/main/resources/db.properties. Reads url/user/password from that file.
  WARNING: reset.sql drops and recreates the `aniplato` database.
.PARAMETER Force
  Skip the confirmation prompt when the database already exists.
.PARAMETER Demo
  Also load the demo story (sql/seed-admin-demo.sql and sql/seed-demo.sql) and copy the sample photos
  from sql/demo-photos to uploads/seed, so every screen has something to show.
#>
param([switch]$Force, [switch]$Demo)

$ErrorActionPreference = 'Stop'
$root = $PSScriptRoot
$props = Join-Path $root 'src/main/resources/db.properties'
$example = Join-Path $root 'src/main/resources/db.properties.example'

# 1. db.properties ------------------------------------------------------
if (-not (Test-Path $props)) {
    Copy-Item $example $props
    Write-Host "Created $props from the example." -ForegroundColor Yellow
    Write-Host "Open it, set 'password=' to your MySQL root password, then run this script again."
    exit 1
}
$cfg = @{}
Get-Content $props | Where-Object { $_ -match '^\s*[^#\s][^=]*=' } | ForEach-Object {
    $k, $v = $_ -split '=', 2
    $cfg[$k.Trim()] = $v.Trim()
}
if (-not $cfg['password']) {
    Write-Host "db.properties has no password set. Edit it and run again." -ForegroundColor Red
    exit 1
}
if ($cfg['url'] -notmatch 'jdbc:mysql://([^:/]+)(?::(\d+))?/') {
    Write-Host "Cannot read host/port from url in db.properties." -ForegroundColor Red
    exit 1
}
$dbHost = $Matches[1]
$port = if ($Matches[2]) { $Matches[2] } else { '3306' }

# 2. Find the mysql client ---------------------------------------------
$mysql = (Get-Command mysql -ErrorAction SilentlyContinue).Source
if (-not $mysql) {
    $mysql = Get-ChildItem 'C:\Program Files\MySQL\MySQL Server *\bin\mysql.exe' -ErrorAction SilentlyContinue |
        Sort-Object FullName -Descending | Select-Object -First 1 -ExpandProperty FullName
}
if (-not $mysql) {
    Write-Host "mysql.exe not found. Install MySQL 8+ (https://dev.mysql.com/downloads/installer/) or add its bin folder to PATH." -ForegroundColor Red
    exit 1
}

$env:MYSQL_PWD = $cfg['password']   # keeps the password off the command line
$common = @('-h', $dbHost, '-P', $port, '-u', $cfg['user'], '--default-character-set=utf8mb4')

try {
    # 3. Server reachable and password correct? -------------------------
    & $mysql @common -e 'SELECT 1' 2>&1 | Out-Null
    if ($LASTEXITCODE -ne 0) {
        Write-Host "Cannot connect to MySQL at ${dbHost}:${port} as '$($cfg['user'])'." -ForegroundColor Red
        Write-Host "Check that the MySQL service is running and the password in db.properties is right."
        exit 1
    }

    # 4. Confirm before dropping an existing database -------------------
    $exists = & $mysql @common -N -e "SHOW DATABASES LIKE 'aniplato'"
    if ($exists -and -not $Force) {
        Write-Host "Database 'aniplato' already exists. Continuing DELETES all its data." -ForegroundColor Yellow
        $answer = Read-Host "Type YES to recreate it"
        if ($answer -ne 'YES') { Write-Host 'Cancelled. Nothing changed.'; exit 0 }
    }

    # 5. Load schema and seed. The file is fed to mysql's stdin as raw bytes,
    #    so UTF-8 names (Las Piñas) survive and paths with spaces are no issue.
    function Invoke-SqlFile([string]$file, [switch]$InDatabase) {
        $errFile = [IO.Path]::GetTempFileName()
        $mysqlArgs = if ($InDatabase) { $common + @('-D', 'aniplato') } else { $common }
        try {
            $p = Start-Process -FilePath $mysql -ArgumentList $mysqlArgs -NoNewWindow -Wait -PassThru `
                -RedirectStandardInput $file -RedirectStandardError $errFile
            if ($p.ExitCode -ne 0) {
                Write-Host (Get-Content $errFile -Raw) -ForegroundColor Red
                throw "$(Split-Path $file -Leaf) failed"
            }
        } finally { Remove-Item $errFile -ErrorAction SilentlyContinue }
    }
    Write-Host 'Creating the empty database...'
    Invoke-SqlFile (Join-Path $root 'sql/reset.sql')
    Write-Host 'Creating tables...'
    Invoke-SqlFile (Join-Path $root 'sql/schema.sql') -InDatabase
    Write-Host 'Creating the admin...'
    Invoke-SqlFile (Join-Path $root 'sql/seed.sql') -InDatabase
    if ($Demo) {
        Write-Host 'Loading the demo story...'
        Invoke-SqlFile (Join-Path $root 'sql/seed-demo.sql') -InDatabase
        Invoke-SqlFile (Join-Path $root 'sql/seed-admin-demo.sql') -InDatabase
        $uploads = if ($cfg['uploads.dir']) { $cfg['uploads.dir'] } else { Join-Path $root 'uploads' }
        $seedPhotos = Join-Path $uploads 'seed'
        New-Item -ItemType Directory -Force -Path $seedPhotos | Out-Null
        Copy-Item (Join-Path $root 'sql/demo-photos/*') $seedPhotos -Force
    }
}
finally {
    Remove-Item Env:\MYSQL_PWD -ErrorAction SilentlyContinue
}

Write-Host ''
Write-Host 'Database ready.' -ForegroundColor Green
Write-Host 'Sign in as admin@aniplato.test with the password Aniplato123!; the app makes you choose a new one.'
if ($Demo) {
    Write-Host 'Demo logins (password: Aniplato123!, no forced change for these)'
    Write-Host '  business@aniplato.test   donor (Sunrise Bakery)'
    Write-Host '  group@aniplato.test      food group (Kapwa Community Kitchen)'
}
Write-Host 'Then run: mvn javafx:run'
