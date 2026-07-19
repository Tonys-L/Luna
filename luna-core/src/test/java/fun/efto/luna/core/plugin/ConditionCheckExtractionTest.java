package fun.efto.luna.core.plugin;

import fun.efto.luna.core.plugin.builtin.log.LogExpressionHandler;
import fun.efto.luna.core.plugin.builtin.snapshot.SnapshotExpressionHandler;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;

import static org.junit.jupiter.api.Assertions.*;

/**
 * @author : Tony.L(286269159@qq.com)
 * @since  : 2026/06/01 14:00
 */
@DisplayName("迭代4：条件检查提取 & ExpressionHandler 清理")
public class ConditionCheckExtractionTest {

    @Nested
    @DisplayName("Task 4A: LogExpressionHandler 不再包含 generateConditionCheck")
    class LogHandlerConditionCheckRemoved {

        @Test
        @DisplayName("LogExpressionHandler 不应有 generateConditionCheck 方法")
        void logHandlerShouldNotHaveGenerateConditionCheck() {
            Method[] methods = LogExpressionHandler.class.getDeclaredMethods();
            for (Method m : methods) {
                assertFalse(m.getName().equals("generateConditionCheck"),
                        "LogExpressionHandler should not contain generateConditionCheck method, found: " + m.getName());
            }
        }
    }

    @Nested
    @DisplayName("Task 4A: SnapshotExpressionHandler 不再包含 generateConditionCheck")
    class SnapshotHandlerConditionCheckRemoved {

        @Test
        @DisplayName("SnapshotExpressionHandler 不应有 generateConditionCheck 方法")
        void snapshotHandlerShouldNotHaveGenerateConditionCheck() {
            Method[] methods = SnapshotExpressionHandler.class.getDeclaredMethods();
            for (Method m : methods) {
                assertFalse(m.getName().equals("generateConditionCheck"),
                        "SnapshotExpressionHandler should not contain generateConditionCheck method, found: " + m.getName());
            }
        }
    }

    @Nested
    @DisplayName("Task 4C: ExpressionHandler 接口已删除")
    class ExpressionHandlerInterfaceRemoved {

        @Test
        @DisplayName("ExpressionHandler 类不应再存在")
        void expressionHandlerShouldNotExist() {
            assertThrows(ClassNotFoundException.class, () -> {
                Class.forName("fun.efto.luna.core.plugin.ExpressionHandler");
            }, "ExpressionHandler interface should have been deleted");
        }

        @Test
        @DisplayName("ExpressionHandlerRegistry 类不应再存在")
        void expressionHandlerRegistryShouldNotExist() {
            assertThrows(ClassNotFoundException.class, () -> {
                Class.forName("fun.efto.luna.core.plugin.registry.ExpressionHandlerRegistry");
            }, "ExpressionHandlerRegistry should have been deleted");
        }
    }

    @Nested
    @DisplayName("Task 4C: LogExpressionHandler 不再实现 ExpressionHandler")
    class LogHandlerNoLongerImplementsExpressionHandler {

        @Test
        @DisplayName("LogExpressionHandler 不应实现 ExpressionHandler 接口")
        void logHandlerShouldNotImplementExpressionHandler() {
            assertThrows(ClassNotFoundException.class, () -> {
                Class.forName("fun.efto.luna.core.plugin.ExpressionHandler");
            });
            Class<?>[] interfaces = LogExpressionHandler.class.getInterfaces();
            assertEquals(0, interfaces.length, "LogExpressionHandler should not implement any interface");
        }
    }

    @Nested
    @DisplayName("Task 4C: SnapshotExpressionHandler 不再实现 ExpressionHandler")
    class SnapshotHandlerNoLongerImplementsExpressionHandler {

        @Test
        @DisplayName("SnapshotExpressionHandler 不应实现 ExpressionHandler 接口")
        void snapshotHandlerShouldNotImplementExpressionHandler() {
            assertThrows(ClassNotFoundException.class, () -> {
                Class.forName("fun.efto.luna.core.plugin.ExpressionHandler");
            });
            Class<?>[] interfaces = SnapshotExpressionHandler.class.getInterfaces();
            assertEquals(0, interfaces.length, "SnapshotExpressionHandler should not implement any interface");
        }
    }
}
