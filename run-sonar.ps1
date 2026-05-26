# SonarQube scan — uses JDK 25 and correct Maven arguments for PowerShell
# Usage:
#   $env:SONAR_TOKEN = "squ_your_token"
#   .\run-sonar.ps1

$ErrorActionPreference = "Stop"

$jdk25 = "C:\Users\maxmo\.jdks\ms-25.0.3"
if (-not (Test-Path "$jdk25\bin\java.exe")) {
    Write-Error "JDK 25 not found at $jdk25. Set JAVA_HOME to your JDK 25 path in this script."
}

$env:JAVA_HOME = $jdk25
$env:Path = "$env:JAVA_HOME\bin;" + $env:Path

if (-not $env:SONAR_TOKEN) {
    Write-Error "Set token first: `$env:SONAR_TOKEN = 'squ_your_token'"
}

Write-Host "Java version:"
java -version

Write-Host "`nRunning Sonar scan..."
& .\mvnw.cmd clean verify sonar:sonar `
    "-Dsonar.host.url=http://localhost:9000" `
    "-Dsonar.token=$env:SONAR_TOKEN"

if ($LASTEXITCODE -eq 0) {
    Write-Host "`nDone. Open one of these (Maven may use hr.algebra:JavaWebPrj on first scan):"
    Write-Host "  http://localhost:9000/dashboard?id=hr.algebra:JavaWebPrj"
    Write-Host "  http://localhost:9000/dashboard?id=JavaWebPrj"
    Write-Host "Or SonarQube home -> Projects"
}
