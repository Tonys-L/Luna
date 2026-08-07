#!/bin/bash
set -e

echo "========================================"
echo "  Luna Demo"
echo "========================================"

# Check Java
if ! command -v java &> /dev/null; then
    echo "[ERROR] Java not found. Please install JDK 8+ and add to PATH."
    exit 1
fi

# Locate jars (bin/ -> ../lib/)
SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
AGENT_JAR="$SCRIPT_DIR/../lib/luna-agent-1.0-SNAPSHOT.jar"
DEMO_JAR="$SCRIPT_DIR/../lib/luna-demo-app-1.0-SNAPSHOT.jar"

# Verify jars exist
if [ ! -f "$AGENT_JAR" ]; then
    echo "[ERROR] Agent jar not found: $AGENT_JAR"
    exit 1
fi
if [ ! -f "$DEMO_JAR" ]; then
    echo "[ERROR] Demo jar not found: $DEMO_JAR"
    exit 1
fi

echo ""
echo "  Agent:  $AGENT_JAR"
echo "  App:    $DEMO_JAR"
echo "  UI:     http://localhost:8421"
echo "========================================"
echo ""

# Delayed browser open (background, wait 5s for service to start)
( sleep 5 && {
    if command -v open &> /dev/null; then
        open http://localhost:8421
    elif command -v xdg-open &> /dev/null; then
        xdg-open http://localhost:8421
    fi
} ) &

# Start application with javaagent (foreground)
java -javaagent:"$AGENT_JAR" -jar "$DEMO_JAR"
