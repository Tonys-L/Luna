@echo off
chcp 65001 >nul 2>&1
setlocal EnableDelayedExpansion

echo ========================================
echo   Luna Agent Dynamic Attach
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

:: Set project root
set "PROJECT_ROOT=%~dp0.."
set "AGENT_JAR=%PROJECT_ROOT%\luna-agent\target\luna-agent-1.0-SNAPSHOT.jar"
set "DEMO_JAR=%PROJECT_ROOT%\example\luna-demo-app\target\luna-demo-app-1.0-SNAPSHOT.jar"
set "ATTACHER_JAR=%PROJECT_ROOT%\luna-attacher\target\luna-attacher-1.0-SNAPSHOT.jar"

:: Step 1: Build Luna
echo.
echo [1/3] Building Luna...
cd /d "%PROJECT_ROOT%"
call mvn package -DskipTests -q
if %ERRORLEVEL% neq 0 (
    echo [ERROR] Failed to build Luna.
    pause
    exit /b 1
)

:: Step 2: Build Demo App
echo [2/3] Building Demo Application...
cd /d "%PROJECT_ROOT%\example\luna-demo-app"
call mvn package -DskipTests -q
if %ERRORLEVEL% neq 0 (
    echo [ERROR] Failed to build Demo Application.
    pause
    exit /b 1
)

:: Step 3: Start Demo App WITHOUT Agent
echo [3/3] Starting Demo Application (without agent)...
echo.
echo ========================================
echo   App:    %DEMO_JAR%
echo   UI:     http://localhost:8421 (after attach)
echo ========================================
echo.
echo After the app starts, run Attacher in another terminal:
echo   java -jar "%ATTACHER_JAR%"
echo.
echo Or use jps + jstat to find the PID, then:
echo   java -jar "%ATTACHER_JAR%" "%AGENT_JAR%"
echo.

:: Start application without agent
start "Luna Demo App" java -jar "%DEMO_JAR%"

echo Demo App started in a new window.
echo Now run Attacher to attach the Luna Agent.
echo.

pause
