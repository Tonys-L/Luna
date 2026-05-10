#!/bin/bash
set -e

echo "========================================"
echo "  Luna Agent Quick Start"
echo "========================================"

# Check Java
if ! command -v java &> /dev/null; then
    echo "[ERROR] Java not found. Please install JDK 8+ and add to PATH."
    exit 1
fi

# Check Maven
if ! command -v mvn &> /dev/null; then
    echo "[ERROR] Maven not found. Please install Maven and add to PATH."
    exit 1
fi

# Set project root (scripts/ -> example/ -> Luna root)
SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
PROJECT_ROOT="$(cd "$SCRIPT_DIR/../.." && pwd)"
AGENT_JAR="$PROJECT_ROOT/luna-agent/target/luna-agent-1.0-SNAPSHOT.jar"
DEMO_JAR="$PROJECT_ROOT/example/luna-demo-app/target/luna-demo-app-1.0-SNAPSHOT.jar"

# Step 1: Build Luna Agent
echo ""
echo "[1/3] Building Luna Agent..."
cd "$PROJECT_ROOT"
mvn package -DskipTests -pl luna-core,luna-agent -am -q

# Step 2: Build Demo App
echo "[2/3] Building Demo Application..."
cd "$PROJECT_ROOT/example/luna-demo-app"
mvn package -DskipTests -q

# Step 3: Start Demo App with Agent
echo "[3/3] Starting Demo Application with Luna Agent..."
echo ""
echo "========================================"
echo "  Agent:  $AGENT_JAR"
echo "  App:    $DEMO_JAR"
echo "  UI:     http://localhost:8421"
echo "========================================"
echo ""

# Try to open browser
if command -v open &> /dev/null; then
    open http://localhost:8421
elif command -v xdg-open &> /dev/null; then
    xdg-open http://localhost:8421
fi

# Start application with javaagent
java -javaagent:"$AGENT_JAR" -jar "$DEMO_JAR"
