#Requires -Version 5.1

<#
.SYNOPSIS
Tests AOPi and creates portable Windows, Mac, and Linux JARs plus a Windows launcher.
.EXAMPLE
.\build-distribution.ps1
.EXAMPLE
.\build-distribution.ps1 -JavaHome 'C:\Program Files\Java\jdk-24'
#>
[CmdletBinding()]
param (
    [string]$JavaHome
)

$ErrorActionPreference = 'Stop'

function Test-Jdk24 {
    param ([string]$Directory)

    if ([string]::IsNullOrWhiteSpace($Directory)) {
        return $false
    }
    $compiler = Join-Path $Directory 'bin\javac.exe'
    $release = Join-Path $Directory 'release'
    if (!(Test-Path -LiteralPath $compiler -PathType Leaf) -or
        !(Test-Path -LiteralPath $release -PathType Leaf)) {
        return $false
    }
    return [bool]((Get-Content -LiteralPath $release) -match '^JAVA_VERSION="24(?:\.|"|-)')
}

function Find-Jdk24 {
    $candidates = @($env:JAVA_HOME)
    $compiler = Get-Command javac.exe -ErrorAction SilentlyContinue
    if ($compiler) {
        $candidates += Split-Path (Split-Path $compiler.Source -Parent) -Parent
    }
    # Check common Windows JDK installation folders; no machine-specific path is required.
    if ($env:ProgramFiles) {
        foreach ($vendor in @('Java', 'Eclipse Adoptium', 'Microsoft')) {
            $directory = Join-Path $env:ProgramFiles $vendor
            if (Test-Path -LiteralPath $directory -PathType Container) {
                $candidates += Get-ChildItem -LiteralPath $directory -Directory |
                    Select-Object -ExpandProperty FullName
            }
        }
    }
    foreach ($candidate in $candidates) {
        if (Test-Jdk24 $candidate) {
            return $candidate
        }
    }
    throw 'JDK 24 was not found. Install JDK 24 and pass -JavaHome or set JAVA_HOME to its installation directory.'
}

if ($JavaHome) {
    if (!(Test-Jdk24 $JavaHome)) {
        throw "The JavaHome directory must contain JDK 24 (including bin\javac.exe): $JavaHome"
    }
} else {
    $JavaHome = Find-Jdk24
}
$JavaHome = (Resolve-Path -LiteralPath $JavaHome).Path

