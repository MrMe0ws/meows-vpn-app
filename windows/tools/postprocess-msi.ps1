# Доработка собранных MSI (вызывается из build.ps1 после packageMsi):
#  1. на последней странице установщика — галочка «Запустить Meows» (по умолчанию включена), в английском и русском MSI;
#  2. языковой переход ru.mst (английский MSI → русский интерфейс) — строит torch.exe из WiX;
#  3. Meows-Setup.exe — оболочка, которая выбирает язык по языку Windows (русский → с переходом, иначе английский).
param(
    [Parameter(Mandatory)][string]$EnMsi,      # английский MSI (он же основной: его скачивает автообновление)
    [Parameter(Mandatory)][string]$RuMsi,      # русский MSI (нужен только для разницы строк)
    [Parameter(Mandatory)][string]$OutExe,     # куда положить Meows-Setup.exe
    [Parameter(Mandatory)][string]$ShimSource, # SetupShim.cs
    [Parameter(Mandatory)][string]$Torch       # torch.exe из WiX
)
$ErrorActionPreference = "Stop"
$inst = New-Object -ComObject WindowsInstaller.Installer
function MsiOpen([string]$path, [int]$mode) { $inst.GetType().InvokeMember("OpenDatabase", "InvokeMethod", $null, $inst, @($path, $mode)) }
function MsiView($db, [string]$sql) {
    $v = $db.GetType().InvokeMember("OpenView", "InvokeMethod", $null, $db, @($sql))
    $null = $v.GetType().InvokeMember("Execute", "InvokeMethod", $null, $v, $null)
    $v
}
function MsiRun($db, [string]$sql) { $v = MsiView $db $sql; $null = $v.GetType().InvokeMember("Close", "InvokeMethod", $null, $v, $null) }
function MsiQuery($db, [string]$sql, [int]$n) {
    $v = MsiView $db $sql; $out = @()
    while ($r = $v.GetType().InvokeMember("Fetch", "InvokeMethod", $null, $v, $null)) {
        $out += , ((1..$n | ForEach-Object { $r.GetType().InvokeMember("StringData", "GetProperty", $null, $r, @([int]$_)) }) -join "|")
    }
    $null = $v.GetType().InvokeMember("Close", "InvokeMethod", $null, $v, $null)
    $out
}
function Free { [GC]::Collect(); [GC]::WaitForPendingFinalizers(); [GC]::Collect() }   # иначе COM держит файл открытым и повторное открытие падает
function MsiCommit($db) { $null = $db.GetType().InvokeMember("Commit", "InvokeMethod", $null, $db, $null) }

# --- 1. галочка «Запустить» на последней странице
function Add-LaunchCheckbox([string]$path, [string]$text) {
    $db = MsiOpen $path 1
    $row = MsiQuery $db 'SELECT `File`,`FileName` FROM `File`' 2 | Where-Object { $_ -match '(?i)\|(.*\|)?Meows\.exe$' } | Select-Object -First 1
    if (-not $row) { throw "В MSI не найден файл Meows.exe" }
    $exeKey = $row.Split('|')[0]
    foreach ($p in @(@('WIXUI_EXITDIALOGOPTIONALCHECKBOXTEXT', $text), @('WIXUI_EXITDIALOGOPTIONALCHECKBOX', '1'))) {
        MsiRun $db ("DELETE FROM ``Property`` WHERE ``Property``='" + $p[0] + "'")
        MsiRun $db ("INSERT INTO ``Property`` (``Property``,``Value``) VALUES ('" + $p[0] + "','" + $p[1] + "')")
    }
    # повторный запуск скрипта не должен падать на дублях
    MsiRun $db "DELETE FROM ``CustomAction`` WHERE ``Action``='LaunchMeows'"
    MsiRun $db "DELETE FROM ``ControlEvent`` WHERE ``Argument``='LaunchMeows'"
    # 210 = запуск exe из установленного файла, асинхронно, ошибки игнорируются
    MsiRun $db ("INSERT INTO ``CustomAction`` (``Action``,``Type``,``Source``) VALUES ('LaunchMeows',210,'" + $exeKey + "')")
    MsiRun $db "INSERT INTO ``ControlEvent`` (``Dialog_``,``Control_``,``Event``,``Argument``,``Condition``,``Ordering``) VALUES ('ExitDialog','Finish','DoAction','LaunchMeows','WIXUI_EXITDIALOGOPTIONALCHECKBOX = 1 AND NOT Installed',1)"
    MsiCommit $db
}
Add-LaunchCheckbox $EnMsi "Launch Meows"; Free
Add-LaunchCheckbox $RuMsi "Запустить Meows"; Free

# --- 2. языковой переход: у обоих MSI должен быть один ProductCode (иначе переход к английскому MSI не применится)
$en = MsiOpen $EnMsi 0
$productCode = @(MsiQuery $en "SELECT ``Value`` FROM ``Property`` WHERE ``Property``='ProductCode'" 1)[0]   # @() — иначе при одной строке берётся первый символ
$en = $null; Free
$ru = MsiOpen $RuMsi 1
MsiRun $ru ("UPDATE ``Property`` SET ``Value``='" + $productCode + "' WHERE ``Property``='ProductCode'")
MsiCommit $ru
$ru = $null; Free
$mst = Join-Path (Split-Path $RuMsi) "ru.mst"
if (Test-Path $mst) { [IO.File]::Delete($mst) }
& $Torch -nologo -t language $EnMsi $RuMsi -out $mst
if ($LASTEXITCODE -ne 0 -or -not (Test-Path $mst)) { throw "torch не смог создать ru.mst" }

# --- 3. Meows-Setup.exe
$csc = Join-Path $env:WINDIR "Microsoft.NET\Framework64\v4.0.30319\csc.exe"
if (Test-Path $OutExe) { [IO.File]::Delete($OutExe) }
& $csc /nologo /target:winexe /out:$OutExe "/resource:$EnMsi,meows.msi" "/resource:$mst,ru.mst" $ShimSource
if ($LASTEXITCODE -ne 0) { throw "Не удалось собрать Meows-Setup.exe" }
"Setup: {0} ({1:N1} МБ)" -f $OutExe, ((Get-Item $OutExe).Length / 1MB)
