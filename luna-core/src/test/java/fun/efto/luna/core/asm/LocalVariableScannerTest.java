package fun.efto.luna.core.asm;

import fun.efto.luna.core.asm.AsmInjectionContext.LocalVarInfo;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.LineNumberNode;
import org.objectweb.asm.tree.MethodNode;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * @author : Tony.L(<286269159@qq.com>)
 * @since  : 2026/05/10 20:30
 */
@DisplayName("LocalVariableScanner 测试")
public class LocalVariableScannerTest {

    public static class ServiceWithLocals {
        public void process(String name, int age) {
            long id = 1L;
            int count = 0;
            for (int i = 0; i < 10; i++) {
                count += i;
            }
            System.out.println(name);
        }
    }

    public static class ServiceWithForLoop {
        public void createUser(String name, int age) {
            long id = 1L;
            int count = 0;
            for (int i = 0; i < 100; i++) {
                int a = 0;
                int b = 0;
                if (i % 2 == 0) {
                    int c = a + b;
                    count++;
                } else {
                    int d = a - b;
                }
            }
            System.out.println(name);
        }
    }

    private byte[] getClassBytecode(Class<?> clazz) throws Exception {
        String internalName = clazz.getName().replace('.', '/');
        InputStream is = getClass().getClassLoader().getResourceAsStream(internalName + ".class");
        assertNotNull(is);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        byte[] buffer = new byte[4096];
        int bytesRead;
        while ((bytesRead = is.read(buffer)) != -1) {
            baos.write(buffer, 0, bytesRead);
        }
        is.close();
        return baos.toByteArray();
    }

    private int findLineNumber(byte[] bytecode, String methodName, int offset) {
        ClassNode cn = new ClassNode();
        new ClassReader(bytecode).accept(cn, 0);
        for (MethodNode mn : cn.methods) {
            if (mn.name.equals(methodName)) {
                int count = 0;
                for (org.objectweb.asm.tree.AbstractInsnNode insn = mn.instructions.getFirst(); insn != null; insn = insn.getNext()) {
                    if (insn instanceof LineNumberNode) {
                        if (count == offset) {
                            return ((LineNumberNode) insn).line;
                        }
                        count++;
                    }
                }
            }
        }
        return -1;
    }

    @Nested
    @DisplayName("变量可见性判断")
    class VisibilityTests {

        @Test
        @DisplayName("方法参数始终可见")
        void testParamsAlwaysVisible() throws Exception {
            byte[] bytecode = getClassBytecode(ServiceWithLocals.class);
            int line = findLineNumber(bytecode, "process", 0);

            List<LocalVarInfo> vars = LocalVariableScanner.scanVisibleLocalVariables(
                    bytecode, "process", "(Ljava/lang/String;I)V", line);

            assertTrue(vars.stream().anyMatch(v -> v.getName().equals("name")),
                    "Parameter 'name' should be visible");
            assertTrue(vars.stream().anyMatch(v -> v.getName().equals("age")),
                    "Parameter 'age' should be visible");
        }

        @Test
        @DisplayName("excludeSameLineStart=true 排除同行声明的变量")
        void testExcludeSameLineStart() throws Exception {
            byte[] bytecode = getClassBytecode(ServiceWithLocals.class);
            int idLine = findLineNumber(bytecode, "process", 0);

            List<LocalVarInfo> varsExclude = LocalVariableScanner.scanVisibleLocalVariables(
                    bytecode, "process", "(Ljava/lang/String;I)V", idLine, true);

            assertFalse(varsExclude.stream().anyMatch(v -> v.getName().equals("id")),
                    "Variable 'id' declared on same line should be excluded when excludeSameLineStart=true");

            List<LocalVarInfo> varsInclude = LocalVariableScanner.scanVisibleLocalVariables(
                    bytecode, "process", "(Ljava/lang/String;I)V", idLine, false);

            assertTrue(varsInclude.stream().anyMatch(v -> v.getName().equals("id")),
                    "Variable 'id' should be included when excludeSameLineStart=false");
        }

        @Test
        @DisplayName("for 循环变量在循环行之前不可见")
        void testLoopVarNotVisibleBeforeLoop() throws Exception {
            byte[] bytecode = getClassBytecode(ServiceWithLocals.class);
            int firstLine = findLineNumber(bytecode, "process", 0);

            List<LocalVarInfo> vars = LocalVariableScanner.scanVisibleLocalVariables(
                    bytecode, "process", "(Ljava/lang/String;I)V", firstLine, true);

            assertFalse(vars.stream().anyMatch(v -> v.getName().equals("i")),
                    "Loop variable 'i' should not be visible before the loop line");
        }

