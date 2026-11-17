# Скачивает флаги стран из Noto Emoji (набор Android; googlefonts/noto-emoji, third_party/region-flags, Apache-2.0)
# и уменьшает до 120 px в ширину: src\main\resources\flags\<код>.png (код ISO в нижнем регистре).
# Windows не умеет рисовать эмодзи-флаги, поэтому приложение показывает эти картинки. Файлы лежат в репозитории; запускать при необходимости.
Add-Type -AssemblyName System.Drawing
$dir = Join-Path $PSScriptRoot "..\src\main\resources\flags"
New-Item -ItemType Directory -Force $dir | Out-Null
$api = "https://api.github.com/repos/googlefonts/noto-emoji/contents/third_party/region-flags/png"
$files = (curl.exe -s $api -H "User-Agent: meows" | ConvertFrom-Json) | Where-Object { $_.name -match '^[A-Z]{2}\.png$' }
$tmp = Join-Path $env:TEMP "meows-flag.png"
foreach ($f in $files) {
    curl.exe -s -f -o $tmp $f.download_url
    if ($LASTEXITCODE -ne 0) { continue }
    $src = [Drawing.Image]::FromFile($tmp)
    $w = 120; $h = [int]($src.Height * $w / $src.Width)
    $bmp = New-Object Drawing.Bitmap $w, $h
    $g = [Drawing.Graphics]::FromImage($bmp); $g.InterpolationMode = "HighQualityBicubic"; $g.SmoothingMode = "HighQuality"
    $g.DrawImage($src, 0, 0, $w, $h); $g.Dispose(); $src.Dispose()
    $bmp.Save((Join-Path $dir ($f.name.ToLower())), [Drawing.Imaging.ImageFormat]::Png); $bmp.Dispose()
}
"флагов: " + (Get-ChildItem $dir -Filter *.png).Count