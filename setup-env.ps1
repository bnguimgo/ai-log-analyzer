Write-Host "Start running this script  with .\setup-env.ps1"
$env:JAVA_HOME = "C:\Program Files\Java\jdk-21.0.10"
$env:MAVEN_HOME = "C:\DevTools\ai-log-analyzer\maven\apache-maven-3.9.16"

$env:Path = "$env:JAVA_HOME\bin;$env:MAVEN_HOME\bin;$env:Path"

Write-Host ""
Write-Host "========================================"
Write-Host " AI Log Analyzer - Environment"
Write-Host "========================================"
Write-Host "JAVA_HOME  = $env:JAVA_HOME"
Write-Host "MAVEN_HOME = $env:MAVEN_HOME"
Write-Host "========================================"
Write-Host ""

Write-Host "Java:"
java -version

Write-Host ""
Write-Host "Maven:"
mvn -version

Write-Host ""