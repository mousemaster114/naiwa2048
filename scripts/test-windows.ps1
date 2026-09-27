param(
    [string]$GradleExecutable = '',
    [string]$RobolectricRuntimeDirectory = ''
)
$ErrorActionPreference = 'Stop'
$projectRoot = Split-Path $PSScriptRoot -Parent
if (!$GradleExecutable) { $GradleExecutable = Join-Path $projectRoot 'gradlew.bat' }
Push-Location $projectRoot
try {
    & $GradleExecutable :app:exportTestClasspath
    if ($LASTEXITCODE -ne 0) { throw 'Test compilation failed.' }
    $testClasspath = Get-Content -LiteralPath 'app/build/test-classpath.txt' -Raw
    $javaArgs = @('-Dfile.encoding=UTF-8')
    if ($RobolectricRuntimeDirectory) {
        $runtimePath = (Resolve-Path -LiteralPath $RobolectricRuntimeDirectory).Path
        $javaArgs += "-Drobolectric.dependency.dir=$runtimePath"
    }
    $javaArgs += @('-cp', $testClasspath, 'org.junit.runner.JUnitCore', 'com.naiwa.game.GameTest', 'com.naiwa.game.ActivityTest')
    Set-Location (Join-Path $projectRoot 'app')
    & java @javaArgs
    if ($LASTEXITCODE -ne 0) { throw 'JUnit tests failed.' }
} finally { Pop-Location }
