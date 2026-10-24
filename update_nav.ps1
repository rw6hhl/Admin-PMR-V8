# update_nav.ps1 — обновление D:\server\Описание из D:\V8 (V9.47)
$src = "D:\V8"
$dst = "D:\server\Описание"
$log = @()
if (-not (Test-Path $src)) { Write-Host "ОШИБКА: $src не существует"; exit 1 }
if (-not (Test-Path $dst)) { New-Item -ItemType Directory -Path $dst -Force | Out-Null }

$versionName = "9.47"; $versionCode = 47
$buildGradle = Join-Path $src "app\build.gradle"
if (Test-Path $buildGradle) {
    $content = Get-Content $buildGradle -Raw
    if ($content -match 'versionName\s+"([^"]+)"') { $versionName = $matches[1] }
    if ($content -match 'versionCode\s+(\d+)') { $versionCode = [int]$matches[1] }
}

$extensions = @("*.java", "*.xml", "*.gradle", "*.properties", "*.yml", "*.pro")
$copied = 0
Get-ChildItem -Path $src -Recurse -Include $extensions -File | ForEach-Object {
    $rel = $_.FullName.Substring($src.Length).TrimStart('\')
    if ($rel -match '^(build|\.gradle|\.git)[\\/]') { return }
    $dstPath = Join-Path $dst ($rel + ".txt")
    $dstDir = Split-Path $dstPath -Parent
    if (-not (Test-Path $dstDir)) { New-Item -ItemType Directory -Path $dstDir -Force | Out-Null }
    Copy-Item -Path $_.FullName -Destination $dstPath -Force
    $copied++
}

$fpDst = Join-Path $dst "app\src\main\res\xml\file_paths.xml.txt"
$fpDir = Split-Path $fpDst -Parent
if (-not (Test-Path $fpDir)) { New-Item -ItemType Directory -Path $fpDir -Force | Out-Null }
if (-not (Test-Path $fpDst)) {
    Set-Content -Path $fpDst -Value "<?xml version=`"1.0`" encoding=`"utf-8`"?>`n<paths>`n    <files-path name=`"logs`" path=`"logs/`" />`n</paths>" -Encoding UTF8
}

$titulPath = Join-Path $dst "ADMIN PMR — СПРАВОЧНИК ПО ФАЙЛАМ.txt"
$titul = "ADMIN PMR — СПРАВОЧНИК ПО ФАЙЛАМ`nВерсия V$versionName`nПапка проекта: D:\V8`nОбновлено: $(Get-Date -Format 'yyyy-MM-dd HH:mm:ss')"
Set-Content -Path $titulPath -Value $titul -Encoding UTF8

$allTxt = Get-ChildItem -Path $dst -Recurse -Filter "*.txt" -File | Where-Object { $_.FullName -notmatch 'ADMIN PMR|СОДЕРЖАНИЕ|СВОДНАЯ|КОМАНДЫ|ТАБЛИЦА' } | Sort-Object FullName
$sb = "СОДЕРЖАНИЕ (V$versionName)`n"
$i = 1
foreach ($f in $allTxt) { $sb += ("{0,3}. {1}`n" -f $i, $f.FullName.Substring($dst.Length).TrimStart('\')); $i++ }
Set-Content -Path (Join-Path $dst "СОДЕРЖАНИЕ.txt") -Value $sb -Encoding UTF8

Write-Host "=== ОБНОВЛЕНИЕ ЗАВЕРШЕНО ==="
Write-Host "Версия: $versionName (versionCode $versionCode)"
Write-Host "Скопировано: $copied файлов"
