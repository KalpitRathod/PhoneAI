# PhoneAI - Local Physics Social & Physics Buddy Deployment Helper (PowerShell)
param (
    [string]$ModelFile = ""
)

$AdbPath = "C:\Users\Kalpit\AppData\Local\Android\Sdk\platform-tools\adb.exe"
if (-not (Test-Path $AdbPath)) { $AdbPath = "adb" }

Write-Host "========================================================" -ForegroundColor Cyan
Write-Host "  PhoneAI - Local Physics Community & Buddy Deployment   " -ForegroundColor Cyan
Write-Host "========================================================" -ForegroundColor Cyan
Write-Host ""

if ($ModelFile -eq "") {
    # Check if a model file exists
    if (Test-Path "smollm2-360m-instruct-q4_k_m.gguf") {
        $ModelFile = "smollm2-360m-instruct-q4_k_m.gguf"
    } elseif (Test-Path "gemma-2-2b-it-q4_k_m.gguf") {
        $ModelFile = "gemma-2-2b-it-q4_k_m.gguf"
    } elseif (Test-Path "qwen2.5-0.5b-instruct-q4_k_m.gguf") {
        $ModelFile = "qwen2.5-0.5b-instruct-q4_k_m.gguf"
    } else {
        $candidate = Get-ChildItem -Filter "*.gguf" | Select-Object -First 1
        if ($null -ne $candidate) {
            $ModelFile = $candidate.Name
        }
    }
}

if ($ModelFile -eq "" -or (-not (Test-Path $ModelFile))) {
    Write-Host "[ERROR] No GGUF model file found!" -ForegroundColor Red
    Write-Host "Place your .gguf file here: $PWD" -ForegroundColor Yellow
    exit 1
}

$fileSizeMb = [math]::Round((Get-Item $ModelFile).Length / 1MB, 1)
Write-Host "Selected Model: $ModelFile ($fileSizeMb MB)" -ForegroundColor Green
Write-Host "Checking connected Android devices..." -ForegroundColor Gray
& $AdbPath devices

Write-Host "[1/3] Pushing $ModelFile to /data/local/tmp/..." -ForegroundColor Yellow
& $AdbPath push $ModelFile /data/local/tmp/

if ($LASTEXITCODE -ne 0) {
    Write-Host "[Fallback] Pushing to /sdcard/Download/..." -ForegroundColor Yellow
    & $AdbPath push $ModelFile /sdcard/Download/
    Write-Host "Pushed to /sdcard/Download/. Open PhoneAI and tap 'Select Model File'." -ForegroundColor Green
} else {
    Write-Host "[2/3] Setting read permissions on /data/local/tmp/$ModelFile..." -ForegroundColor Gray
    & $AdbPath shell chmod 644 "/data/local/tmp/$ModelFile"

    Write-Host "[3/3] Copying into app private storage..." -ForegroundColor Gray
    & $AdbPath shell "run-as com.phoneai.localmodel.debug cp /data/local/tmp/$ModelFile /data/data/com.phoneai.localmodel.debug/files/$ModelFile" 2>$null

    Write-Host ""
    Write-Host "========================================================" -ForegroundColor Green
    Write-Host "Model deployed successfully! Launch PhoneAI on your device." -ForegroundColor Green
    Write-Host "========================================================" -ForegroundColor Green
}

# Optional: Push Wikipedia physics ZIM if present
if (Test-Path "wikipedia_en_physics_nopic_2026-07.zim") {
    Write-Host ""
    Write-Host "Found Wikipedia Physics dataset (wikipedia_en_physics_nopic_2026-07.zim)." -ForegroundColor Cyan
    $pushZim = Read-Host "Do you want to push the Wikipedia physics dataset too? (y/n)"
    if ($pushZim -eq "y" -or $pushZim -eq "Y") {
        Write-Host "Pushing wikipedia_en_physics_nopic_2026-07.zim..." -ForegroundColor Yellow
        & $AdbPath push "wikipedia_en_physics_nopic_2026-07.zim" /data/local/tmp/
        & $AdbPath shell chmod 644 "/data/local/tmp/wikipedia_en_physics_nopic_2026-07.zim"
        & $AdbPath shell "run-as com.phoneai.localmodel.debug cp /data/local/tmp/wikipedia_en_physics_nopic_2026-07.zim /data/data/com.phoneai.localmodel.debug/files/wikipedia_en_physics_nopic_2026-07.zim" 2>$null
        Write-Host "Wikipedia Physics dataset pushed!" -ForegroundColor Green
    }
}
