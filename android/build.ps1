# Сборка МЯУС VPN без Android Studio.
#   .\build.ps1            -> release APK (подписанный), универсальный
#   .\build.ps1 -Debug     -> debug APK
#   .\build.ps1 -Play      -> AAB для Google Play (dist\MeowsVPN-play.aab)
# Инструменты лежат в C:\MeowsTools (JDK 17, Gradle, Android SDK) — вне OneDrive и без кириллицы в пути.
param([switch]$Debug, [switch]$Play, [string]$Task = "")

$ErrorActionPreference = "Continue"   # нативные программы пишут инфо в stderr; ошибки ловим по $LASTEXITCODE
$T = "C:\MeowsTools"
$env:JAVA_HOME = "$T\jdk17"
$env:ANDROID_HOME = "$T\android-sdk"
$env:ANDROID_SDK_ROOT = "$T\android-sdk"
$env:MEOWS_BUILD_DIR = "C:\MeowsBuild\out"
$env:Path = "$env:JAVA_HOME\bin;$T\gradle-8.14.3\bin;$env:Path"

# Ключ подписи: создаётся один раз. Храни meows.keystore и keypass.txt (C:\MeowsTools) — без них APK не обновить поверх.
$ks = "$T\meows.keystore"
$passFile = "$T\keypass.txt"
if ($env:MEOWS_KEYPASS) { $pass = $env:MEOWS_KEYPASS }
elseif (Test-Path $passFile) { $pass = (Get-Content $passFile -Raw).Trim() }
elseif (Test-Path $ks) { $pass = "meows-change-me" }   # ключ, созданный первой версией скрипта
else { $pass = -join ((48..57) + (65..90) + (97..122) | Get-Random -Count 24 | ForEach-Object { [char]$_ }) }
if (-not (Test-Path $passFile)) { Set-Content -Path $passFile -Value $pass -NoNewline }
if (-not (Test-Path $ks)) {
    $ErrorActionPreference = "Continue"
    & "$env:JAVA_HOME\bin\keytool.exe" -genkeypair -keystore $ks -storepass $pass -keypass $pass -alias meows `
        -keyalg RSA -keysize 2048 -validity 10000 -dname "CN=Meows VPN" 2>$null
}$env:MEOWS_KEYSTORE = $ks
$env:MEOWS_KEYPASS = $pass

# Gradle/AGP на Windows не любят не-ASCII в пути проекта (папка «Рабочий стол»), поэтому собираем из зеркала.
$work = "C:\MeowsBuild\src"
New-Item -ItemType Directory -Force $work | Out-Null
robocopy $PSScriptRoot $work /MIR /XD .gradle build dist .toolchain .git .idea /XF *.keystore /NFL /NDL /NJH /NJS /NP | Out-Null
Set-Location $work
$gradleTask = if ($Task) { $Task } elseif ($Play) { "bundlePlayRelease" } elseif ($Debug) { "assembleDirectDebug" } else { "assembleDirectRelease" }
gradle $gradleTask --no-daemon --console=plain
if ($LASTEXITCODE -ne 0) { throw "Gradle завершился с ошибкой" }

if ($Play) {
    $aab = Get-ChildItem "$env:MEOWS_BUILD_DIR\app\outputs\bundle\playRelease" -Filter *.aab | Select-Object -First 1
    if (-not $aab) { throw "AAB не найден" }
    New-Item -ItemType Directory -Force "$PSScriptRoot\dist" | Out-Null
    Copy-Item $aab.FullName "$PSScriptRoot\dist\MeowsVPN-play.aab" -Force
    Write-Host ("AAB: $PSScriptRoot\dist\MeowsVPN-play.aab ({0:N1} МБ)" -f ((Get-Item "$PSScriptRoot\dist\MeowsVPN-play.aab").Length / 1MB))
    return
}
$kind = if ($Debug) { "debug" } else { "release" }
$apk = Get-ChildItem "$env:MEOWS_BUILD_DIR\app\outputs\apk\direct\$kind" -Filter *.apk | Select-Object -First 1
if ($apk) {
    New-Item -ItemType Directory -Force "$PSScriptRoot\dist" | Out-Null
    $dest = "$PSScriptRoot\dist\MeowsVPN-$kind.apk"
    Copy-Item $apk.FullName $dest -Force
    Write-Host ("APK: {0} ({1:N1} МБ)" -f $dest, ((Get-Item $dest).Length / 1MB))
}
