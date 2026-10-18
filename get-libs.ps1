# Downloads Emerald's libraries from Maven Central into lib\ and checks their SHA-1 checksums.
#   LWJGL + GLFW - controllers (Emerald draws its own window with Java; GLFW only reads gamepads)
# build.ps1 runs this automatically; it's quick when everything is already there.

$ErrorActionPreference = 'Stop'
$lib = Join-Path $PSScriptRoot 'lib'
New-Item -ItemType Directory -Force $lib | Out-Null

$lwjgl = '3.3.6'
$artifacts = @()
foreach ($module in @('lwjgl', 'lwjgl-glfw')) {
    $artifacts += "org/lwjgl/$module/$lwjgl/$module-$lwjgl.jar"
    # every platform's native code, so one emerald.jar runs on Windows, macOS and Linux
    foreach ($platform in @('windows', 'macos', 'macos-arm64', 'linux', 'linux-arm64')) {
        $artifacts += "org/lwjgl/$module/$lwjgl/$module-$lwjgl-natives-$platform.jar"
    }
}

$repo = 'https://repo1.maven.org/maven2'
foreach ($path in $artifacts) {
    $file = Join-Path $lib (Split-Path $path -Leaf)
    if (Test-Path $file) { continue }
    Write-Host "Downloading $(Split-Path $path -Leaf)"
    $expected = (Invoke-WebRequest "$repo/$path.sha1" -UseBasicParsing).Content.ToString().Trim().Split(' ')[0]
    Invoke-WebRequest "$repo/$path" -UseBasicParsing -OutFile "$file.part"
    $actual = (Get-FileHash "$file.part" -Algorithm SHA1).Hash
    if ($actual -ne $expected.ToUpper()) {
        Remove-Item "$file.part"
        throw "Checksum mismatch for $path (expected $expected, got $actual)"
    }
    Move-Item "$file.part" $file
}
Write-Host "Libraries ready in $lib"
