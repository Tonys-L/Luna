package fun.efto.luna.core.bytecode.asm.injector;

import fun.efto.luna.core.TestSetup;
import fun.efto.luna.core.injection.InjectionContext;
import fun.efto.luna.core.bytecode.asm.AsmInjectionContext;
import fun.efto.luna.core.bytecode.asm.ClassLoaderAwareClassWriter;
import fun.efto.luna.core.bytecode.asm.LocalVariableScanner;
import fun.efto.luna.core.bytecode.asm.assembler.ExpressionBytecodeAssembler;
import fun.efto.luna.core.injection.InjectionPoint;
import fun.efto.luna.core.injection.code.InjectableCode;
import fun.efto.luna.core.injection.code.CodeType;
import fun.efto.luna.core.injection.target.LineNumberTarget;
import fun.efto.luna.core.plugin.builtin.line.BeforeLineInjector;
import fun.efto.luna.core.plugin.builtin.line.LineNumberInjectionType;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.LineNumberNode;
import org.objectweb.asm.tree.MethodNode;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.lang.reflect.Constructor;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * @author ：Tony.L(286269159@qq.com)
 * @since  : 2026/05/10 18:00
 */
public class LineBeforeInjectionVerifyTest {

    @BeforeAll
    static void setUp() {
        TestSetup.init();
    }

    public static class TargetService {
        public void createUser(String name, int age) {
            long id = 1L;       // line 23
            int count = 0;      // line 24
            for (int i = 0; i < 10; i++) {  // line 25
                count += i;
            }
            System.out.println("User: " + name + ", Age: " + age + ", ID: " + id + ", Count: " + count);
        }
    }

    public static class BytecodeClassLoader extends ClassLoader {
        public Class<?> defineClass(String name, byte[] b) {
            return defineClass(name, b, 0, b.length);
        }
    }

    @Test
    public void testLineBeforeSnapshotInjection() throws Exception {
        String targetClassName = TargetService.class.getName();
        String targetInternalName = targetClassName.replace('.', '/');

        InputStream is = getClass().getClassLoader().getResourceAsStream(targetInternalName + ".class");
        assertNotNull(is, "Cannot find class file");
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        byte[] buffer = new byte[4096];
        int bytesRead;
        while ((bytesRead = is.read(buffer)) != -1) {
            baos.write(buffer, 0, bytesRead);
        }
        is.close();
        byte[] originalBytecode = baos.toByteArray();

        LineNumberTarget target = new LineNumberTarget(
                LineNumberInjectionType.BEFORE, targetClassName, 45, 0,
                "createUser", "(Ljava/lang/String;I)V");

        InjectableCode code = new InjectableCode() {
            @Override
            public String getCode() {
                return "snapshot:true";
            }
            @Override
            public CodeType getCodeType() {
                return CodeType.EXPRESSION;
            }
        };

        InjectionPoint injectionPoint = new InjectionPoint(target, code);
        InjectionContext context = new InjectionContext(injectionPoint);

        BeforeLineInjector injector = new BeforeLineInjector();
        ExpressionBytecodeAssembler assembler = new ExpressionBytecodeAssembler();

        byte[] transformedBytecode = injector.inject(context, originalBytecode, assembler);

        assertNotNull(transformedBytecode, "Transformed bytecode should not be null");
        assertTrue(transformedBytecode.length > 0, "Transformed bytecode should have content");

        BytecodeClassLoader classLoader = new BytecodeClassLoader();
        Class<?> transformedClass = classLoader.defineClass(targetClassName, transformedBytecode);

        Object instance = transformedClass.getDeclaredConstructor().newInstance();
        java.lang.reflect.Method method = transformedClass.getMethod("createUser", String.class, int.class);
        assertDoesNotThrow(() -> method.invoke(instance, "TestUser", 25),
                "Injected method should execute without VerifyError");
    }

