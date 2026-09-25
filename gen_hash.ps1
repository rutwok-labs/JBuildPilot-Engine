$jarPath = "jbuildpilot-cli\target\jbuildpilot-cli.jar"
if (Test-Path $jarPath) {
    Write-Host "Creating sha256 checksum..."
    # certutil returns 3 lines, we want the second line (the hash)
    $hash = (certutil -hashfile $jarPath SHA256)[1] -replace " "
    $hash | Out-File -FilePath "$jarPath.sha256" -Encoding ascii
    Write-Host "Done: $jarPath.sha256"
} else {
    Write-Host "Wait, jbuildpilot-cli.jar doesn't exist yet!"
}
