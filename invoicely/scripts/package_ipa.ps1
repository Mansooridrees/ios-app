# PowerShell IPA Packaging Script with Standard Unix Forward Slashes
$ErrorActionPreference = "Stop"

$workspaceRoot = "D:\invoicely"
$codeExe = "C:\Users\manso\AppData\Local\Programs\Microsoft VS Code\Code.exe"
$buildJs = "$workspaceRoot\scripts\build_ipa.js"
$payloadDir = "$workspaceRoot\build\ios\Payload"
$targetIpa = "C:\Users\manso\Downloads\InvoiceTracker.ipa"
$localIpa = "$workspaceRoot\build\InvoiceTracker.ipa"

Write-Host ">>> Step 1: Running App Bundle Assembly..." -ForegroundColor Cyan
$env:ELECTRON_RUN_AS_NODE = "1"
$process = Start-Process -FilePath $codeExe -ArgumentList $buildJs -Wait -NoNewWindow -PassThru

if ($process.ExitCode -ne 0) {
    throw "Build JS process exited with error code $($process.ExitCode)"
}

Start-Sleep -Seconds 1

if (-not (Test-Path "$payloadDir\InvoiceTracker.app\Info.plist")) {
    throw "App bundle assembly failed! Info.plist not found in $payloadDir\InvoiceTracker.app"
}

Write-Host ">>> Step 2: Packaging Payload into .IPA with standard POSIX forward slashes..." -ForegroundColor Cyan

# Remove existing targets if any
if (Test-Path $targetIpa) {
    Remove-Item -Path $targetIpa -Force
}
if (Test-Path $localIpa) {
    Remove-Item -Path $localIpa -Force
}

$buildDir = "$workspaceRoot\build"
if (-not (Test-Path $buildDir)) {
    New-Item -ItemType Directory -Path $buildDir -Force | Out-Null
}

$sourceDir = "$workspaceRoot\build\ios"
Add-Type -AssemblyName System.IO.Compression
Add-Type -AssemblyName System.IO.Compression.FileSystem

# Create zip file and add all files with '/' path separators
$zipFileStream = [System.IO.File]::Open($targetIpa, [System.IO.FileMode]::Create)
$zipArchive = New-Object System.IO.Compression.ZipArchive($zipFileStream, [System.IO.Compression.ZipArchiveMode]::Create)

$allFiles = Get-ChildItem -Path $sourceDir -Recurse -File
foreach ($file in $allFiles) {
    $relativePath = $file.FullName.Substring($sourceDir.Length + 1).Replace('\', '/')
    [System.IO.Compression.ZipFileExtensions]::CreateEntryFromFile($zipArchive, $file.FullName, $relativePath, [System.IO.Compression.CompressionLevel]::Optimal) | Out-Null
}

$zipArchive.Dispose()
$zipFileStream.Dispose()

# Copy to local build directory
Copy-Item -Path $targetIpa -Destination $localIpa -Force

Write-Host ">>> Step 3: Verifying final .IPA package..." -ForegroundColor Green
if (Test-Path $targetIpa) {
    $item = Get-Item $targetIpa
    Write-Host "SUCCESS: IPA created at $targetIpa" -ForegroundColor Green
    Write-Host "Size: $([math]::Round($item.Length / 1KB, 2)) KB ($($item.Length) bytes)" -ForegroundColor Yellow
} else {
    throw "Target IPA not found at $targetIpa"
}
