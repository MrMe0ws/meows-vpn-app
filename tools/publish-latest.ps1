# Обновляет «плавающий» релиз с постоянной ссылкой на свежую сборку.
#   .\tools\publish-latest.ps1 -Tag android-v1.4.0     -> релиз android-latest, файл Meows.apk
#   .\tools\publish-latest.ps1 -Tag windows-v1.0.0     -> релиз windows-latest, файл Meows.msi
# Нужен gh (авторизованный; в Actions — GH_TOKEN). Вызывается автоматически workflow «floating-latest» при выходе релиза.
param([Parameter(Mandatory = $true)][string]$Tag, [string]$Repo = "MrMe0ws/meows-vpn-app")
$ErrorActionPreference = "Stop"

if ($Tag -like "android-v*") { $platform = "android"; $pattern = "*.apk"; $fixed = "Meows.apk"; $title = "Meows для Android — последняя версия" }
elseif ($Tag -like "windows-v*") { $platform = "windows"; $pattern = "*.msi"; $fixed = "Meows.msi"; $title = "Meows для Windows — последняя версия" }
else { throw "Тег должен быть android-vX.Y.Z или windows-vX.Y.Z, получено: $Tag" }
$floating = "$platform-latest"
$version = $Tag -replace "^$platform-v", ""

$tmp = Join-Path ([IO.Path]::GetTempPath()) ("meows-latest-" + [guid]::NewGuid().ToString("N"))
New-Item -ItemType Directory $tmp | Out-Null
try {
    gh release download $Tag --repo $Repo --pattern $pattern --dir $tmp
    $src = Get-ChildItem $tmp -File | Select-Object -First 1
    if (-not $src) { throw "В релизе $Tag нет файла $pattern" }
    $file = Join-Path $tmp $fixed
    Move-Item $src.FullName $file

    $notes = "Здесь всегда лежит последняя сборка (сейчас $version) под постоянным именем $fixed.`nПрямая ссылка: https://github.com/$Repo/releases/download/$floating/$fixed`nОбычный релиз с описанием изменений: https://github.com/$Repo/releases/tag/$Tag`n`nЭтот релиз обновляется автоматически (tools/publish-latest.ps1), не удаляйте его."
    $ErrorActionPreference = "Continue"   # gh пишет «release not found» в stderr — это не ошибка скрипта
    gh release view $floating --repo $Repo *> $null
    $exists = ($LASTEXITCODE -eq 0)
    $ErrorActionPreference = "Stop"
    if (-not $exists) {
        gh release create $floating --repo $Repo --target main --title $title --notes $notes --latest=false
    } else {
        gh release edit $floating --repo $Repo --title $title --notes $notes --latest=false --prerelease=false
        $ErrorActionPreference = "Continue"
        gh release delete-asset $floating $fixed --repo $Repo --yes *> $null   # старый файл убираем, потом кладём новый
        $ErrorActionPreference = "Stop"
    }
    gh release upload $floating $file --repo $Repo --clobber
    "OK: $floating -> $fixed ($version)"

    # Windows: рядом лежит установщик с выбором языка (Meows-Setup-X.Y.Z.exe) — он же под постоянным именем Meows-Setup.exe
    if ($platform -eq "windows") {
        $sdir = Join-Path $tmp "setup"; New-Item -ItemType Directory $sdir | Out-Null
        $ErrorActionPreference = "Continue"
        gh release download $Tag --repo $Repo --pattern "Meows-Setup-*.exe" --dir $sdir *> $null
        $ErrorActionPreference = "Stop"
        $setup = Get-ChildItem $sdir -File -ErrorAction SilentlyContinue | Select-Object -First 1
        if ($setup) {
            $setupFile = Join-Path $tmp "Meows-Setup.exe"; Move-Item $setup.FullName $setupFile
            $ErrorActionPreference = "Continue"; gh release delete-asset $floating "Meows-Setup.exe" --repo $Repo --yes *> $null; $ErrorActionPreference = "Stop"
            gh release upload $floating $setupFile --repo $Repo --clobber
            "OK: $floating -> Meows-Setup.exe ($version)"
        }
    }
} finally { Remove-Item $tmp -Recurse -Force -ErrorAction SilentlyContinue }
