$adb = "$env:LOCALAPPDATA\Android\Sdk\platform-tools\adb.exe"
$apk = "$PSScriptRoot\..\app\build\outputs\apk\debug\app-debug.apk"

if (-not (Test-Path $apk)) {
    Write-Error "No se encontró el APK en $apk"
    exit 1
}

Write-Host "Verificando dispositivo Android..." -ForegroundColor Cyan
& $adb devices

Write-Host "Instalando APK nativo Jetpack Compose (PardoFinanzas)..." -ForegroundColor Green
& $adb install -r --user 0 $apk

Write-Host "Iniciando aplicación..." -ForegroundColor Cyan
& $adb shell am start -n com.example.finanzaspersonales/.MainActivity

Write-Host "¡PardoFinanzas nativo instalado y ejecutándose!" -ForegroundColor Green
