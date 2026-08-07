package fun.efto.luna.agent.e2e;

import io.restassured.response.Response;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static io.restassured.RestAssured.given;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.notNullValue;

/**
 * 注入域端到端测试：覆盖 P0 核心场景。
 *
 * <p>测试链路：Agent 启动 → HTTP API → 字节码注入 → 探针输出。
 *
 * <p>前置条件：先执行 `mvn package -DskipTests` 生成 luna-agent 和 luna-demo-app 的 JAR。
 * 通过 Maven profile e2e 激活：`mvn test -Pe2e -pl luna-agent -am`。
 *
 * @author : Tony.L(286269159@qq.com)
 * @since  : 2026/08/02 21:30
 */
@DisplayName("注入域 E2E 测试")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class InjectionE2ETest {

    private static E2ETestHarness harness;

    @BeforeAll
    static void startAgent() throws Exception {
        harness = new E2ETestHarness();
        harness.start();
        harness.awaitReady();
        harness.connectWebSocket();
    }

    @AfterAll
    static void stopAgent() {
        if (harness != null) {
            harness.stop();
        }
    }

    /**
     * E2E-01: Agent 启动后 /api/status 返回 200 且 success=true。
     */
    @Test
    @Order(1)
    @DisplayName("E2E-01: Agent 启动后 /api/status 返回成功")
    void agentStatusShouldBeRunning() {
        given()
                .get("/api/status")
                .then()
                .statusCode(200)
                .body("success", is(true));
    }

    /**
     * E2E-04: 注入生命周期 - 注入 → list 包含 → delete → list 不包含。
     */
    @Test
    @Order(2)
    @DisplayName("E2E-04: 注入生命周期（注入 → 查询 → 删除 → 再查询）")
    void injectionLifecycleShouldBeConsistent() {
        // 1. 注入 METHOD_ENTER LOG 探针
        Response injectResp = harness.injectLog(
                E2ETestHarness.TARGET_CLASS,
                E2ETestHarness.TARGET_METHOD,
                E2ETestHarness.TARGET_DESC,
                "METHOD_ENTER",
                "log:Lifecycle test entered"
        );
        injectResp.then().statusCode(200).body("success", is(true));

        String injectionPointId = injectResp.jsonPath().getString("data.injectionPointId");
        assertThat("injectionPointId 不能为空", injectionPointId, notNullValue());
        assertThat("injectionPointId 不能为空字符串", injectionPointId.isEmpty(), is(false));

        // 2. 查询 list 应包含该注入
        Response listResp1 = harness.listInjections();
        listResp1.then().statusCode(200);
        List<String> ids = listResp1.jsonPath().getList("data.id");
        assertThat("list 应包含刚注入的 id", ids, hasItem(injectionPointId));

        // 3. 删除注入
        harness.deleteInjection(injectionPointId).then().statusCode(200);

        // 4. 查询 list 不应再包含该注入
        Response listResp2 = harness.listInjections();
        List<String> idsAfter = listResp2.jsonPath().getList("data.id");
        if (idsAfter != null) {
            assertThat("list 不应包含已删除的 id", idsAfter, not(hasItem(injectionPointId)));
        }
    }

    /**
     * E2E-02: METHOD_ENTER LOG 注入 → 触发目标方法 → WebSocket 收到 log 类型 ProbeMessage。
     */
    @Test
    @Order(3)
    @DisplayName("E2E-02: METHOD_ENTER 注入并触发，WebSocket 应收到探针消息")
    void methodEnterInjectionShouldProduceProbeMessage() {
        harness.clearMessages();

        // 1. 注入 METHOD_ENTER LOG 探针
        Response injectResp = harness.injectLog(
                E2ETestHarness.TARGET_CLASS,
                E2ETestHarness.TARGET_METHOD,
                E2ETestHarness.TARGET_DESC,
                "METHOD_ENTER",
                "log:E2E-02 enter: $1"
        );
        injectResp.then().statusCode(200).body("success", is(true));
        String injectionPointId = injectResp.jsonPath().getString("data.injectionPointId");

        try {
            // 2. 触发目标方法
            harness.invokeMethod(
                    E2ETestHarness.TARGET_CLASS,
                    E2ETestHarness.TARGET_METHOD,
                    E2ETestHarness.TARGET_DESC,
                    Arrays.asList("e2e-user-02", 25)
            );

            // 3. 等待 WebSocket 消息（至少 1 条）
            harness.awaitMessages(1);

            // 4. 验证消息内容包含探针文本
            List<String> messages = harness.getReceivedMessages();
            assertThat("应收到至少 1 条 WebSocket 消息", messages.size(), greaterThan(0));
            boolean found = messages.stream().anyMatch(m -> m.contains("E2E-02 enter"));
            assertThat("WebSocket 消息应包含探针输出 'E2E-02 enter'", found, is(true));
        } finally {
            // 清理：删除注入
            harness.deleteInjection(injectionPointId);
        }
    }

    /**
     * E2E-03: LINE_BEFORE LOG 注入 → 触发目标方法 → WebSocket 收到消息且 className/methodName 正确。
     */
    @Test
    @Order(4)
    @DisplayName("E2E-03: LINE_BEFORE 注入并触发，WebSocket 应收到带 className 的消息")
    void lineBeforeInjectionShouldProduceProbeMessageWithClassInfo() {
        harness.clearMessages();

        // UserService.createUser 中第 22 行 System.out.println
        Response injectResp = harness.injectLineLog(
                E2ETestHarness.TARGET_CLASS,
                E2ETestHarness.TARGET_METHOD,
                E2ETestHarness.TARGET_DESC,
                "LINE_BEFORE",
                "log:Line before test",
                22
        );
        injectResp.then().statusCode(200).body("success", is(true));
        String injectionPointId = injectResp.jsonPath().getString("data.injectionPointId");

        try {
            // 触发目标方法
            harness.invokeMethod(
                    E2ETestHarness.TARGET_CLASS,
                    E2ETestHarness.TARGET_METHOD,
                    E2ETestHarness.TARGET_DESC,
                    Arrays.asList("e2e-user-03", 30)
            );

            harness.awaitMessages(1);

            List<String> messages = harness.getReceivedMessages();
            assertThat("应收到至少 1 条 WebSocket 消息", messages.size(), greaterThan(0));
            // 消息中应包含目标类名（ProbeMessage 通常携带 className 字段）
            boolean foundClass = messages.stream().anyMatch(m ->
                    m.contains("UserService") || m.contains(E2ETestHarness.TARGET_CLASS));
            assertThat("WebSocket 消息应包含 className 信息", foundClass, is(true));
        } finally {
            harness.deleteInjection(injectionPointId);
        }
    }

    /**
     * E2E-05: dry-run 注入不实际生效，返回字节码大小信息。
     */
    @Test
    @Order(5)
    @DisplayName("E2E-05: dry-run 注入不实际生效，返回字节码差异")
    void dryRunShouldNotActuallyInject() {
        // 先清空消息并记录当前 list
        harness.clearMessages();
        int countBefore = harness.listInjections().jsonPath().getList("data").size();

        // 执行 dry-run
        Map<String, Object> dryRunBody = new HashMap<>();
        dryRunBody.put("clazz", E2ETestHarness.TARGET_CLASS);
        dryRunBody.put("method", E2ETestHarness.TARGET_METHOD);
        dryRunBody.put("desc", E2ETestHarness.TARGET_DESC);
        dryRunBody.put("injectionType", "METHOD_ENTER");
        dryRunBody.put("codeType", "EXPRESSION");
        dryRunBody.put("code", "log:Dry run test");
        Response dryRunResp = harness.dryRunInjection(dryRunBody);
        dryRunResp.then().statusCode(200).body("success", is(true));

        // dry-run 后 list 数量不应增加
        int countAfter = harness.listInjections().jsonPath().getList("data").size();
        assertThat("dry-run 不应实际增加注入数量", countAfter, equalTo(countBefore));
    }

    /**
     * E2E-06: 注入不存在的方法应失败。
     */
    @Test
    @Order(6)
    @DisplayName("E2E-06: 注入不存在的方法应返回失败")
    void injectingNonExistentMethodShouldFail() {
        Response resp = harness.injectLog(
                E2ETestHarness.TARGET_CLASS,
                "nonExistentMethod",
                "()V",
                "METHOD_ENTER",
                "log:should fail"
        );
        resp.then().statusCode(200);
        // 应返回失败（success=false 或错误信息）
        boolean success = resp.jsonPath().getBoolean("success");
        assertThat("注入不存在的方法应失败", success, is(false));
    }
}
