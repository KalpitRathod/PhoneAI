@echo off
setlocal enabledelayedexpansion

echo ========================================================
echo   PhoneAI - Local Physics Community & Buddy Deployment
echo ========================================================
echo.

set ADB_PATH=C:\Users\Kalpit\AppData\Local\Android\Sdk\platform-tools\adb.exe
if not exist "%ADB_PATH%" set ADB_PATH=adb

set MODEL_FILE=%~1

if "%MODEL_FILE%"=="" (
    if exist "smollm2-360m-instruct-q4_k_m.gguf" (
        set MODEL_FILE=smollm2-360m-instruct-q4_k_m.gguf
    ) else if exist "gemma-2-2b-it-q4_k_m.gguf" (
        set MODEL_FILE=gemma-2-2b-it-q4_k_m.gguf
    ) else if exist "qwen2.5-0.5b-instruct-q4_k_m.gguf" (
        set MODEL_FILE=qwen2.5-0.5b-instruct-q4_k_m.gguf
    ) else (
        for %%f in (*.gguf) do (
            set MODEL_FILE=%%f
            goto :found
        )
    )
)

:found
if "%MODEL_FILE%"=="" (
    echo [ERROR] No GGUF model file found!
    echo Place your .gguf file in this folder and re-run.
    pause
    exit /b 1
)

echo Selected Model: %MODEL_FILE%
echo Checking connected Android devices...
"%ADB_PATH%" devices
echo.

echo [1/3] Pushing %MODEL_FILE% to /data/local/tmp/...
"%ADB_PATH%" push "%MODEL_FILE%" /data/local/tmp/
if %ERRORLEVEL% NEQ 0 (
    echo [WARNING] Failed to push to /data/local/tmp. Falling back to /sdcard/Download/...
    "%ADB_PATH%" push "%MODEL_FILE%" /sdcard/Download/
    echo Pushed to /sdcard/Download/. Open PhoneAI and tap "Select Model File" to import it.
) else (
    echo [2/3] Setting read permissions on /data/local/tmp/%MODEL_FILE%...
    "%ADB_PATH%" shell chmod 644 "/data/local/tmp/%MODEL_FILE%"
    
    echo [3/3] Copying directly into debug app files directory if installed...
    "%ADB_PATH%" shell "run-as com.phoneai.localmodel.debug cp /data/local/tmp/%MODEL_FILE% /data/data/com.phoneai.localmodel.debug/files/%MODEL_FILE%" 2>nul
    
    echo.
    echo ========================================================
    echo Deployment completed!
    echo Open the PhoneAI app on your phone.
    echo ========================================================
)

pause