    @Test
    public void testLineBeforeLogInjection() throws Exception {
        String targetClassName = TargetService.class.getName();
        String targetInternalName = targetClassName.replace('.', '/');

        InputStream is = getClass().getClassLoader().getResourceAsStream(targetInternalName + ".class");
        assertNotNull(is, "Cannot find class file");
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        byte[] buffer = new byte[4096];
        int bytesRead;
        while ((bytesRead = is.read(buffer)) != -1) {
            baos.write(buffer, 0, bytesRead);
        }
        is.close();
        byte[] originalBytecode = baos.toByteArray();

        LineNumberTarget target = new LineNumberTarget(
                LineNumberInjectionType.BEFORE, targetClassName, 45, 0,
                "createUser", "(Ljava/lang/String;I)V");

        InjectableCode code = new InjectableCode() {
            @Override
            public String getCode() {
                return "log:User created: $1, age: $2";
            }
            @Override
            public CodeType getCodeType() {
                return CodeType.EXPRESSION;
            }
        };

        InjectionPoint injectionPoint = new InjectionPoint(target, code);
        InjectionContext context = new InjectionContext(injectionPoint);

        BeforeLineInjector injector = new BeforeLineInjector();
        ExpressionBytecodeAssembler assembler = new ExpressionBytecodeAssembler();

        byte[] transformedBytecode = injector.inject(context, originalBytecode, assembler);

        assertNotNull(transformedBytecode, "Transformed bytecode should not be null");

        BytecodeClassLoader classLoader = new BytecodeClassLoader();
        Class<?> transformedClass = classLoader.defineClass(targetClassName, transformedBytecode);

        Object instance = transformedClass.getDeclaredConstructor().newInstance();
        java.lang.reflect.Method method = transformedClass.getMethod("createUser", String.class, int.class);
        assertDoesNotThrow(() -> method.invoke(instance, "TestUser", 25),
                "Injected method should execute without VerifyError");
    }

    @Test
    public void testLineBeforeConditionalLogInjection() throws Exception {
        String targetClassName = TargetService.class.getName();
        String targetInternalName = targetClassName.replace('.', '/');

        InputStream is = getClass().getClassLoader().getResourceAsStream(targetInternalName + ".class");
        assertNotNull(is, "Cannot find class file");
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        byte[] buffer = new byte[4096];
        int bytesRead;
        while ((bytesRead = is.read(buffer)) != -1) {
            baos.write(buffer, 0, bytesRead);
        }
        is.close();
        byte[] originalBytecode = baos.toByteArray();

        LineNumberTarget target = new LineNumberTarget(
                LineNumberInjectionType.BEFORE, targetClassName, 45, 0,
                "createUser", "(Ljava/lang/String;I)V");

        InjectableCode code = new InjectableCode() {
            @Override
            public String getCode() {
                return "${param[2] >= 18}::log:Adult user: $1, age: $2";
            }
            @Override
            public CodeType getCodeType() {
                return CodeType.EXPRESSION;
            }
        };

        InjectionPoint injectionPoint = new InjectionPoint(target, code);
        InjectionContext context = new InjectionContext(injectionPoint);

        BeforeLineInjector injector = new BeforeLineInjector();
        ExpressionBytecodeAssembler assembler = new ExpressionBytecodeAssembler();

        byte[] transformedBytecode = injector.inject(context, originalBytecode, assembler);

        assertNotNull(transformedBytecode, "Transformed bytecode should not be null");

        BytecodeClassLoader classLoader = new BytecodeClassLoader();
        Class<?> transformedClass = classLoader.defineClass(targetClassName, transformedBytecode);

        Object instance = transformedClass.getDeclaredConstructor().newInstance();
        java.lang.reflect.Method method = transformedClass.getMethod("createUser", String.class, int.class);
        assertDoesNotThrow(() -> method.invoke(instance, "TestUser", 25),
                "Injected method should execute without VerifyError");
    }

    @Test
    public void testDebugMaxLocals() throws Exception {
        String targetClassName = TargetService.class.getName();
        String targetInternalName = targetClassName.replace('.', '/');

        InputStream is = getClass().getClassLoader().getResourceAsStream(targetInternalName + ".class");
        assertNotNull(is);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        byte[] buffer = new byte[4096];
        int bytesRead;
        while ((bytesRead = is.read(buffer)) != -1) {
            baos.write(buffer, 0, bytesRead);
        }
        is.close();
        byte[] originalBytecode = baos.toByteArray();

        ClassNode cn = new ClassNode();
        new ClassReader(originalBytecode).accept(cn, 0);

        for (MethodNode mn : cn.methods) {
            if (mn.name.equals("createUser")) {
                System.out.println("createUser: maxLocals=" + mn.maxLocals + ", maxStack=" + mn.maxStack + ", desc=" + mn.desc);

                System.out.println("LineNumberNodes in createUser:");
                for (AbstractInsnNode insn = mn.instructions.getFirst(); insn != null; insn = insn.getNext()) {
                    if (insn instanceof LineNumberNode) {
                        LineNumberNode lnn = (LineNumberNode) insn;
                        System.out.println("  line " + lnn.line);
                    }
                }

                System.out.println("LocalVariables in createUser:");
                if (mn.localVariables != null) {
                    for (org.objectweb.asm.tree.LocalVariableNode lv : mn.localVariables) {
                        System.out.println("  " + lv.name + " desc=" + lv.desc + " index=" + lv.index + " start=" + lv.start + " end=" + lv.end);
                    }
                }
            }
        }
    }