$previousJavaHome = $env:JAVA_HOME
$previousGradleHome = $env:GRADLE_USER_HOME
Push-Location -LiteralPath $PSScriptRoot
try {
    $env:JAVA_HOME = $JavaHome
    if ([string]::IsNullOrWhiteSpace($env:GRADLE_USER_HOME)) {
        $env:GRADLE_USER_HOME = Join-Path $PSScriptRoot '.gradle-user-home'
    }
    Write-Host "Building AOPi with $env:JAVA_HOME"
    # The wrapper filename is gradlew.bat, not gradlew\.bat. Always run from this project.
    & (Join-Path $PSScriptRoot 'gradlew.bat') --no-daemon :core:test :lwjgl3:test :lwjgl3:platformJars
    if ($LASTEXITCODE -ne 0) {
        throw "Gradle failed with exit code $LASTEXITCODE. See the build output above."
    }
    $platforms = @('Windows', 'Mac', 'Linux')
    # Validate every platform before publishing any of the JARs.
    Add-Type -AssemblyName System.IO.Compression.FileSystem
    foreach ($targetPlatform in $platforms) {
        $jar = Join-Path $PSScriptRoot "lwjgl3\build\libs\AOPi-$targetPlatform.jar"
        if (!(Test-Path -LiteralPath $jar -PathType Leaf)) {
            throw "The build finished but the executable JAR was not found: $jar"
        }
        # Validate the archive before publishing either distribution file.
        $archive = [System.IO.Compression.ZipFile]::OpenRead($jar)
        try {
            $entries = @($archive.Entries | Select-Object -ExpandProperty FullName)
            $required = @(
                'de/isuret/polos/aopi/desktop/DesktopLauncher.class',
                'de/isuret/polos/aopi/AopiApplication.class',
                'com/badlogic/gdx/backends/lwjgl3/Lwjgl3Application.class',
                'com/badlogic/gdx/graphics/g2d/SpriteBatch.class',
                'org/lwjgl/glfw/GLFW.class',
                'com/sun/jna/Native.class',
                'com/fazecast/jSerialComm/SerialPort.class',
                'com/github/sarxos/webcam/Webcam.class'
            )
            if ($targetPlatform -eq 'Windows') {
                $required += @('gdx64.dll', 'gdx-freetype64.dll',
                    'windows/x64/org/lwjgl/lwjgl.dll', 'windows/x64/org/lwjgl/glfw/glfw.dll',
                    'windows/x64/org/lwjgl/opengl/lwjgl_opengl.dll', 'windows/x64/org/lwjgl/stb/lwjgl_stb.dll')
                $foreignNativePattern = '\.(so|dylib|jnilib)$'
            } else {
                if ($targetPlatform -eq 'Mac') {
                    $nativeDirectory = 'macos'
                    $extension = 'dylib'
                    $foreignNativePattern = '\.(dll|so)$'
                } else {
                    $nativeDirectory = 'linux'
                    $extension = 'so'
                    $foreignNativePattern = '\.(dll|dylib|jnilib)$'
                }
                $required += @("libgdx64.$extension", "libgdxarm64.$extension",
                    "libgdx-freetype64.$extension", "libgdx-freetypearm64.$extension")
                foreach ($architecture in @('x64', 'arm64')) {
                    foreach ($library in @('org/lwjgl/liblwjgl', 'org/lwjgl/glfw/libglfw',
                            'org/lwjgl/opengl/liblwjgl_opengl', 'org/lwjgl/stb/liblwjgl_stb')) {
                        $required += "$nativeDirectory/$architecture/$library.$extension"
                    }
                }
            }
            if ($entries -match $foreignNativePattern) { throw "Foreign platform natives found in $jar" }
            $resources = Join-Path $PSScriptRoot 'core\src\main\resources'
            $required += Get-ChildItem -LiteralPath $resources -File -Recurse | ForEach-Object {
                $_.FullName.Substring($resources.Length + 1).Replace('\', '/')
            }
            foreach ($entry in $required) {
                if ($entries -cnotcontains $entry) { throw "Required file is missing from the JAR: $entry" }
            }
            $manifest = $archive.GetEntry('META-INF/MANIFEST.MF')
            if (!$manifest) { throw 'The JAR manifest is missing.' }
            $reader = New-Object System.IO.StreamReader($manifest.Open())
            try { $manifestText = $reader.ReadToEnd() } finally { $reader.Dispose() }
            if ($manifestText -notmatch '(?m)^Main-Class: de\.isuret\.polos\.aopi\.desktop\.DesktopLauncher\r?$') {
                throw 'The JAR manifest has an incorrect Main-Class.'
            }
            if ($manifestText -notmatch '(?m)^Enable-Native-Access: ALL-UNNAMED\r?$') {
                throw 'The JAR manifest must enable native access for the desktop libraries.'
            }
        } finally {
            $archive.Dispose()
        }
    }
    $distribution = Join-Path $PSScriptRoot 'dist'
    New-Item -ItemType Directory -Path $distribution -Force | Out-Null
    foreach ($targetPlatform in $platforms) {
        $name = "AOPi-$targetPlatform.jar"
        Copy-Item -LiteralPath (Join-Path $PSScriptRoot "lwjgl3\build\libs\$name") -Destination (Join-Path $distribution $name) -Force
    }
    # Retire only the previous generated universal JAR; never remove application data.
    $previousJar = Join-Path $distribution 'AOPi.jar'
    if (Test-Path -LiteralPath $previousJar -PathType Leaf) { Remove-Item -LiteralPath $previousJar }
    $launcher = Join-Path $distribution 'AOPi.bat'
    $batch = @'
@echo off
setlocal
cd /d "%~dp0" || exit /b 1
javaw -jar "AOPi-Windows.jar"
endlocal
'@
    [System.IO.File]::WriteAllText($launcher, $batch.Replace("`r`n", "`n").Replace("`n", "`r`n") + "`r`n",
        [System.Text.Encoding]::ASCII)
    Write-Host ''
    Write-Host 'Distribution built successfully. Launch with:'
    Write-Host "& `"$launcher`""
    Write-Host "Portable JARs: $distribution\AOPi-Windows.jar, AOPi-Mac.jar, AOPi-Linux.jar"
    Write-Host 'Mac: java -XstartOnFirstThread -jar AOPi-Mac.jar'
    Write-Host 'Linux: java -jar AOPi-Linux.jar'
    Write-Host 'The distribution requires Java 24; it does not bundle a Java runtime.'
} finally {
    Pop-Location
    $env:JAVA_HOME = $previousJavaHome
    $env:GRADLE_USER_HOME = $previousGradleHome
}
