@echo off
chcp 65001 >nul 2>&1

echo ========================================
echo   Luna Demo
echo ========================================

:: Check Java
where java >nul 2>&1
if errorlevel 1 (
    echo [ERROR] Java not found. Please install JDK 8+ and add to PATH.
    pause
    exit /b 1
)

:: Locate jars (bin/ -> ../lib/)
set "AGENT_JAR=%~dp0..\lib\luna-agent-1.0-SNAPSHOT.jar"
set "DEMO_JAR=%~dp0..\lib\luna-demo-app-1.0-SNAPSHOT.jar"

:: Verify jars exist
if not exist "%AGENT_JAR%" (
    echo [ERROR] Agent jar not found: %AGENT_JAR%
    pause
    exit /b 1
)
if not exist "%DEMO_JAR%" (
    echo [ERROR] Demo jar not found: %DEMO_JAR%
    pause
    exit /b 1
)

echo.
echo   Agent:  %AGENT_JAR%
echo   App:    %DEMO_JAR%
echo   UI:     http://localhost:8421
echo ========================================
echo.

:: Delayed browser open (background, wait 5s for service to start)
start "" cmd /c "timeout /t 5 /nobreak >nul & start http://localhost:8421"

:: Start application with javaagent (foreground)
java -javaagent:"%AGENT_JAR%" -jar "%DEMO_JAR%"

pause
