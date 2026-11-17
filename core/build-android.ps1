# Сборка ядра Xray (libxkcore.so) для всех ABI приложения. Нужны: Go, git, Android NDK (см. docs/build.md).
#   .\build-android.ps1      -> кладёт .so в ..\android\app\src\main\jniLibs\<abi>\
# arm64 собирается без cgo; armeabi-v7a/x86/x86_64 требуют cgo (NDK clang).
param([string]$Ndk = "C:\MeowsTools\android-sdk\ndk\29.0.14206865", [string]$Work = "C:\MeowsBuild\xray-core")
$ErrorActionPreference = "Continue"
$commit = "a52db8f6ed19"                       # версия Xray-core (см. docs/decisions.md)
$here = $PSScriptRoot
$jni = Join-Path $here "..\android\app\src\main\jniLibs"
$bin = "$Ndk\toolchains\llvm\prebuilt\windows-x86_64\bin"

if (-not (Test-Path "$Work\.git")) {
    New-Item -ItemType Directory -Force $Work | Out-Null
    git -C $Work init -q; git -C $Work remote add origin https://github.com/XTLS/Xray-core.git
    git -C $Work fetch --depth 300 origin main
}
git -C $Work checkout -q $commit
$env:GOFLAGS = "-mod=mod"; $env:GOOS = "android"
$ld = "-s -w -checklinkname=0"                 # -checklinkname=0: нужен wlynxg/anet

# --- arm64: чистый Go, обёртка как есть (файлы с cgo исключаются автоматически)
$libxk = "$Work\libxk"; New-Item -ItemType Directory -Force $libxk | Out-Null
Get-ChildItem "$here\*.go" | Where-Object { $_.Name -ne "xk_test.go" } | Copy-Item -Destination $libxk -Force
$env:CGO_ENABLED = "0"; $env:GOARCH = "arm64"; $env:CC = ""; $env:GOARM = ""
New-Item -ItemType Directory -Force "$jni\arm64-v8a" | Out-Null
go build -trimpath -ldflags $ld -o "$jni\arm64-v8a\libxkcore.so" -C $Work ./libxk

# --- остальные ABI: cgo; из обёртки убираем c-shared-часть (main.go, measure_cgo.go) и тег !cgo у android_main.go
$cg = "$Work\libxk_cgo"; if (Test-Path $cg) { [IO.Directory]::Delete($cg, $true) }; New-Item -ItemType Directory $cg | Out-Null
Get-ChildItem "$here\*.go" | Where-Object { $_.Name -notin "main.go", "measure_cgo.go", "xk_test.go" } | Copy-Item -Destination $cg -Force
$am = "$cg\android_main.go"; (Get-Content $am -Raw) -replace '(?m)^//go:build !cgo\r?\n\r?\n', '' | Set-Content $am -NoNewline
$env:CGO_ENABLED = "1"
$targets = @(
    @{ abi = "armeabi-v7a"; arch = "arm";   cc = "armv7a-linux-androideabi24-clang.cmd"; arm = "7" },
    @{ abi = "x86_64";      arch = "amd64"; cc = "x86_64-linux-android24-clang.cmd";      arm = "" },
    @{ abi = "x86";         arch = "386";   cc = "i686-linux-android24-clang.cmd";        arm = "" }
)
foreach ($t in $targets) {
    $env:GOARCH = $t.arch; $env:CC = "$bin\$($t.cc)"; $env:GOARM = $t.arm
    New-Item -ItemType Directory -Force "$jni\$($t.abi)" | Out-Null
    go build -trimpath -buildmode=pie -ldflags $ld -o "$jni\$($t.abi)\libxkcore.so" -C $Work ./libxk_cgo
    "{0}: {1}" -f $t.abi, $(if (Test-Path "$jni\$($t.abi)\libxkcore.so") { "ok" } else { "FAILED" })
}