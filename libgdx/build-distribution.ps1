#Requires -Version 5.1

<#
.SYNOPSIS
Tests AOPi and creates a portable Windows distribution containing AOPi.jar and AOPi.bat.
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
    & (Join-Path $PSScriptRoot 'gradlew.bat') --no-daemon :core:test :lwjgl3:test :lwjgl3:fatJar
    if ($LASTEXITCODE -ne 0) {
        throw "Gradle failed with exit code $LASTEXITCODE. See the build output above."
    }
    $jar = Join-Path $PSScriptRoot 'lwjgl3\build\libs\AOPi.jar'
    if (!(Test-Path -LiteralPath $jar -PathType Leaf)) {
        throw "The build finished but the executable JAR was not found: $jar"
    }
    # Validate the archive before publishing either distribution file.
    Add-Type -AssemblyName System.IO.Compression.FileSystem
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
            'com/github/sarxos/webcam/Webcam.class',
            'gdx64.dll', 'gdx-freetype64.dll',
            'windows/x64/org/lwjgl/lwjgl.dll',
            'windows/x64/org/lwjgl/glfw/glfw.dll',
            'windows/x64/org/lwjgl/opengl/lwjgl_opengl.dll',
            'windows/x64/org/lwjgl/stb/lwjgl_stb.dll'
        )
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
    $distribution = Join-Path $PSScriptRoot 'dist'
    New-Item -ItemType Directory -Path $distribution -Force | Out-Null
    Copy-Item -LiteralPath $jar -Destination (Join-Path $distribution 'AOPi.jar') -Force
    $launcher = Join-Path $distribution 'AOPi.bat'
    $batch = @'
@echo off
setlocal
cd /d "%~dp0" || exit /b 1
javaw -jar "AOPi.jar"
endlocal
'@
    [System.IO.File]::WriteAllText($launcher, $batch.Replace("`r`n", "`n").Replace("`n", "`r`n") + "`r`n",
        [System.Text.Encoding]::ASCII)
    Write-Host ''
    Write-Host 'Distribution built successfully. Launch with:'
    Write-Host "& `"$launcher`""
    Write-Host "Portable files: $distribution\AOPi.jar and $distribution\AOPi.bat"
    Write-Host 'The distribution requires Java 24; it does not bundle a Java runtime.'
} finally {
    Pop-Location
    $env:JAVA_HOME = $previousJavaHome
    $env:GRADLE_USER_HOME = $previousGradleHome
}
