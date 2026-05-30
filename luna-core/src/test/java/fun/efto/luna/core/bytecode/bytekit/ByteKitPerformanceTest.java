/**
 * @author : Tony.L(<286269159@qq.com>)
 * @since  : 2026/06/01 02:00
 */
package fun.efto.luna.core.bytecode.bytekit;

import fun.efto.luna.core.TestSetup;
import fun.efto.luna.core.bytecode.asm.assembler.BytecodeAssemblerRegistry;
import fun.efto.luna.core.bytecode.asm.injector.BytecodeInjector;
import fun.efto.luna.core.bytecode.asm.injector.BytecodeInjectorRegistry;
import fun.efto.luna.core.bytecode.bytekit.adapter.ByteKitEnterInjector;
import fun.efto.luna.core.bytecode.BytecodeAssembler;
import fun.efto.luna.core.injection.InjectionContext;
import fun.efto.luna.core.injection.InjectionPoint;
import fun.efto.luna.core.injection.code.InjectableCode;
import fun.efto.luna.core.injection.code.type.CodeType;
import fun.efto.luna.core.injection.target.MethodTarget;
import fun.efto.luna.core.plugin.builtin.method.MethodInjectionType;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;

import static org.junit.jupiter.api.Assertions.*;

public class ByteKitPerformanceTest {

    private static final long THRESHOLD_NS = 100_000;
    private static final int WARMUP_ITERATIONS = 100;
    private static final int MEASURE_ITERATIONS = 1000;

    public static class SimpleTarget {
        public String simpleMethod() {
            return "hello";
        }
    }

    private static byte[] targetBytecode;
    private static BytecodeInjector injector;
    private static InjectionContext injectionContext;
    private static BytecodeAssembler assembler;

    @BeforeAll
    static void setUp() throws Exception {
        TestSetup.init();

        targetBytecode = getClassBytecode(SimpleTarget.class);

        injector = BytecodeInjectorRegistry.getInstance().get(MethodInjectionType.ENTER).orElse(null);
        assertNotNull(injector, "ByteKit ENTER injector should be registered");
        assertInstanceOf(ByteKitEnterInjector.class, injector);

        MethodTarget target = new MethodTarget(MethodInjectionType.ENTER, SimpleTarget.class.getName(), "simpleMethod", "()Ljava/lang/String;");
        InjectableCode code = new InjectableCode() {
            @Override
            public String getCode() {
                return "log:test";
            }
            @Override
            public CodeType getCodeType() {
                return CodeType.EXPRESSION;
            }
        };
        InjectionPoint point = new InjectionPoint(target, code);
        injectionContext = new InjectionContext(point);

        assembler = BytecodeAssemblerRegistry.getInstance().get(CodeType.EXPRESSION).orElse(null);
        assertNotNull(assembler, "EXPRESSION assembler should be registered");
    }

    @Test
    @DisplayName("ByteKit 注入判断平均耗时 < 0.1ms")
    void testByteKitInjectionPerformance() {
        for (int i = 0; i < WARMUP_ITERATIONS; i++) {
            injector.inject(injectionContext, targetBytecode, assembler);
        }

        long totalTime = 0;
        for (int i = 0; i < MEASURE_ITERATIONS; i++) {
            long startTime = System.nanoTime();
            injector.inject(injectionContext, targetBytecode, assembler);
            totalTime += System.nanoTime() - startTime;
        }

        long averageTime = totalTime / MEASURE_ITERATIONS;
        System.out.println("ByteKit 注入判断平均时间: " + averageTime + " ns");
        assertTrue(averageTime < THRESHOLD_NS,
                "ByteKit 注入判断时间超过 0.1ms，实际: " + averageTime + " ns");
    }

    private static byte[] getClassBytecode(Class<?> clazz) throws Exception {
        String className = clazz.getName().replace('.', '/') + ".class";
        try (InputStream is = clazz.getClassLoader().getResourceAsStream(className)) {
            assertNotNull(is, "Cannot read bytecode for: " + className);
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            byte[] buffer = new byte[4096];
            int n;
            while ((n = is.read(buffer)) != -1) {
                bos.write(buffer, 0, n);
            }
            return bos.toByteArray();
        }
    }
}
