package fun.efto.luna.agent.e2e;

import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import io.restassured.response.Response;
import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;

import java.io.File;
import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static io.restassured.RestAssured.given;
import static org.awaitility.Awaitility.await;

/**
 * E2E 测试基础设施：管理目标进程（Demo app + Agent）、HTTP 客户端、WebSocket 客户端。
 *
 * <p>使用方式：
 * <pre>
 *   E2ETestHarness harness = new E2ETestHarness();
 *   harness.start();       // 启动 Demo app（带 -javaagent）
 *   harness.awaitReady();  // 等待 /api/status 返回 200
 *   // ... 执行测试 ...
 *   harness.stop();        // 清理进程
 * </pre>
 *
 * @author : Tony.L(286269159@qq.com)
 * @since  : 2026/08/02 21:00
 */
public class E2ETestHarness {

    public static final String TARGET_CLASS = "fun.efto.luna.demo.service.UserService";
    public static final String TARGET_METHOD = "createUser";
    public static final String TARGET_DESC = "(Ljava/lang/String;I)Lfun/efto/luna/demo/model/User;";
    public static final String TARGET_CLASS_NAME_SHORT = "UserService";

    public static final int AGENT_PORT = 8421;
    public static final String BASE_URL = "http://localhost:" + AGENT_PORT;

    private Process demoProcess;
    private LogWebSocketClient wsClient;

    /**
     * 启动 Demo app 进程，通过 -javaagent 加载 Agent。
     */
    public void start() throws Exception {
        File agentJar = locateJar("luna-agent", "luna-agent");
        File demoJar = locateJar("example/luna-demo-app", "luna-demo-app");

        cleanPersistenceFiles();

        ProcessBuilder pb = new ProcessBuilder(
                "java",
                "-javaagent:" + agentJar.getAbsolutePath(),
                "-jar", demoJar.getAbsolutePath()
        );
        pb.redirectErrorStream(true);
        pb.redirectOutput(ProcessBuilder.Redirect.INHERIT);
        demoProcess = pb.start();

        Runtime.getRuntime().addShutdownHook(new Thread(this::stop));
    }

    /**
     * 等待 Agent HTTP 服务就绪（/api/status 返回 200）。
     */
    public void awaitReady() {
        RestAssured.baseURI = BASE_URL;
        await().atMost(60, TimeUnit.SECONDS).pollInterval(1, TimeUnit.SECONDS).until(() -> {
            try {
                Response resp = given().get("/api/status");
                return resp.getStatusCode() == 200;
            } catch (Exception e) {
                return false;
            }
        });
    }

    /**
     * 连接 WebSocket /ws/log，收集 ProbeMessage。
     */
    public void connectWebSocket() throws Exception {
        wsClient = new LogWebSocketClient(new URI("ws://localhost:" + AGENT_PORT + "/ws/log"));
        wsClient.connectBlocking(10, TimeUnit.SECONDS);
    }

    /**
     * 停止 Demo app 进程。
     */
    public void stop() {
        if (wsClient != null) {
            try { wsClient.closeBlocking(); } catch (Exception ignored) {}
            wsClient = null;
        }
        if (demoProcess != null) {
            demoProcess.destroyForcibly();
            try {
                demoProcess.waitFor(5, TimeUnit.SECONDS);
            } catch (InterruptedException ignored) {}
            demoProcess = null;
        }
        cleanPersistenceFiles();
    }

    /**
     * 获取 WebSocket 收到的所有消息。
     */
    public List<String> getReceivedMessages() {
        return wsClient != null ? wsClient.getMessages() : new ArrayList<>();
    }

    /**
     * 清空 WebSocket 收到的消息。
     */
    public void clearMessages() {
        if (wsClient != null) wsClient.clear();
    }

    /**
     * 等待 WebSocket 收到至少 N 条消息，超时 10 秒。
     */
    public void awaitMessages(int count) {
        await().atMost(15, TimeUnit.SECONDS).pollInterval(500, TimeUnit.MILLISECONDS).until(() ->
                getReceivedMessages().size() >= count);
    }

