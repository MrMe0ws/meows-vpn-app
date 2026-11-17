# Сборка ядра для Windows (meows-core.exe): чистый Go, без cgo. Нужны Go и git.
#   .\build-windows.ps1  -> ..\windows\resources\windows-x64\meows-core.exe  (+ geo-файлы из android\app\src\main\assets)
param([string]$Work = "C:\MeowsBuild\xray-core")
$ErrorActionPreference = "Continue"
$commit = "a52db8f6ed19"                       # та же версия Xray-core, что и на Android
$here = $PSScriptRoot
$out = Join-Path $here "..\windows\resources\windows-x64"
New-Item -ItemType Directory -Force $out | Out-Null

if (-not (Test-Path "$Work\.git")) {
    New-Item -ItemType Directory -Force $Work | Out-Null
    git -C $Work init -q; git -C $Work remote add origin https://github.com/XTLS/Xray-core.git
    git -C $Work fetch --depth 300 origin main
}
git -C $Work checkout -q $commit

$libxk = "$Work\libxk_win"; if (Test-Path $libxk) { [IO.Directory]::Delete($libxk, $true) }; New-Item -ItemType Directory $libxk | Out-Null
Get-ChildItem "$here\*.go" | Where-Object { $_.Name -ne "xk_test.go" } | Copy-Item -Destination $libxk -Force

$env:GOFLAGS = "-mod=mod"; $env:GOOS = "windows"; $env:GOARCH = "amd64"; $env:CGO_ENABLED = "0"; $env:CC = ""
$exe = "$out\meows-core.exe"; if (Test-Path $exe) { [IO.File]::Delete($exe) }
Push-Location $Work
go build -trimpath -ldflags "-s -w -checklinkname=0" -o $exe ./libxk_win
Pop-Location
Copy-Item (Join-Path $here "..\android\app\src\main\assets\geoip.dat"), (Join-Path $here "..\android\app\src\main\assets\geosite.dat") $out -Force
"meows-core.exe: {0}" -f $(if (Test-Path $exe) { "ok ({0:N1} MB)" -f ((Get-Item $exe).Length / 1MB) } else { "FAILED" })