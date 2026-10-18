# Builds Emerald into build\emerald.jar: one file with Emerald, the Seven language and its libraries.
# Needs JDK 21+. Downloads the libraries into lib\ the first time (see get-libs.ps1).

param([string]$OutDir = '')
$ErrorActionPreference = 'Stop'
$root    = $PSScriptRoot
$build   = if ($OutDir) { $OutDir } else { Join-Path $root 'build' }
$classes = Join-Path $build 'classes'
$lib     = Join-Path $root 'lib'
$jdk = if ($env:JAVA_HOME) { Join-Path $env:JAVA_HOME 'bin' } else { Split-Path (Get-Command javac.exe).Source }

# Windows locks the jar while Emerald is running
$jarFile = Join-Path $build 'emerald.jar'
if (Test-Path $jarFile) {
    try { [IO.File]::Open($jarFile, 'Open', 'ReadWrite', 'None').Close() }
    catch { Write-Host 'Emerald is still open. Save (Ctrl+S), close it, then build again.'; exit 1 }
}

& (Join-Path $root 'get-libs.ps1')
if (Test-Path $classes) { Remove-Item -Recurse -Force $classes }
New-Item -ItemType Directory -Force $classes | Out-Null

# Emerald's code and the Seven language (src\sev7n: the language Emerald's carts are written in)
$sources = Get-ChildItem -Recurse (Join-Path $root 'src') -Filter *.java
$list = Join-Path $build 'sources.txt'
[IO.File]::WriteAllLines($list, ($sources | ForEach-Object { '"' + ($_.FullName -replace '\\', '/') + '"' }))
$libraries = Get-ChildItem $lib -Filter *.jar
$classpath = ($libraries | Where-Object { $_.Name -notmatch 'natives' } | ForEach-Object { $_.FullName }) -join ';'
& (Join-Path $jdk 'javac.exe') --release 21 -encoding UTF-8 -cp $classpath -d $classes "@$list"
if ($LASTEXITCODE -ne 0) { exit 1 }

# the libraries go inside the jar (including their native files), then the font, cursors and icon
Push-Location $classes
foreach ($library in $libraries) { & (Join-Path $jdk 'jar.exe') xf $library.FullName }
Remove-Item -Recurse -Force (Join-Path $classes 'META-INF') -ErrorAction SilentlyContinue
Pop-Location
Copy-Item (Join-Path $root 'assets\*') (Join-Path $classes 'emerald')

$manifest = Join-Path $build 'manifest.txt'
[IO.File]::WriteAllText($manifest, "Main-Class: emerald.Emerald`n")
& (Join-Path $jdk 'jar.exe') --create --file $jarFile --manifest $manifest -C $classes .
if ($LASTEXITCODE -ne 0) { exit 1 }
Write-Host "Built $jarFile"
