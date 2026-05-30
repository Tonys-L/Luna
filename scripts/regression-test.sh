#!/bin/bash
# Luna 一键回归测试脚本
# 流程：构建 → 清理 → 启动 → 等待就绪 → 运行测试 → 报告 → 清理

set -e

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
PROJECT_DIR="$(dirname "$SCRIPT_DIR")"
AGENT_JAR="$PROJECT_DIR/luna-agent/target/luna-agent-1.0-SNAPSHOT.jar"
DEMO_JAR="$PROJECT_DIR/example/luna-demo-app/target/luna-demo-app-1.0-SNAPSHOT.jar"
API_BASE="http://localhost:8421"
MAX_WAIT=60
JAVA_PID=""

# 颜色输出
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m'

log_info()  { echo -e "${GREEN}[INFO]${NC} $1"; }
log_warn()  { echo -e "${YELLOW}[WARN]${NC} $1"; }
log_error() { echo -e "${RED}[ERROR]${NC} $1"; }

cleanup() {
    if [ -n "$JAVA_PID" ]; then
        log_info "Stopping Agent + Demo App (PID: $JAVA_PID)..."
        kill $JAVA_PID 2>/dev/null || true
        wait $JAVA_PID 2>/dev/null || true
        JAVA_PID=""
    fi
    log_info "Cleanup complete."
}

trap cleanup EXIT

# Step 1: Build
log_info "Step 1/6: Building project..."
cd "$PROJECT_DIR"
mvn package -DskipTests -q
log_info "Build successful."

# Step 2: Clean persistence files
log_info "Step 2/6: Cleaning persistence files..."
rm -f "$PROJECT_DIR/luna-rules.json" "$PROJECT_DIR/luna-injections.json"
rm -f "$PROJECT_DIR/luna-core/luna-rules.json" "$PROJECT_DIR/luna-core/luna-injections.json"
rm -f "$PROJECT_DIR/example/luna-demo-app/luna-rules.json" "$PROJECT_DIR/example/luna-demo-app/luna-injections.json"
log_info "Persistence files cleaned."

# Step 3: Start Agent + Demo App
log_info "Step 3/6: Starting Agent + Demo App..."
java -javaagent:"$AGENT_JAR" -jar "$DEMO_JAR" &
JAVA_PID=$!

# Step 4: Wait for ready
log_info "Step 4/6: Waiting for service to be ready (max ${MAX_WAIT}s)..."
elapsed=0
while [ $elapsed -lt $MAX_WAIT ]; do
    if curl -s -o /dev/null -w "%{http_code}" "$API_BASE/api/status" 2>/dev/null | grep -q "200"; then
        log_info "Service is ready after ${elapsed}s."
        break
    fi
    sleep 2
    elapsed=$((elapsed + 2))
done

if [ $elapsed -ge $MAX_WAIT ]; then
    log_error "Service failed to start within ${MAX_WAIT}s. Aborting."
    exit 1
fi

# Step 5: Run E2E tests
log_info "Step 5/6: Running E2E regression tests..."
cd "$PROJECT_DIR/luna-ui"
TEST_EXIT_CODE=0
npx playwright test e2e/api.spec.js || TEST_EXIT_CODE=$?

# Step 6: Report
if [ $TEST_EXIT_CODE -eq 0 ]; then
    log_info "Step 6/6: All regression tests PASSED!"
else
    log_error "Step 6/6: Some regression tests FAILED (exit code: $TEST_EXIT_CODE)"
fi

exit $TEST_EXIT_CODE