    /**
     * 注入 LOG 探针。
     */
    public Response injectLog(String className, String method, String desc,
                              String injectionType, String code) {
        Map<String, Object> body = new HashMap<>();
        body.put("clazz", className);
        body.put("method", method);
        body.put("desc", desc);
        body.put("injectionType", injectionType);
        body.put("codeType", "EXPRESSION");
        body.put("code", code);
        return given()
                .contentType(ContentType.JSON)
                .body(body)
                .post("/api/injections");
    }

    /**
     * 注入行号级 LOG 探针。
     */
    public Response injectLineLog(String className, String method, String desc,
                                  String injectionType, String code, int lineNumber) {
        Map<String, Object> body = new HashMap<>();
        body.put("clazz", className);
        body.put("method", method);
        body.put("desc", desc);
        body.put("injectionType", injectionType);
        body.put("codeType", "EXPRESSION");
        body.put("code", code);
        body.put("lineNumber", lineNumber);
        return given()
                .contentType(ContentType.JSON)
                .body(body)
                .post("/api/injections");
    }

    /**
     * 查询注入列表。
     */
    public Response listInjections() {
        return given().get("/api/injections/list");
    }

    /**
     * 删除注入。
     */
    public Response deleteInjection(String id) {
        return given().delete("/api/injections/" + id);
    }

    /**
     * dry-run 注入（不实际注入）。
     */
    public Response dryRunInjection(Map<String, Object> body) {
        return given()
                .contentType(ContentType.JSON)
                .body(body)
                .post("/api/injections/dry-run");
    }

    /**
     * 调用目标方法（通过 /api/test/invoke）。
     */
    public Response invokeMethod(String className, String methodName, String desc,
                                 List<Object> args) {
        Map<String, Object> body = new HashMap<>();
        body.put("className", className);
        body.put("methodName", methodName);
        body.put("desc", desc);
        body.put("args", args);
        return given()
                .contentType(ContentType.JSON)
                .body(body)
                .post("/api/test/invoke");
    }

    private File locateJar(String modulePath, String artifactId) {
        File baseDir = new File(System.getProperty("user.dir"));
        // 测试运行目录可能是 luna-agent 或 luna 根目录
        File jar = new File(baseDir, modulePath + "/target/" + artifactId + "-1.0-SNAPSHOT.jar");
        if (!jar.exists() && baseDir.getName().equals("luna-agent")) {
            jar = new File(baseDir.getParentFile(), modulePath + "/target/" + artifactId + "-1.0-SNAPSHOT.jar");
        }
        if (!jar.exists() && baseDir.getParentFile() != null) {
            jar = new File(baseDir.getParentFile(), modulePath + "/target/" + artifactId + "-1.0-SNAPSHOT.jar");
        }
        if (!jar.exists()) {
            throw new IllegalStateException("JAR not found: " + jar.getAbsolutePath()
                    + ". Run 'mvn package -DskipTests' first.");
        }
        return jar;
    }

    private void cleanPersistenceFiles() {
        String[] files = {"luna-injections.json", "luna-rules.json"};
        for (String f : files) {
            File file = new File(f);
            if (file.exists()) {
                file.delete();
            }
        }
    }

    /**
     * WebSocket 客户端，收集 ProbeMessage 原始文本。
     */
    private static class LogWebSocketClient extends WebSocketClient {
        private final List<String> messages = new ArrayList<>();

        public LogWebSocketClient(URI serverUri) {
            super(serverUri);
        }

        @Override
        public void onOpen(ServerHandshake handshake) {}

        @Override
        public void onMessage(String message) {
            messages.add(message);
        }

        @Override
        public void onClose(int code, String reason, boolean remote) {}

        @Override
        public void onError(Exception ex) {}

        public List<String> getMessages() {
            synchronized (messages) {
                return new ArrayList<>(messages);
            }
        }

        public void clear() {
            synchronized (messages) {
                messages.clear();
            }
        }
    }
}
