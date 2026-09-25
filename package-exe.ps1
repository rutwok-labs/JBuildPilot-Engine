# Builds a self-contained Windows .exe for the JBuildPilot interactive CLI.
#
# Output: dist\JBuildPilot\JBuildPilot.exe
# Double-clicking that .exe opens a console window and launches the interactive
# menu (it asks for a working directory, then shows the options).
#
# Requirements: JDK 21+ with jpackage on PATH (bundled with the JDK).
# No separate Java install is needed on the target machine - jpackage bundles a
# trimmed runtime.

param(
    [string]$Version = "0.1.0"
)

$ErrorActionPreference = "Stop"
$root = Split-Path -Parent $MyInvocation.MyCommand.Path
Set-Location $root

$jar = Join-Path $root "jbuildpilot-cli\target\jbuildpilot-cli.jar"
if (-not (Test-Path $jar)) {
    Write-Host "Shaded CLI jar not found. Building it first..."
    & mvn -q -pl jbuildpilot-cli -am package "-DskipTests" "-Djacoco.skip=true" "-Dgpg.skip=true" "-Dmaven.source.skip=true" "-Dmaven.javadoc.skip=true" "-Dmaven.compiler.fork=true"
    if (-not (Test-Path $jar)) { throw "Build did not produce $jar" }
}

# Clean staging dir holding ONLY the runnable jar (jpackage bundles everything here).
$stage = Join-Path $root "target\exe-input"
if (Test-Path $stage) { Remove-Item -Recurse -Force $stage }
New-Item -ItemType Directory -Force $stage | Out-Null
Copy-Item $jar (Join-Path $stage "jbuildpilot-cli.jar")

$dest = Join-Path $root "dist"
if (Test-Path (Join-Path $dest "JBuildPilot")) { Remove-Item -Recurse -Force (Join-Path $dest "JBuildPilot") }
New-Item -ItemType Directory -Force $dest | Out-Null

Write-Host "Running jpackage..."
& jpackage `
    --type app-image `
    --name JBuildPilot `
    --app-version $Version `
    --input $stage `
    --main-jar jbuildpilot-cli.jar `
    --main-class io.github.rutwoklabs.jbuildpilot.cli.JBuildPilotCli `
    --arguments interactive `
    --win-console `
    --dest $dest

$exe = Join-Path $dest "JBuildPilot\JBuildPilot.exe"
if (Test-Path $exe) {
    Write-Host ""
    Write-Host "SUCCESS: $exe"
    Write-Host "Double-click it (or run it) to launch the interactive menu."
} else {
    throw "jpackage did not produce $exe"
}
