# Builds the GT6 washing-crash fix coremod without Gradle: javac + jar only.
# All compile dependencies are vendored in ./libs, so the build is fully offline.
#
# Usage:
#   powershell -ExecutionPolicy Bypass -File .\build.ps1
#   powershell -ExecutionPolicy Bypass -File .\build.ps1 -JdkHome "C:\path\to\jdk8"
#
# JDK resolution order: -JdkHome > $env:JAVA_HOME > javac on PATH (must be JDK 8).
param(
    [string]$JdkHome = "C:\Program Files\Java\jdk8u504-b01"
)
$ErrorActionPreference = "Stop"

$root       = Split-Path -Parent $MyInvocation.MyCommand.Path
$buildDir   = Join-Path $root "build"
$classesDir = Join-Path $buildDir "classes"
$resStaging = Join-Path $buildDir "resources"
$libsDir    = Join-Path $buildDir "libs"
$outJar     = Join-Path $libsDir "gt6washingfix-1.0.0.jar"
$manifest   = Join-Path $root "src/main/resources/META-INF/MANIFEST.MF"

# Vendored compile classpath (./libs)
$cpJars = @(
    "launchwrapper-1.12.jar",
    "asm-debug-all-5.0.3.jar",
    "guava-17.0.jar",
    "forge-binaries.jar"
) | ForEach-Object { Join-Path $root "libs/$_" }

# --- locate a JDK 8 ---
if ($JdkHome -and (Test-Path (Join-Path $JdkHome "bin\javac.exe"))) {
    $javac = Join-Path $JdkHome "bin\javac.exe"
    $jar   = Join-Path $JdkHome "bin\jar.exe"
}
elseif ($env:JAVA_HOME -and (Test-Path (Join-Path $env:JAVA_HOME "bin\javac.exe"))) {
    $javac = Join-Path $env:JAVA_HOME "bin\javac.exe"
    $jar   = Join-Path $env:JAVA_HOME "bin\jar.exe"
}
elseif (Get-Command javac -ErrorAction SilentlyContinue) {
    $javac = "javac"
    $jar   = "jar"
}
else {
    throw "No JDK found. Pass -JdkHome, set JAVA_HOME, or put JDK 8's bin on PATH."
}

foreach ($p in @($cpJars + $manifest)) {
    if (-not (Test-Path $p)) { throw "Required file not found: $p" }
}

if (Test-Path $buildDir) { Remove-Item $buildDir -Recurse -Force }
New-Item -ItemType Directory -Force -Path $classesDir, $resStaging, $libsDir | Out-Null

$sources = Get-ChildItem -Recurse (Join-Path $root "src/main/java") -Filter *.java | ForEach-Object FullName
$classpath = [string]::Join(";", $cpJars)

& $javac -encoding UTF-8 -source 1.8 -target 1.8 -nowarn `
    -classpath $classpath -d $classesDir @sources
if ($LASTEXITCODE -ne 0) { throw "javac failed" }

Copy-Item -Recurse (Join-Path $root "src/main/resources/*") $resStaging

Push-Location $classesDir
try {
    & $jar cfm $outJar (Join-Path $resStaging "META-INF/MANIFEST.MF") .
    if ($LASTEXITCODE -ne 0) { throw "jar failed" }
} finally { Pop-Location }

Write-Host "Built: $outJar"
