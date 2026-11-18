# Сборка Meows для Windows без IDE.
#   .\build.ps1                -> dist\Meows-win-x64-<версия>.zip (портативная папка) и dist\Meows-<версия>.msi (установщик)
#   .\build.ps1 -Run           -> запуск из Gradle (для разработки)
#   .\build.ps1 -Test          -> только юнит-тесты (gradle test)
# Нужны: C:\MeowsTools (JDK 17, Gradle), собранное ядро: ..\core\build-windows.ps1
param([switch]$Run, [switch]$Test)
$ErrorActionPreference = "Continue"
$T = "C:\MeowsTools"
$env:JAVA_HOME = "$T\jdk17"
$env:Path = "$env:JAVA_HOME\bin;$T\gradle-8.14.3\bin;$env:Path"
$env:MEOWS_WIN_BUILD_DIR = "C:\MeowsBuild\winout"

if (-not (Test-Path "$PSScriptRoot\resources\windows-x64\meows-core.exe")) {
    throw "Нет resources\windows-x64\meows-core.exe — сначала запусти ..\core\build-windows.ps1"
}
# Gradle не любит кириллицу в пути («Рабочий стол») — собираем из зеркала
$work = "C:\MeowsBuild\winsrc"
New-Item -ItemType Directory -Force $work | Out-Null
robocopy $PSScriptRoot $work /MIR /XD .gradle build dist /NFL /NDL /NJH /NJS /NP | Out-Null
Set-Location $work
if ($Run) { gradle run --no-daemon --console=plain; return }
if ($Test) { gradle test --no-daemon --console=plain; return }
gradle createDistributable unzipWix --no-daemon --console=plain
if ($LASTEXITCODE -ne 0) { throw "Gradle завершился с ошибкой" }

# WiX light.exe падает на ICE-валидации (нет доступа к службе Windows Installer) — подставляем обёртку с -sval
$wix = "$env:MEOWS_WIN_BUILD_DIR\wix311"
if (-not (Test-Path "$wix\light-real.exe")) { Move-Item "$wix\light.exe" "$wix\light-real.exe" -Force }
Copy-Item "$wix\light.exe.config" "$wix\light-real.exe.config" -Force   # .NET-конфиг привязан к имени exe
$csc = "$env:WINDIR\Microsoft.NET\Framework64\v4.0.30319\csc.exe"
& $csc /nologo /target:exe /out:"$wix\light.exe" "$PSScriptRoot\tools\LightShim.cs"
if ($LASTEXITCODE -ne 0) { throw "Не удалось собрать обёртку light.exe" }
# русская сборка установщика: обёртка light.exe соберёт ещё и русский MSI (для языкового перехода Setup.exe)
$env:MEOWS_RU_WXL = "$work\tools\installer\MsiInstallerStrings_ru.wxl"
$env:MEOWS_RU_MSI = "C:\MeowsBuild\msi_ru\Meows-ru.msi"
if (Test-Path $env:MEOWS_RU_MSI) { [IO.File]::Delete($env:MEOWS_RU_MSI) }
gradle packageMsi -x unzipWix --no-daemon --console=plain
if ($LASTEXITCODE -ne 0) { throw "Gradle (packageMsi) завершился с ошибкой" }

$ver = (Select-String -Path "$PSScriptRoot\build.gradle.kts" -Pattern '^version = "(.+)"').Matches[0].Groups[1].Value
$app = Get-ChildItem "$env:MEOWS_WIN_BUILD_DIR\compose\binaries\main\app" -Directory | Select-Object -First 1
New-Item -ItemType Directory -Force "$PSScriptRoot\dist" | Out-Null
$zip = "$PSScriptRoot\dist\Meows-win-x64-$ver.zip"
if (Test-Path $zip) { [IO.File]::Delete($zip) }
Compress-Archive -Path $app.FullName -DestinationPath $zip
"ZIP: {0} ({1:N1} МБ)" -f $zip, ((Get-Item $zip).Length / 1MB)
$msi = Get-ChildItem "$env:MEOWS_WIN_BUILD_DIR\compose\binaries\main\msi" -Filter *.msi -ErrorAction SilentlyContinue | Select-Object -First 1
if ($msi) {
    # галочка «Запустить Meows» на последней странице + русский язык (Meows-Setup.exe выбирает язык по Windows)
    if (Test-Path $env:MEOWS_RU_MSI) {
        & powershell -NoProfile -ExecutionPolicy Bypass -File "$PSScriptRoot\tools\postprocess-msi.ps1" -EnMsi $msi.FullName -RuMsi $env:MEOWS_RU_MSI -OutExe "$PSScriptRoot\dist\Meows-Setup-$ver.exe" -ShimSource "$PSScriptRoot\tools\SetupShim.cs" -Torch "$wix\torch.exe"
        if ($LASTEXITCODE -ne 0) { throw "Доработка установщика не удалась" }
    } else { Write-Warning "Русский MSI не собран — Setup.exe не создан" }
    $dst = "$PSScriptRoot\dist\Meows-$ver.msi"
    Copy-Item $msi.FullName $dst -Force
    "MSI: {0} ({1:N1} МБ)" -f $dst, ((Get-Item $dst).Length / 1MB)
}