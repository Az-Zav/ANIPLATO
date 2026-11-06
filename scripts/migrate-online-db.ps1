<#
 Applies sql/migrate-stored-file.sql to the shared online database as the owner (avnadmin). The app user cannot change the schema.
   .\scripts\migrate-online-db.ps1 -DbHost <host>.aivencloud.com -Port <port> -CaFile "C:\path\ca.pem"
 It adds the stored_file table and puts the n-tilde back in two city names (sql/repair-city-names.sql); running it twice is harmless. Take a mysqldump first.
 It asks for the avnadmin password and saves nothing.
#>
param(
  [Parameter(Mandatory)][string]$DbHost,
  [Parameter(Mandatory)][int]$Port,
  [Parameter(Mandatory)][string]$CaFile,
  [string]$AdminUser = "avnadmin"
)
$ErrorActionPreference = "Stop"
$OutputEncoding = New-Object System.Text.UTF8Encoding($false)   # text piped to mysql goes out as UTF-8 (the default is ASCII, which turns n-tilde into "?")
$s = Read-Host "Admin password for $AdminUser" -AsSecureString
$env:MYSQL_PWD = [Runtime.InteropServices.Marshal]::PtrToStringAuto([Runtime.InteropServices.Marshal]::SecureStringToBSTR($s))
try {
  $sql = "SET time_zone = '+08:00';`n" + (Get-Content "sql/migrate-stored-file.sql" -Raw -Encoding UTF8) + "`n" + (Get-Content "sql/repair-city-names.sql" -Raw -Encoding UTF8)
  $sql | & mysql -h $DbHost -P $Port -u $AdminUser --ssl-mode=VERIFY_CA "--ssl-ca=$CaFile" --default-character-set=utf8mb4
  if ($LASTEXITCODE -ne 0) { throw "mysql failed" }
  "SHOW TABLES FROM aniplato;" | & mysql -h $DbHost -P $Port -u $AdminUser --ssl-mode=VERIFY_CA "--ssl-ca=$CaFile"
  Write-Host "Done. stored_file should be in the list above."
} finally {
  Remove-Item Env:MYSQL_PWD
}
