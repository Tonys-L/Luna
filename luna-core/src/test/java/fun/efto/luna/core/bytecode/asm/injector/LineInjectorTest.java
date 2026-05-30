package fun.efto.luna.core.bytecode.asm.injector;

import fun.efto.luna.core.injection.InjectionContext;
import fun.efto.luna.core.TestSetup;
import fun.efto.luna.core.bytecode.asm.assembler.ExpressionBytecodeAssembler;
import fun.efto.luna.core.injection.InjectionPoint;
import fun.efto.luna.core.injection.code.InjectableCode;
import fun.efto.luna.core.injection.code.type.CodeType;
import fun.efto.luna.core.injection.target.LineNumberTarget;
import fun.efto.luna.core.plugin.builtin.line.AfterLineInjector;
import fun.efto.luna.core.plugin.builtin.line.BeforeLineInjector;
import fun.efto.luna.core.plugin.builtin.line.LineNumberInjectionType;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodNode;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * @author : Tony.L(<286269159@qq.com>)
 * @since  : 2026/05/10 20:00
 */
@DisplayName("行号级注入器测试")
public class LineInjectorTest {

    @BeforeAll
    static void setUp() {
        TestSetup.init();
    }

    public static class TargetService {
        public void createUser(String name, int age) {
            long id = 1L;
            int count = 0;
            for (int i = 0; i < 10; i++) {
                count += i;
            }
            System.out.println("User: " + name + ", Age: " + age + ", ID: " + id + ", Count: " + count);
        }

        public int calculate(int a, int b) {
            int sum = a + b;
            return sum;
        }

        public String greet(String name) {
            return "Hello, " + name;
        }

        public void emptyMethod() {
        }

        public void multiReturn(int x) {
            if (x > 0) {
                System.out.println("positive");
                return;
            }
            System.out.println("non-positive");
        }
    }

    static class BytecodeClassLoader extends ClassLoader {
        public Class<?> defineClass(String name, byte[] b) {
            return defineClass(name, b, 0, b.length);
        }
    }

    private byte[] getClassBytecode(Class<?> clazz) throws Exception {
        String internalName = clazz.getName().replace('.', '/');
        InputStream is = getClass().getClassLoader().getResourceAsStream(internalName + ".class");
        assertNotNull(is, "Cannot find class file for " + clazz.getName());
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        byte[] buffer = new byte[4096];
        int bytesRead;
        while ((bytesRead = is.read(buffer)) != -1) {
            baos.write(buffer, 0, bytesRead);
        }
        is.close();
        return baos.toByteArray();
    }

    private int findLineNumber(byte[] bytecode, String methodName, String methodDesc) {
        ClassNode cn = new ClassNode();
        new ClassReader(bytecode).accept(cn, 0);
        for (MethodNode mn : cn.methods) {
            if (mn.name.equals(methodName) && (methodDesc == null || mn.desc.equals(methodDesc))) {
                return mn.maxLocals;
            }
        }
        return -1;
    }

    @Nested
    @DisplayName("BeforeLineInjector 测试")
    class BeforeLineTests {

        @Test
        @DisplayName("line_before log 注入 - 无 VerifyError")
        void testLogInjection() throws Exception {
            byte[] bytecode = getClassBytecode(TargetService.class);
            int line = findFirstMethodLine(bytecode, "createUser");

            LineNumberTarget target = new LineNumberTarget(
                    LineNumberInjectionType.BEFORE, TargetService.class.getName(), line, 0,
                    "createUser", "(Ljava/lang/String;I)V");

            InjectableCode code = createCode("log:User created: $1, age: $2");
            InjectionPoint ip = new InjectionPoint(target, code);
            InjectionContext ctx = new InjectionContext(ip);

            BeforeLineInjector injector = new BeforeLineInjector();
            byte[] result = injector.inject(ctx, bytecode, new ExpressionBytecodeAssembler());

            assertValidBytecode(result, TargetService.class.getName());
        }

        @Test
        @DisplayName("line_before snapshot 注入 - 无 VerifyError")
        void testSnapshotInjection() throws Exception {
            byte[] bytecode = getClassBytecode(TargetService.class);
            int line = findFirstMethodLine(bytecode, "createUser");

            LineNumberTarget target = new LineNumberTarget(
                    LineNumberInjectionType.BEFORE, TargetService.class.getName(), line, 0,
                    "createUser", "(Ljava/lang/String;I)V");

            InjectableCode code = createCode("snapshot:true");
            InjectionPoint ip = new InjectionPoint(target, code);
            InjectionContext ctx = new InjectionContext(ip);

            BeforeLineInjector injector = new BeforeLineInjector();
            byte[] result = injector.inject(ctx, bytecode, new ExpressionBytecodeAssembler());

            assertValidBytecode(result, TargetService.class.getName());
        }

        @Test
        @DisplayName("line_before 条件注入 - 无 VerifyError")
        void testConditionalInjection() throws Exception {
            byte[] bytecode = getClassBytecode(TargetService.class);
            int line = findFirstMethodLine(bytecode, "createUser");

            LineNumberTarget target = new LineNumberTarget(
                    LineNumberInjectionType.BEFORE, TargetService.class.getName(), line, 0,
                    "createUser", "(Ljava/lang/String;I)V");

            InjectableCode code = createCode("${param[2] >= 18}::log:Adult user: $1");
            InjectionPoint ip = new InjectionPoint(target, code);
            InjectionContext ctx = new InjectionContext(ip);

            BeforeLineInjector injector = new BeforeLineInjector();
            byte[] result = injector.inject(ctx, bytecode, new ExpressionBytecodeAssembler());

            assertValidBytecode(result, TargetService.class.getName());
        }

