@echo off
chcp 65001 >nul 2>&1

echo ========================================
echo   Luna Agent Dynamic Attach
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
set "ATTACHER_JAR=%~dp0..\lib\luna-attacher-1.0-SNAPSHOT.jar"

:: Verify jars exist
if not exist "%AGENT_JAR%" (
    echo [ERROR] Agent jar not found: %AGENT_JAR%
    pause
    exit /b 1
)
if not exist "%ATTACHER_JAR%" (
    echo [ERROR] Attacher jar not found: %ATTACHER_JAR%
    pause
    exit /b 1
)

:: Detect JDK version to decide whether tools.jar is needed
set "ATTACH_CMD=java"
for /f "tokens=3" %%v in ('java -version 2^>^&1 ^| findstr /i "version"') do (
    set "JAVA_VERSION_RAW=%%v"
)
set "JAVA_VERSION_RAW=%JAVA_VERSION_RAW:"=%"
for /f "tokens=1 delims=." %%a in ("%JAVA_VERSION_RAW%") do set "JAVA_MAJOR=%%a"

:: JDK 8 needs tools.jar on bootclasspath; JDK 9+ uses jdk.attach module
if "%JAVA_MAJOR%"=="1" (
    for /f "tokens=2 delims=." %%b in ("%JAVA_VERSION_RAW%") do set "JAVA_MAJOR=%%b"
)
if "%JAVA_MAJOR%"=="8" (
    if exist "%JAVA_HOME%\lib\tools.jar" (
        set "ATTACH_CMD=java -Xbootclasspath/a:%JAVA_HOME%\lib\tools.jar"
        echo [INFO] JDK 8 detected, using tools.jar from %JAVA_HOME%\lib\tools.jar
    ) else (
        echo [WARN] JDK 8 detected but tools.jar not found at %JAVA_HOME%\lib\tools.jar
        echo [WARN] Attach may fail. Ensure JAVA_HOME points to a JDK ^(not JRE^) installation.
    )
) else (
    echo [INFO] JDK %JAVA_MAJOR% detected, no tools.jar needed.
)

echo.
echo   Agent jar:    %AGENT_JAR%
echo   Attacher jar: %ATTACHER_JAR%
echo ========================================
echo.
echo Select the target JVM from the list ^(enter the number^).
echo.

%ATTACH_CMD% -jar "%ATTACHER_JAR%" "%AGENT_JAR%"

pause
