@echo off
chcp 65001 >nul 2>&1
setlocal EnableDelayedExpansion

echo ========================================
echo   Luna Agent Quick Start
echo ========================================

:: Check Java
where java >nul 2>&1
if %ERRORLEVEL% neq 0 (
    echo [ERROR] Java not found. Please install JDK 8+ and add to PATH.
    pause
    exit /b 1
)

:: Check Maven
where mvn >nul 2>&1
if %ERRORLEVEL% neq 0 (
    echo [ERROR] Maven not found. Please install Maven and add to PATH.
    pause
    exit /b 1
)

:: Set project root (scripts/ -> example/ -> Luna root)
set "PROJECT_ROOT=%~dp0..\.."
set "AGENT_JAR=%PROJECT_ROOT%\luna-agent\target\luna-agent-1.0-SNAPSHOT.jar"
set "DEMO_JAR=%PROJECT_ROOT%\example\luna-demo-app\target\luna-demo-app-1.0-SNAPSHOT.jar"

:: Step 1: Build Luna Agent
echo.
echo [1/3] Building Luna Agent...
cd /d "%PROJECT_ROOT%"
call mvn clean
call mvn package -DskipTests -pl luna-core,luna-agent -am -q
if %ERRORLEVEL% neq 0 (
    echo [ERROR] Failed to build Luna Agent.
    pause
    exit /b 1
)

:: Step 2: Build Demo App
echo [2/3] Building Demo Application...
cd /d "%PROJECT_ROOT%\example\luna-demo-app"
call mvn clean
call mvn package -DskipTests -q
if %ERRORLEVEL% neq 0 (
    echo [ERROR] Failed to build Demo Application.
    pause
    exit /b 1
)

:: Step 3: Start Demo App with Agent
echo [3/3] Starting Demo Application with Luna Agent...
echo.
echo ========================================
echo   Agent:  %AGENT_JAR%
echo   App:    %DEMO_JAR%
echo   UI:     http://localhost:8421
echo ========================================
echo.

:: Try to open browser
start http://localhost:8421

:: Start application with javaagent
java -javaagent:"%AGENT_JAR%" -jar "%DEMO_JAR%"

pause