        @Test
        @DisplayName("line_before 简单方法注入 - 无 VerifyError")
        void testSimpleMethodInjection() throws Exception {
            byte[] bytecode = getClassBytecode(TargetService.class);
            int line = findFirstMethodLine(bytecode, "calculate");

            LineNumberTarget target = new LineNumberTarget(
                    LineNumberInjectionType.BEFORE, TargetService.class.getName(), line, 0,
                    "calculate", "(II)I");

            InjectableCode code = createCode("log:Calculating: $1 + $2");
            InjectionPoint ip = new InjectionPoint(target, code);
            InjectionContext ctx = new InjectionContext(ip);

            BeforeLineInjector injector = new BeforeLineInjector();
            byte[] result = injector.inject(ctx, bytecode, new ExpressionBytecodeAssembler());

            assertValidBytecode(result, TargetService.class.getName());
        }

        @Test
        @DisplayName("line_before 无参方法注入 - 无 VerifyError")
        void testNoArgMethodInjection() throws Exception {
            byte[] bytecode = getClassBytecode(TargetService.class);
            int line = findFirstMethodLine(bytecode, "emptyMethod");

            LineNumberTarget target = new LineNumberTarget(
                    LineNumberInjectionType.BEFORE, TargetService.class.getName(), line, 0,
                    "emptyMethod", "()V");

            InjectableCode code = createCode("log:Empty method called");
            InjectionPoint ip = new InjectionPoint(target, code);
            InjectionContext ctx = new InjectionContext(ip);

            BeforeLineInjector injector = new BeforeLineInjector();
            byte[] result = injector.inject(ctx, bytecode, new ExpressionBytecodeAssembler());

            assertValidBytecode(result, TargetService.class.getName());
        }
    }

    @Nested
    @DisplayName("AfterLineInjector 测试")
    class AfterLineTests {

        @Test
        @DisplayName("line_after log 注入 - 无 VerifyError")
        void testLogInjection() throws Exception {
            byte[] bytecode = getClassBytecode(TargetService.class);
            int line = findFirstMethodLine(bytecode, "createUser");

            LineNumberTarget target = new LineNumberTarget(
                    LineNumberInjectionType.AFTER, TargetService.class.getName(), line, 0,
                    "createUser", "(Ljava/lang/String;I)V");

            InjectableCode code = createCode("log:After line execution");
            InjectionPoint ip = new InjectionPoint(target, code);
            InjectionContext ctx = new InjectionContext(ip);

            AfterLineInjector injector = new AfterLineInjector();
            byte[] result = injector.inject(ctx, bytecode, new ExpressionBytecodeAssembler());

            assertValidBytecode(result, TargetService.class.getName());
        }

        @Test
        @DisplayName("line_after snapshot 注入 - 无 VerifyError")
        void testSnapshotInjection() throws Exception {
            byte[] bytecode = getClassBytecode(TargetService.class);
            int line = findFirstMethodLine(bytecode, "calculate");

            LineNumberTarget target = new LineNumberTarget(
                    LineNumberInjectionType.AFTER, TargetService.class.getName(), line, 0,
                    "calculate", "(II)I");

            InjectableCode code = createCode("snapshot:true");
            InjectionPoint ip = new InjectionPoint(target, code);
            InjectionContext ctx = new InjectionContext(ip);

            AfterLineInjector injector = new AfterLineInjector();
            byte[] result = injector.inject(ctx, bytecode, new ExpressionBytecodeAssembler());

            assertValidBytecode(result, TargetService.class.getName());
        }
    }

    private InjectableCode createCode(String codeStr) {
        return new InjectableCode() {
            @Override
            public String getCode() { return codeStr; }
            @Override
            public CodeType getCodeType() { return CodeType.EXPRESSION; }
        };
    }

    private int findFirstMethodLine(byte[] bytecode, String methodName) {
        ClassNode cn = new ClassNode();
        new ClassReader(bytecode).accept(cn, 0);
        for (MethodNode mn : cn.methods) {
            if (mn.name.equals(methodName)) {
                org.objectweb.asm.tree.AbstractInsnNode insn = mn.instructions.getFirst();
                while (insn != null) {
                    if (insn instanceof org.objectweb.asm.tree.LineNumberNode) {
                        return ((org.objectweb.asm.tree.LineNumberNode) insn).line;
                    }
                    insn = insn.getNext();
                }
            }
        }
        return -1;
    }

    private void assertValidBytecode(byte[] bytecode, String className) throws Exception {
        assertNotNull(bytecode, "Bytecode should not be null");
        assertTrue(bytecode.length > 0, "Bytecode should have content");
        BytecodeClassLoader cl = new BytecodeClassLoader();
        Class<?> loaded = cl.defineClass(className, bytecode);
        assertNotNull(loaded, "Class should be loadable");
    }
}
