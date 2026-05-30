@echo off
chcp 65001 >nul 2>&1
setlocal EnableDelayedExpansion

echo ========================================
echo   Luna Injection End-to-End Test
echo ========================================

set "PROJECT_ROOT=%~dp0..\.."
set "AGENT_JAR=%PROJECT_ROOT%\luna-agent\target\luna-agent-1.0-SNAPSHOT.jar"
set "DEMO_JAR=%PROJECT_ROOT%\example\luna-demo-app\target\luna-demo-app-1.0-SNAPSHOT.jar"
set "BASE_URL=http://localhost:8421"

echo.
echo [1/6] Building Luna Agent...
cd /d "%PROJECT_ROOT%"
call mvn package -DskipTests -pl luna-core,luna-agent -am -q
if %ERRORLEVEL% neq 0 (
    echo [FAIL] Build failed
    exit /b 1
)

echo [2/6] Building Demo App...
cd /d "%PROJECT_ROOT%\example\luna-demo-app"
call mvn package -DskipTests -q
if %ERRORLEVEL% neq 0 (
    echo [FAIL] Demo build failed
    exit /b 1
)

echo [3/6] Starting Demo App with Agent...
start /B java -javaagent:"%AGENT_JAR%" -jar "%DEMO_JAR%" > nul 2>&1

echo [4/6] Waiting for Agent to be ready...
set READY=0
for /L %%i in (1,1,30) do (
    if !READY! equ 0 (
        curl -s "%BASE_URL%/api/status" > nul 2>&1
        if !ERRORLEVEL! equ 0 (
            set READY=1
            echo   Agent ready after %%i seconds
        ) else (
            timeout /t 1 /nobreak > nul
        )
    )
)
if %READY% equ 0 (
    echo [FAIL] Agent did not start within 30 seconds
    taskkill /F /IM java.exe /FI "WINDOWTITLE eq Luna*" > nul 2>&1
    exit /b 1
)

echo [5/6] Testing injection...
echo   - Dry run...
curl -s -X POST "%BASE_URL%/api/injections/dry-run" -H "Content-Type: application/json" -d "{\"clazz\":\"fun.efto.luna.demo.service.UserService\",\"method\":\"createUser\",\"injectionType\":\"ENTER\",\"codeType\":\"EXPRESSION\",\"code\":\"log:test-injection\"}"

echo.
echo   - Injecting...
curl -s -X POST "%BASE_URL%/api/injections" -H "Content-Type: application/json" -d "{\"clazz\":\"fun.efto.luna.demo.service.UserService\",\"method\":\"createUser\",\"injectionType\":\"ENTER\",\"codeType\":\"EXPRESSION\",\"code\":\"log:test-injection\"}"

echo.
echo [6/6] Verifying injection...
set VERIFY_RESULT=
for /f "delims=" %%r in ('curl -s -X POST "%BASE_URL%/api/injections/verify" -H "Content-Type: application/json" -d "{\"clazz\":\"fun.efto.luna.demo.service.UserService\",\"method\":\"createUser\",\"injectionType\":\"ENTER\",\"codeType\":\"EXPRESSION\",\"code\":\"log:verify-test\"}"') do set VERIFY_RESULT=%%r

echo   Verify result: %VERIFY_RESULT%
echo %VERIFY_RESULT% | findstr /C:"method enter" > nul
if %ERRORLEVEL% equ 0 (
    echo.
    echo ========================================
    echo   TEST RESULT: PASS
    echo ========================================
) else (
    echo.
    echo ========================================
    echo   TEST RESULT: FAIL
    echo   Injection output not found in response
    echo ========================================
)

echo.
echo Cleaning up...
for /f "tokens=2" %%p in ('tasklist /FI "IMAGENAME eq java.exe" /FO LIST ^| findstr "PID"') do (
    wmic process where "ProcessId=%%p and CommandLine like '%%luna-demo-app%%'" call terminate > nul 2>&1
)

echo Done.
