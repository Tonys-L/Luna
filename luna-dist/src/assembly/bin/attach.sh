#!/bin/bash
set -e

echo "========================================"
echo "  Luna Agent Dynamic Attach"
echo "========================================"

# Check Java
if ! command -v java &> /dev/null; then
    echo "[ERROR] Java not found. Please install JDK 8+ and add to PATH."
    exit 1
fi

# Locate jars (bin/ -> ../lib/)
SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
AGENT_JAR="$SCRIPT_DIR/../lib/luna-agent-1.0-SNAPSHOT.jar"
ATTACHER_JAR="$SCRIPT_DIR/../lib/luna-attacher-1.0-SNAPSHOT.jar"

# Verify jars exist
if [ ! -f "$AGENT_JAR" ]; then
    echo "[ERROR] Agent jar not found: $AGENT_JAR"
    exit 1
fi
if [ ! -f "$ATTACHER_JAR" ]; then
    echo "[ERROR] Attacher jar not found: $ATTACHER_JAR"
    exit 1
fi

# Detect JDK version to decide whether tools.jar is needed
JAVA_VERSION_RAW=$(java -version 2>&1 | head -n1 | awk -F\" '{print $2}')
JAVA_MAJOR=$(echo "$JAVA_VERSION_RAW" | cut -d. -f1)
if [ "$JAVA_MAJOR" = "1" ]; then
    # Old style version like 1.8.0_xxx -> major is 8
    JAVA_MAJOR=$(echo "$JAVA_VERSION_RAW" | cut -d. -f2)
fi

ATTACH_CMD="java"
if [ "$JAVA_MAJOR" = "8" ]; then
    if [ -n "$JAVA_HOME" ] && [ -f "$JAVA_HOME/lib/tools.jar" ]; then
        ATTACH_CMD="java -Xbootclasspath/a:$JAVA_HOME/lib/tools.jar"
        echo "[INFO] JDK 8 detected, using tools.jar from $JAVA_HOME/lib/tools.jar"
    else
        echo "[WARN] JDK 8 detected but tools.jar not found at \$JAVA_HOME/lib/tools.jar"
        echo "[WARN] Attach may fail. Ensure JAVA_HOME points to a JDK (not JRE) installation."
    fi
else
    echo "[INFO] JDK $JAVA_MAJOR detected, no tools.jar needed."
fi

echo ""
echo "  Agent jar:    $AGENT_JAR"
echo "  Attacher jar: $ATTACHER_JAR"
echo "========================================"
echo ""
echo "Select the target JVM from the list (enter the number)."
echo ""

$ATTACH_CMD -jar "$ATTACHER_JAR" "$AGENT_JAR"
