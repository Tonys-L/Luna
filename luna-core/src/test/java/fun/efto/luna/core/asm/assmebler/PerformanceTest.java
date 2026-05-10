package fun.efto.luna.core.asm.assmebler;

import fun.efto.luna.core.InjectionContext;
import fun.efto.luna.core.expression.ConditionRegistry;
import fun.efto.luna.core.injection.InjectionPoint;
import fun.efto.luna.core.injection.code.InjectableCode;
import fun.efto.luna.core.injection.code.type.CodeType;
import fun.efto.luna.core.injection.target.MethodTarget;
import fun.efto.luna.core.injection.target.type.MethodInjectionType;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Opcodes;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.PrintStream;
import java.lang.reflect.Method;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 性能测试：验证带有条件判断的插桩对单次调用的损耗
 *
 * @author : Tony.L(<286269159@qq.com>)
 * @since  : 2026/05/10 00:00
 */
public class PerformanceTest {

    private PrintStream originalOut;
    private ByteArrayOutputStream outContent;

    @BeforeEach
    public void setUpStreams() {
        originalOut = System.out;
        outContent = new ByteArrayOutputStream();
        System.setOut(new PrintStream(outContent)); // 禁用真实输出以防刷屏
    }

    @AfterEach
    public void restoreStreams() {
        System.setOut(originalOut);
    }

    public static class PerfTarget {
        public void process(String data, int iterations) {
            // empty method
        }
    }

    public static class BytecodeClassLoader extends ClassLoader {
        public Class<?> defineClass(String name, byte[] b) {
            return defineClass(name, b, 0, b.length);
        }
    }

    @Test
    public void testConditionEvaluationPerformance() throws Exception {
        String targetClassName = PerfTarget.class.getName();
        String targetInternalName = targetClassName.replace('.', '/');

        InputStream is = getClass().getClassLoader().getResourceAsStream(targetInternalName + ".class");
        byte[] originalBytecode = new byte[is.available()];
        is.read(originalBytecode);
        is.close();

        // 构造注入点，包含条件 ${param[2] > 1000}，不满足时不打印
        MethodTarget target = new MethodTarget(MethodInjectionType.ENTER, targetClassName, "process", "(Ljava/lang/String;I)V");
        InjectableCode code = new InjectableCode() {
            @Override
            public String getCode() {
                return "${param[2] > 1000}::log:Data processed: $1";
            }
            @Override
            public CodeType getCodeType() {
                return CodeType.JAVA;
            }
        };
        InjectionPoint point = new InjectionPoint(target, code) {
            @Override
            public fun.efto.luna.core.injection.target.type.InjectionType getInjectionType() {
                return MethodInjectionType.ENTER;
            }
        };
        
        String injectionId = point.getId();

        InjectionContext context = new InjectionContext(point);
        ExpressionBytecodeAssembler assembler = new ExpressionBytecodeAssembler();

        ClassReader cr = new ClassReader(originalBytecode);
        ClassWriter cw = new ClassWriter(ClassWriter.COMPUTE_FRAMES | ClassWriter.COMPUTE_MAXS);
        org.objectweb.asm.ClassVisitor cv = new org.objectweb.asm.ClassVisitor(Opcodes.ASM9, cw) {
            @Override
            public org.objectweb.asm.MethodVisitor visitMethod(int access, String name, String descriptor, String signature, String[] exceptions) {
                org.objectweb.asm.MethodVisitor mv = super.visitMethod(access, name, descriptor, signature, exceptions);
                if ("process".equals(name)) {
                    return new org.objectweb.asm.MethodVisitor(Opcodes.ASM9, mv) {
                        @Override
                        public void visitCode() {
                            super.visitCode();
                            fun.efto.luna.core.asm.AsmInjectionContext asmCtx = new fun.efto.luna.core.asm.AsmInjectionContext(context, originalBytecode);
                            asmCtx.setMethodVisitor(mv);
                            asmCtx.setMethodAccess(access);
                            assembler.assemble(asmCtx, null);
                        }
                    };
                }
                return mv;
            }
        };
        cr.accept(cv, ClassReader.EXPAND_FRAMES);
        byte[] transformedBytecode = cw.toByteArray();

        BytecodeClassLoader classLoader = new BytecodeClassLoader();
        Class<?> transformedClass = classLoader.defineClass(targetClassName, transformedBytecode);

        Object instance = transformedClass.getDeclaredConstructor().newInstance();
        Method method = transformedClass.getMethod("process", String.class, int.class);

        // 预热 (JVM JIT 编译)
        for (int i = 0; i < 10000; i++) {
            method.invoke(instance, "Warmup", 10);
        }

        int iterations = 100000;
        long startTime = System.nanoTime();

        for (int i = 0; i < iterations; i++) {
            method.invoke(instance, "PerfTest", 10); // 条件不满足
        }

        long endTime = System.nanoTime();
        double avgTimeMs = (endTime - startTime) / 1_000_000.0 / iterations;

        System.setOut(originalOut);
        System.out.println("性能测试完成: " + iterations + " 次调用");
        System.out.println("平均单次条件插桩判定耗时: " + avgTimeMs + " ms");

        // 校验红线: 单次插桩判定耗时必须 < 0.1ms
        assertTrue(avgTimeMs < 0.1, "单次插桩判定耗时过高: " + avgTimeMs + " ms > 0.1 ms");
        
        ConditionRegistry.unregister(injectionId);
    }
}
