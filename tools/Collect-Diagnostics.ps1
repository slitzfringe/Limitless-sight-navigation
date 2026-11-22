param([string] $Serial)

$ErrorActionPreference = 'Stop'
$sdk = if ($env:ANDROID_HOME) { $env:ANDROID_HOME } else { Join-Path $env:LOCALAPPDATA 'Android\Sdk' }
$adb = Join-Path $sdk 'platform-tools\adb.exe'
$target = @()
if ($Serial) { $target = @('-s', $Serial) }
$deviceState = & $adb @target get-state
if ($LASTEXITCODE -ne 0 -or $deviceState -ne 'device') {
    throw 'Connect and unlock the phone, approve USB debugging, and specify -Serial if several devices are connected.'
}

$package = 'app.limitlesssight.poc'
$current = & $adb @target shell run-as $package cat files/diagnostics.log
if ($LASTEXITCODE -ne 0 -or -not $current) {
    throw 'No readable diagnostics. Install the debug build and launch it before collecting.'
}
$previousName = 'files/diagnostics.previous.log'
$previous = @()
& $adb @target shell run-as $package test -f $previousName
if ($LASTEXITCODE -eq 0) {
    $previous = & $adb @target shell run-as $package cat $previousName
    if ($LASTEXITCODE -ne 0) { throw 'Could not read the rotated diagnostics.' }
}
$folder = Join-Path (Split-Path -Parent $PSScriptRoot) 'diagnostics'
New-Item -ItemType Directory -Force -Path $folder | Out-Null
$file = Join-Path $folder ("bench-" + (Get-Date -Format 'yyyyMMdd-HHmmss-fff') + ".log")
Set-Content -LiteralPath $file -Encoding UTF8 -Value (@($previous) + @($current))
Write-Output $file
