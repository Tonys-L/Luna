package fun.efto.luna.core.asm.assmebler;

import fun.efto.luna.core.InjectionContext;
import fun.efto.luna.core.asm.injector.EnterMethodInjector;
import fun.efto.luna.core.expression.ConditionRegistry;
import fun.efto.luna.core.expression.context.EvaluationContext;
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
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodNode;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.PrintStream;
import java.lang.reflect.Method;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 端到端条件注入测试（S1.1.6）
 * 测试：解析条件 -> 注入条件判定字节码 -> 运行时求值判定 -> IFEQ 跳过日志
 *
 * @author : Tony.L(<286269159@qq.com>)
 * @since  : 2026/05/10 00:00
 */
public class ConditionalInjectionEndToEndTest {

    private PrintStream originalOut;
    private ByteArrayOutputStream outContent;

    @BeforeEach
    public void setUpStreams() {
        originalOut = System.out;
        outContent = new ByteArrayOutputStream();
        System.setOut(new PrintStream(outContent));
    }

    @AfterEach
    public void restoreStreams() {
        System.setOut(originalOut);
    }

    public static class TargetObject {
        public void processUser(String name, int age) {
            // target method
        }
    }

    // 自定义类加载器用于加载注入后的字节码
    public static class BytecodeClassLoader extends ClassLoader {
        public Class<?> defineClass(String name, byte[] b) {
            return defineClass(name, b, 0, b.length);
        }
    }

    @Test
    public void testConditionalInjection() throws Exception {
        String targetClassName = TargetObject.class.getName();
        String targetInternalName = targetClassName.replace('.', '/');

        // 1. 获取目标类的原始字节码
        InputStream is = getClass().getClassLoader().getResourceAsStream(targetInternalName + ".class");
        assertNotNull(is, "Cannot find class file");
        byte[] originalBytecode = new byte[is.available()];
        is.read(originalBytecode);
        is.close();

        // 2. 构造注入点：条件为 age >= 18
        MethodTarget target = new MethodTarget(MethodInjectionType.ENTER, targetClassName, "processUser", "(Ljava/lang/String;I)V");
        InjectableCode code = new InjectableCode() {
            @Override
            public String getCode() {
                // 条件前缀 + log指令
                return "${param[2] >= 18}::log:Adult user: $1, age: $2";
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
        
        // 保存生成的 ID 用于注册条件
        String injectionId = point.getId();

        // 3. 执行注入转换
        InjectionContext context = new InjectionContext(point);
        EnterMethodInjector injector = new EnterMethodInjector();
        ExpressionBytecodeAssembler assembler = new ExpressionBytecodeAssembler();
        
        ClassReader cr = new ClassReader(originalBytecode);
        ClassWriter cw = new ClassWriter(ClassWriter.COMPUTE_FRAMES | ClassWriter.COMPUTE_MAXS);
        org.objectweb.asm.ClassVisitor cv = new org.objectweb.asm.ClassVisitor(Opcodes.ASM9, cw) {
            @Override
            public org.objectweb.asm.MethodVisitor visitMethod(int access, String name, String descriptor, String signature, String[] exceptions) {
                org.objectweb.asm.MethodVisitor mv = super.visitMethod(access, name, descriptor, signature, exceptions);
                if ("processUser".equals(name)) {
                    // 我们使用一个适配器在方法入口处插入字节码
                    return new org.objectweb.asm.MethodVisitor(Opcodes.ASM9, mv) {
                        @Override
                        public void visitCode() {
                            super.visitCode();
                            // 在这里调用 Assembler！
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

        // 4. 加载修改后的类
        BytecodeClassLoader classLoader = new BytecodeClassLoader();
        // 必须使用一个新的名字避免类冲突，但因为上面替换字节码没改类名，我们直接在类加载器里加载它
        Class<?> transformedClass = classLoader.defineClass(targetClassName, transformedBytecode);

        // 5. 反射执行
        Object instance = transformedClass.getDeclaredConstructor().newInstance();
        Method method = transformedClass.getMethod("processUser", String.class, int.class);

        // 测试 1：条件不满足 (age = 15)
        fun.efto.luna.core.spy.LunaSpy.LOG_BUFFER.poll(); // 清空旧数据
        method.invoke(instance, "Alice", 15);
        String output1 = fun.efto.luna.core.spy.LunaSpy.LOG_BUFFER.poll();
        assertNull(output1, "条件不满足时，不应输出日志");

        // 测试 2：条件满足 (age = 20)
        method.invoke(instance, "Bob", 20);
        String output2 = fun.efto.luna.core.spy.LunaSpy.LOG_BUFFER.poll();
        assertNotNull(output2, "条件满足时，应有日志输出");
        assertTrue(output2.contains("Adult user: Bob, age: 20"), "条件满足时，应输出包含变量的日志: " + output2);
        assertTrue(output2.contains("method enter"), "应该有日志前缀");
        
        ConditionRegistry.unregister(injectionId);
    }
}
