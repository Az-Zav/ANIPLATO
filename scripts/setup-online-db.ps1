<#
 One-time setup of the shared online MySQL (Aiven). Run by whoever owns the service, once.
   .\scripts\setup-online-db.ps1 -DbHost <host>.aivencloud.com -Port <port> -CaFile "C:\path\ca.pem"
 It asks for the admin (avnadmin) password and the passwords to give the app user and the test user; nothing is saved.
 It creates: database aniplato (empty), user aniplato_app (data only: no CREATE/DROP), and user aniplato_test_user
 (may create/drop only databases named aniplato_test*). Add -LoadSchema to also create the tables, and -LoadSeed for the base data.
 It refuses to touch an aniplato database that already has tables, and never runs sql/schema.sql as it is (that file drops the database).
#>
param(
  [Parameter(Mandatory)][string]$DbHost,
  [Parameter(Mandatory)][int]$Port,
  [Parameter(Mandatory)][string]$CaFile,
  [string]$AdminUser = "avnadmin",
  [switch]$LoadSchema,
  [switch]$LoadSeed
)
$ErrorActionPreference = "Stop"
$OutputEncoding = New-Object System.Text.UTF8Encoding($false)   # text piped to mysql goes out as UTF-8 (the default is ASCII, which turns n-tilde into "?")
function Plain([string]$prompt) {
  $s = Read-Host $prompt -AsSecureString
  [Runtime.InteropServices.Marshal]::PtrToStringAuto([Runtime.InteropServices.Marshal]::SecureStringToBSTR($s))
}
function Run([string]$sql, [string]$db = "") {
  $args = @("-h", $DbHost, "-P", $Port, "-u", $AdminUser, "--ssl-mode=VERIFY_CA", "--ssl-ca=$CaFile", "--default-character-set=utf8mb4")
  if ($db) { $args += $db }
  $sql | & mysql @args
  if ($LASTEXITCODE -ne 0) { throw "mysql failed" }
}
$env:MYSQL_PWD = Plain "Admin password for $AdminUser"
$appPwd  = Plain "Password to give the app user aniplato_app"
$testPwd = Plain "Password to give aniplato_test_user"
if ($appPwd -match "'" -or $testPwd -match "'") { throw "Passwords may not contain a single quote." }

$existing = (Run "SELECT COUNT(*) FROM information_schema.tables WHERE table_schema='aniplato';" | Select-Object -Last 1)
if ([int]$existing -gt 0) { throw "Database aniplato already has $existing tables. Nothing was changed." }

Run @"
CREATE DATABASE IF NOT EXISTS aniplato CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE USER IF NOT EXISTS 'aniplato_app'@'%' IDENTIFIED BY '$appPwd';
GRANT SELECT, INSERT, UPDATE, DELETE ON aniplato.* TO 'aniplato_app'@'%';
CREATE USER IF NOT EXISTS 'aniplato_test_user'@'%' IDENTIFIED BY '$testPwd';
GRANT ALL PRIVILEGES ON ``aniplato\_test%``.* TO 'aniplato_test_user'@'%';
"@
Write-Host "Database and users created."

function Online([string]$file) {
  # the same script minus the lines that would drop or recreate the database; times are Manila's
  $lines = Get-Content $file -Encoding UTF8 | Where-Object { $_ -notmatch '^\s*(DROP DATABASE|CREATE DATABASE|USE aniplato)' }
  "SET time_zone = '+08:00';`n" + ($lines -join "`n")
}
if ($LoadSchema) { Run (Online "sql/schema.sql") "aniplato"; Write-Host "Tables created." }
if ($LoadSeed)   { Run (Online "sql/seed.sql") "aniplato"; Write-Host "Base data loaded." }
Remove-Item Env:MYSQL_PWD
