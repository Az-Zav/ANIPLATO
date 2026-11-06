<#
 Builds ANIPLATO as a Windows program that needs no Java or Maven on the computer that runs it.
   .\scripts\package-exe.ps1
 Result: target\installer\ANIPLATO\ANIPLATO.exe (a whole folder: zip it and give it out). Needs JDK 17+ with jpackage on PATH.
 The build leaves db.properties out; each user puts their own at %APPDATA%\ANIPLATO\db.properties (see docs/PACKAGING.md).
#>
$ErrorActionPreference = "Stop"
if (-not (Get-Command jpackage -ErrorAction SilentlyContinue)) { throw "jpackage not found. Install a JDK (17 or newer) and put its bin folder on PATH." }

mvn -q -Pdist clean package -DskipTests
if ($LASTEXITCODE -ne 0) { throw "Maven build failed" }

# one flat folder of jars: the program, its libraries, and only the Windows JavaFX jars (the others carry no native code)
$input = "target\dist"
Copy-Item "target\aniplato-*.jar" $input
Get-ChildItem "$input\lib\*.jar" | Where-Object { $_.Name -notmatch '^javafx-.*-21\.0\.2\.jar$' -or $_.Name -match '-win\.jar$' } | Move-Item -Destination $input -Force
Remove-Item "$input\lib" -Recurse -Force
$jar = (Get-ChildItem "$input\aniplato-*.jar").Name

Remove-Item "target\installer" -Recurse -Force -ErrorAction SilentlyContinue
jpackage --type app-image --name ANIPLATO --dest target\installer --input $input --main-jar $jar `
  --main-class com.aniplato.Launcher --icon packaging\aniplato.ico --app-version 0.1.0 `
  --add-modules "java.se,jdk.unsupported,jdk.crypto.ec,jdk.charsets" `
  --java-options "-Dfile.encoding=UTF-8" --java-options "--enable-native-access=ALL-UNNAMED"
if ($LASTEXITCODE -ne 0) { throw "jpackage failed" }
Write-Host "Done: target\installer\ANIPLATO\ANIPLATO.exe"
