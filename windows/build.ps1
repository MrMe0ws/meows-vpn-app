# Сборка Meows для Windows без IDE.
#   .\build.ps1                -> dist\Meows-win-x64-<версия>.zip  (портативная папка с Meows.exe)
#   .\build.ps1 -Run           -> запуск из Gradle (для разработки)
# Нужны: C:\MeowsTools (JDK 17, Gradle), собранное ядро: ..\core\build-windows.ps1
param([switch]$Run)
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
gradle createDistributable --no-daemon --console=plain
if ($LASTEXITCODE -ne 0) { throw "Gradle завершился с ошибкой" }

$ver = (Select-String -Path "$PSScriptRoot\build.gradle.kts" -Pattern '^version = "(.+)"').Matches[0].Groups[1].Value
$app = Get-ChildItem "$env:MEOWS_WIN_BUILD_DIR\compose\binaries\main\app" -Directory | Select-Object -First 1
New-Item -ItemType Directory -Force "$PSScriptRoot\dist" | Out-Null
$zip = "$PSScriptRoot\dist\Meows-win-x64-$ver.zip"
if (Test-Path $zip) { [IO.File]::Delete($zip) }
Compress-Archive -Path $app.FullName -DestinationPath $zip
"ZIP: {0} ({1:N1} МБ)" -f $zip, ((Get-Item $zip).Length / 1MB)