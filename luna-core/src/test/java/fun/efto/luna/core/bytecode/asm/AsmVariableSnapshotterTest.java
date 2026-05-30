package fun.efto.luna.core.bytecode.asm;

import fun.efto.luna.core.testing.LineInjectionTestHelper;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * @author ：Tony.L(286269159@qq.com)
 * @since  : 2026/05/31 21:00
 */
@DisplayName("AsmVariableSnapshotter 测试")
class AsmVariableSnapshotterTest {

    private static byte[] bytecode;
    private static int firstLine;

    @BeforeAll
    static void setUp() {
        bytecode = LineInjectionTestHelper.getClassBytecode(
                LineInjectionTestHelper.TestTargetService.class);
        firstLine = LineInjectionTestHelper.findFirstMethodLine(bytecode, "processWithPrimitives");
    }

    @Nested
    @DisplayName("接口实现")
    class InterfaceTests {

        @Test
        @DisplayName("AsmVariableSnapshotter 实现 VariableSnapshotter 接口")
        void testImplementsInterface() {
            VariableSnapshotter snapshotter = new AsmVariableSnapshotter();
            assertNotNull(snapshotter);
            assertTrue(snapshotter instanceof VariableSnapshotter);
        }
    }

    @Nested
    @DisplayName("snapshot 方法")
    class SnapshotTests {

        @Test
        @DisplayName("snapshot 返回结果与 LocalVariableScanner 一致")
        void testSnapshotConsistentWithScanner() {
            VariableSnapshotter snapshotter = new AsmVariableSnapshotter();

            List<VariableInfo> snapshotResult = snapshotter.snapshot(
                    bytecode, "processWithPrimitives", "(BSIJFDCZ)V", firstLine);

            List<AsmInjectionContext.LocalVarInfo> scannerResult =
                    LocalVariableScanner.scanVisibleLocalVariables(
                            bytecode, "processWithPrimitives", "(BSIJFDCZ)V", firstLine);

            assertEquals(scannerResult.size(), snapshotResult.size(),
                    "snapshot 结果数量应与 LocalVariableScanner 一致");

            for (int i = 0; i < scannerResult.size(); i++) {
                AsmInjectionContext.LocalVarInfo scannerVar = scannerResult.get(i);
                VariableInfo snapshotVar = snapshotResult.get(i);

                assertEquals(scannerVar.getName(), snapshotVar.getName(),
                        "第 " + i + " 个变量 name 应一致");
                assertEquals(scannerVar.getDescriptor(), snapshotVar.getDescriptor(),
                        "第 " + i + " 个变量 descriptor 应一致");
                assertEquals(scannerVar.getSlot(), snapshotVar.getSlot(),
                        "第 " + i + " 个变量 slot 应一致");
            }
        }

        @Test
        @DisplayName("snapshot 返回的 VariableInfo 包含正确的参数信息")
        void testSnapshotContainsParameters() {
            VariableSnapshotter snapshotter = new AsmVariableSnapshotter();

            List<VariableInfo> result = snapshotter.snapshot(
                    bytecode, "processWithPrimitives", "(BSIJFDCZ)V", firstLine);

            assertTrue(result.stream().anyMatch(v -> v.getName().equals("b")),
                    "参数 b 应可见");
            assertTrue(result.stream().anyMatch(v -> v.getName().equals("s")),
                    "参数 s 应可见");
            assertTrue(result.stream().anyMatch(v -> v.getName().equals("i")),
                    "参数 i 应可见");
        }

        @Test
        @DisplayName("snapshot 对不存在的方法返回空列表")
        void testSnapshotNonExistentMethod() {
            VariableSnapshotter snapshotter = new AsmVariableSnapshotter();

            List<VariableInfo> result = snapshotter.snapshot(
                    bytecode, "nonExistentMethod", "()V", 1);

            assertTrue(result.isEmpty());
        }

        @Test
        @DisplayName("snapshot 对无效行号返回空列表")
        void testSnapshotInvalidLine() {
            VariableSnapshotter snapshotter = new AsmVariableSnapshotter();

            List<VariableInfo> result = snapshotter.snapshot(
                    bytecode, "processWithPrimitives", "(BSIJFDCZ)V", 0);

            assertTrue(result.isEmpty());
        }
    }
}