        @Test
        @DisplayName("long 类型变量占用两个 slot")
        void testLongVariableSlotSize() throws Exception {
            byte[] bytecode = getClassBytecode(ServiceWithLocals.class);
            int idLine = findLineNumber(bytecode, "process", 0);

            List<LocalVarInfo> vars = LocalVariableScanner.scanVisibleLocalVariables(
                    bytecode, "process", "(Ljava/lang/String;I)V", idLine, false);

            LocalVarInfo idVar = vars.stream().filter(v -> v.getName().equals("id")).findFirst().orElse(null);
            assertNotNull(idVar, "Variable 'id' should be visible");
            assertEquals("J", idVar.getDescriptor(), "id should be long type (descriptor J)");
        }

        @Test
        @DisplayName("无效参数返回空列表")
        void testInvalidParams() {
            List<LocalVarInfo> result1 = LocalVariableScanner.scanVisibleLocalVariables(null, "test", "()V", 1);
            assertTrue(result1.isEmpty());

            List<LocalVarInfo> result2 = LocalVariableScanner.scanVisibleLocalVariables(new byte[0], null, "()V", 1);
            assertTrue(result2.isEmpty());

            List<LocalVarInfo> result3 = LocalVariableScanner.scanVisibleLocalVariables(new byte[0], "test", "()V", 0);
            assertTrue(result3.isEmpty());
        }

        @Test
        @DisplayName("不存在的方法返回空列表")
        void testNonExistentMethod() throws Exception {
            byte[] bytecode = getClassBytecode(ServiceWithLocals.class);

            List<LocalVarInfo> result = LocalVariableScanner.scanVisibleLocalVariables(
                    bytecode, "nonExistent", "()V", 1);

            assertTrue(result.isEmpty(), "Non-existent method should return empty list");
        }

        @Test
        @DisplayName("for 循环变量 i 在 count=0 行不可见（与 UserService 第21行场景一致）")
        void testLoopVarNotVisibleAtCountLine() throws Exception {
            byte[] bytecode = getClassBytecode(ServiceWithForLoop.class);

            int countLineNum = findLineNumber(bytecode, "createUser", 1);

            List<LocalVarInfo> vars = LocalVariableScanner.scanVisibleLocalVariables(
                    bytecode, "createUser", "(Ljava/lang/String;I)V", countLineNum, true);

            assertFalse(vars.stream().anyMatch(v -> v.getName().equals("i")),
                    "Loop variable 'i' should NOT be visible at 'int count = 0' line (before the for loop)");
            assertFalse(vars.stream().anyMatch(v -> v.getName().equals("a")),
                    "Variable 'a' inside the loop should NOT be visible at 'int count = 0' line");
            assertFalse(vars.stream().anyMatch(v -> v.getName().equals("count")),
                    "Variable 'count' declared on same line should be excluded when excludeSameLineStart=true");

            List<LocalVarInfo> varsInclude = LocalVariableScanner.scanVisibleLocalVariables(
                    bytecode, "createUser", "(Ljava/lang/String;I)V", countLineNum, false);
            assertTrue(varsInclude.stream().anyMatch(v -> v.getName().equals("count")),
                    "Variable 'count' should be visible when excludeSameLineStart=false");
            assertFalse(varsInclude.stream().anyMatch(v -> v.getName().equals("i")),
                    "Loop variable 'i' should NOT be visible even when excludeSameLineStart=false");
        }

        @Test
        @DisplayName("UserService 场景：for 循环变量 i 在 count=0 行不可见，在 for 行可见")
        void testUserServiceLoopVarIssue() throws Exception {
            byte[] bytecode = getClassBytecode(ServiceWithForLoop.class);

            int countLineNum = findLineNumber(bytecode, "createUser", 1);
            int forLoopLineNum = findLineNumber(bytecode, "createUser", 2);

            assertTrue(countLineNum < forLoopLineNum,
                    "count line (" + countLineNum + ") should be before for loop line (" + forLoopLineNum + ")");

            List<LocalVarInfo> varsAtCountLine = LocalVariableScanner.scanVisibleLocalVariables(
                    bytecode, "createUser", "(Ljava/lang/String;I)V", countLineNum, false);

            assertFalse(varsAtCountLine.stream().anyMatch(v -> v.getName().equals("i")),
                    "Loop variable 'i' should NOT be visible at 'int count = 0' line");

            List<LocalVarInfo> varsAtForLine = LocalVariableScanner.scanVisibleLocalVariables(
                    bytecode, "createUser", "(Ljava/lang/String;I)V", forLoopLineNum, false);

            assertTrue(varsAtForLine.stream().anyMatch(v -> v.getName().equals("i")),
                    "Loop variable 'i' SHOULD be visible at the for loop line");
        }
    }
}