    @Test
    public void testLineBeforeLogWithLocalVarNoDescriptor() throws Exception {
        String targetClassName = TargetService.class.getName();
        String targetInternalName = targetClassName.replace('.', '/');

        InputStream is = getClass().getClassLoader().getResourceAsStream(targetInternalName + ".class");
        assertNotNull(is, "Cannot find class file");
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        byte[] buffer = new byte[4096];
        int bytesRead;
        while ((bytesRead = is.read(buffer)) != -1) {
            baos.write(buffer, 0, bytesRead);
        }
        is.close();
        byte[] originalBytecode = baos.toByteArray();

        int countLine = -1;
        ClassNode cn = new ClassNode();
        new ClassReader(originalBytecode).accept(cn, 0);
        for (MethodNode mn : cn.methods) {
            if (mn.name.equals("createUser")) {
                int lineIdx = 0;
                for (AbstractInsnNode insn = mn.instructions.getFirst(); insn != null; insn = insn.getNext()) {
                    if (insn instanceof LineNumberNode) {
                        System.out.println("[DEBUG] lineIdx=" + lineIdx + " line=" + ((LineNumberNode) insn).line);
                        if (lineIdx == 1) {
                            countLine = ((LineNumberNode) insn).line;
                        }
                        lineIdx++;
                    }
                }
                System.out.println("[DEBUG] localVariables in createUser:");
                if (mn.localVariables != null) {
                    for (org.objectweb.asm.tree.LocalVariableNode lv : mn.localVariables) {
                        System.out.println("[DEBUG]   var=" + lv.name + " desc=" + lv.desc + " index=" + lv.index);
                    }
                }
            }
        }
        assertTrue(countLine > 0, "Should find the line number for 'int count = 0'");
        System.out.println("[DEBUG] Target line for injection: " + countLine);

        List<AsmInjectionContext.LocalVarInfo> safeVars = LocalVariableScanner.scanVisibleLocalVariables(
                originalBytecode, "createUser", "(Ljava/lang/String;I)V", countLine, true);
        List<AsmInjectionContext.LocalVarInfo> allVars = LocalVariableScanner.scanVisibleLocalVariables(
                originalBytecode, "createUser", "(Ljava/lang/String;I)V", countLine, false);
        System.out.println("[DEBUG] safeVars (excludeSameLineStart=true): " + safeVars.size());
        for (AsmInjectionContext.LocalVarInfo v : safeVars) {
            System.out.println("[DEBUG]   safe: " + v.getName() + " slot=" + v.getSlot() + " desc=" + v.getDescriptor());
        }
        System.out.println("[DEBUG] allVars (excludeSameLineStart=false): " + allVars.size());
        for (AsmInjectionContext.LocalVarInfo v : allVars) {
            System.out.println("[DEBUG]   all: " + v.getName() + " slot=" + v.getSlot() + " desc=" + v.getDescriptor());
        }

        LineNumberTarget target = new LineNumberTarget(
                LineNumberInjectionType.BEFORE, targetClassName, countLine, 0,
                "createUser", "");

        InjectableCode code = new InjectableCode() {
            @Override
            public String getCode() {
                return "log:check $id";
            }
            @Override
            public CodeType getCodeType() {
                return CodeType.EXPRESSION;
            }
        };

        InjectionPoint injectionPoint = new InjectionPoint(target, code);
        InjectionContext context = new InjectionContext(injectionPoint);

        BeforeLineInjector injector = new BeforeLineInjector();
        ExpressionBytecodeAssembler assembler = new ExpressionBytecodeAssembler();

        byte[] transformedBytecode = injector.inject(context, originalBytecode, assembler);

        assertNotNull(transformedBytecode, "Transformed bytecode should not be null");

        BytecodeClassLoader classLoader = new BytecodeClassLoader();
        Class<?> transformedClass = classLoader.defineClass(targetClassName, transformedBytecode);

        Object instance = transformedClass.getDeclaredConstructor().newInstance();
        java.lang.reflect.Method method = transformedClass.getMethod("createUser", String.class, int.class);

        while (fun.efto.luna.core.probe.ProbeOutput.BUFFER.poll() != null) {}

        assertDoesNotThrow(() -> method.invoke(instance, "TestUser", 25),
                "Injected method should execute without VerifyError");

        fun.efto.luna.core.probe.ProbeMessage logOutput = null;
        for (int i = 0; i < 100; i++) {
            logOutput = fun.efto.luna.core.probe.ProbeOutput.BUFFER.poll();
            if (logOutput != null && logOutput.getPayload().contains("check")) break;
            Thread.sleep(10);
        }

        assertNotNull(logOutput, "Should have log output containing 'check'");
        String payload = logOutput.getPayload();
        System.out.println("[TEST] Log output: " + payload);
        assertFalse(payload.contains("$id"), "Variable $id should be resolved, not appear as literal text. Got: " + payload);
    }
}
