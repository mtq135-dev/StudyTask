param(
    [ValidateSet('x64','arm64','x86')][string]$Architecture = 'x64',
    [string]$JdkHome = $env:JAVA_HOME,
    [switch]$BundledJavaFx,
    [switch]$ImageOnly
)
$ErrorActionPreference = 'Stop'
Set-Location (Split-Path $PSScriptRoot -Parent)
function Run([string]$exe, [string[]]$arguments) {
    & $exe @arguments
    if ($LASTEXITCODE -ne 0) { throw "$exe failed with exit code $LASTEXITCODE" }
}
function Machine([string]$file) {
    $stream = [IO.File]::OpenRead($file)
    $reader = [IO.BinaryReader]::new($stream)
    try { $stream.Position=0x3c; $offset=$reader.ReadInt32(); $stream.Position=$offset+4; return $reader.ReadUInt16() }
    finally { $reader.Dispose() }
}
$transcriptStarted = $false
try {
    New-Item -ItemType Directory -Force 'logs' | Out-Null
    $logPath = Join-Path (Get-Location) ("logs\build-exe-{0}-{1}.log" -f $Architecture,(Get-Date -Format 'yyyyMMdd-HHmmss-fff'))
    Start-Transcript -LiteralPath $logPath | Out-Null
    $transcriptStarted = $true
    Write-Host "Build log: $logPath"
    if (!$JdkHome) { $JdkHome = Split-Path (Split-Path (Get-Command java.exe).Source -Parent) -Parent }
    $JdkHome = (Resolve-Path -LiteralPath $JdkHome).Path
    foreach ($tool in @('java','javac','jlink','jdeps','jpackage')) {
        if (!(Test-Path "$JdkHome\bin\$tool.exe")) { throw "JDK lacks $tool.exe: $JdkHome" }
    }
    $ErrorActionPreference = 'Continue' # Windows PowerShell wraps native stderr as ErrorRecord.
    $settings = (& "$JdkHome\bin\java.exe" -XshowSettings:properties -version 2>&1 | Out-String)
    $ErrorActionPreference = 'Stop'
    if ($settings -notmatch 'java.specification.version = 21\s') { throw 'A Java 21 JDK is required.' }
    $machineCode = @{x64=0x8664; arm64=0xaa64; x86=0x14c}[$Architecture]
    if ((Machine "$JdkHome\bin\java.exe") -ne $machineCode) { throw "JDK architecture does not match $Architecture. Cross-compilation is not supported." }
    if ($Architecture -ne 'x64' -and !$BundledJavaFx) { throw 'ARM64 and x86 require -BundledJavaFx and a matching full Java 21 JDK with JavaFX modules, jlink and jpackage.' }
    $modules = (& "$JdkHome\bin\java.exe" --list-modules | Out-String)
    if ($BundledJavaFx -and $modules -notmatch 'javafx.controls@21') { throw 'The selected full JDK must contain JavaFX 21.' }
    $env:JAVA_HOME=$JdkHome; $env:PATH="$JdkHome\bin;$env:PATH"
    $mavenArgs=@('-B','clean','verify')
    if ($BundledJavaFx) { $mavenArgs += '-Pbundled-javafx' }
    Run '.\mvnw.cmd' $mavenArgs
    $stage = Join-Path (Get-Location) 'target\package-input'
    New-Item -ItemType Directory -Force "$stage\lib" | Out-Null
    Copy-Item 'target\studytask-1.0.0.jar' $stage
    Copy-Item 'target\lib\*.jar' "$stage\lib"
    # Check JavaFX native binaries rather than inferring their architecture from the OS.
    Add-Type -AssemblyName System.IO.Compression.FileSystem
    if (!$BundledJavaFx) {
        $found = 0
        foreach ($jar in Get-ChildItem "$stage\lib\javafx-*.jar") {
            $zip=[IO.Compression.ZipFile]::OpenRead($jar.FullName)
            try { foreach($entry in $zip.Entries) { if($entry.Name -like '*.dll') {
                $native=Join-Path 'target' $entry.Name
                [IO.Compression.ZipFileExtensions]::ExtractToFile($entry,$native,$true)
                if((Machine $native) -ne $machineCode) { throw "JavaFX native architecture mismatch: $($entry.Name)" }; $found++
            } } } finally { $zip.Dispose() }
        }
        if(!$found) { throw 'No JavaFX native DLLs found.' }
    } else {
        if(!(Test-Path "$JdkHome\bin\glass.dll") -or (Machine "$JdkHome\bin\glass.dll") -ne $machineCode) { throw 'JavaFX glass.dll architecture mismatch or missing.' }
    }
    $runtime = Join-Path (Get-Location) 'target\runtime'
    $roots='java.base,java.desktop,java.logging,java.xml,java.prefs,jdk.unsupported,jdk.crypto.ec'
    if($BundledJavaFx) { $roots += ',javafx.controls' }
    Run "$JdkHome\bin\jlink.exe" @('--module-path',"$JdkHome\jmods",'--add-modules',$roots,'--output',$runtime,'--strip-debug','--no-header-files','--no-man-pages','--compress=2')
    # Refuse to replace prior releases. Move/archive dist before building another architecture.
    if(Test-Path 'dist\StudyTask') { throw 'dist\StudyTask already exists. Archive dist before rebuilding.' }
    New-Item -ItemType Directory -Force dist | Out-Null
    $packageArgs=@('--type','app-image','--name','StudyTask','--app-version','1.0.0','--vendor','StudyTask','--input',$stage,'--main-jar','studytask-1.0.0.jar','--main-class','com.studytask.Main','--runtime-image',$runtime,'--dest','dist')
    $packageArgs += @('--icon',(Join-Path (Get-Location) 'src\main\resources\icons\studytask.ico'))
    if($BundledJavaFx) { $packageArgs += @('--java-options','--add-modules=javafx.controls') }
    Run "$JdkHome\bin\jpackage.exe" $packageArgs
    if((Machine 'dist\StudyTask\StudyTask.exe') -ne $machineCode) { throw 'Generated launcher architecture mismatch.' }
    if(!(Test-Path 'dist\StudyTask\runtime\bin\server\jvm.dll')) { throw 'Runtime missing.' }
    if((Machine 'dist\StudyTask\runtime\bin\server\jvm.dll') -ne $machineCode) { throw 'Runtime architecture mismatch.' }
    "Architecture=$Architecture`nJDK=$JdkHome`nJavaFXBundled=$BundledJavaFx" | Set-Content 'dist\build-info.txt'
    if($ImageOnly) { Write-Host 'Portable image generated; installer not requested.'; exit 0 }
    if(!(Get-Command candle.exe -ErrorAction SilentlyContinue) -or !(Get-Command light.exe -ErrorAction SilentlyContinue)) { throw 'Portable image generated. Installer requires WiX 3.x candle.exe and light.exe on PATH (Java 21 requirement).' }
    Run "$JdkHome\bin\jpackage.exe" @('--type','exe','--name','StudyTask','--app-version','1.0.0','--vendor','StudyTask','--app-image','dist\StudyTask','--icon',(Join-Path (Get-Location) 'src\main\resources\icons\studytask.ico'),'--dest','target\installer','--win-per-user-install','--win-dir-chooser','--win-menu','--win-shortcut','--win-upgrade-uuid','9eae3ba1-d8d2-4f9d-a93e-6260cc07efea')
    Move-Item -LiteralPath 'target\installer\StudyTask-1.0.0.exe' -Destination 'dist\StudyTask-Setup.exe'
    if((Get-Item 'dist\StudyTask-Setup.exe').Length -eq 0) { throw 'Empty installer.' }
    Write-Host "Generated $Architecture image and installer. Launch and installation still require manual verification."
} catch { Write-Error $_; exit 1 }
finally { if ($transcriptStarted) { Stop-Transcript -ErrorAction SilentlyContinue | Out-Null } }
