package fun.efto.luna.core.asm.injector;

import fun.efto.luna.core.InjectionContext;
import fun.efto.luna.core.asm.AsmInjectionContext;
import fun.efto.luna.core.asm.Constants;
import fun.efto.luna.core.asm.injector.visitor.LineNumberVisitor;
import fun.efto.luna.core.bytecode.BytecodeAssembler;
import fun.efto.luna.core.injection.target.LineNumberTarget;
import fun.efto.luna.core.injector.BytecodeInjector;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.MethodVisitor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 在特定行号前注入代码的注入器
 *
 * @author ：Tony.L(286269159@qq.com)
 * @since ：2025/10/4 18:00
 */
public class BeforeLineInjector implements BytecodeInjector {
    private static final Logger LOGGER = LoggerFactory.getLogger(BeforeLineInjector.class);

    @Override
    public byte[] inject(InjectionContext injectionContext, byte[] bytecode, BytecodeAssembler bytecodeAssembler) {
        LOGGER.info("在行前注入");

        // 创建ASM注入上下文
        AsmInjectionContext asmContext = new AsmInjectionContext(injectionContext, bytecode);

        ClassReader cr = new ClassReader(bytecode);
        ClassWriter cw = new ClassWriter(cr, ClassWriter.COMPUTE_MAXS | ClassWriter.COMPUTE_FRAMES);
        asmContext.setClassVisitor(cw);

        ClassVisitor cv = new ClassVisitor(Constants.AMS_API_VERSION, cw) {
            @Override
            public MethodVisitor visitMethod(int access, String name, String descriptor, String signature, String[] exceptions) {
                MethodVisitor mv = super.visitMethod(access, name, descriptor, signature, exceptions);
                asmContext.setMethodVisitor(mv);

                // 检查是否需要在该方法中注入
                if (shouldInjectIntoMethod(name, descriptor, asmContext)) {
                    // 创建行号访问器，在行前注入
                    return new LineNumberVisitor(Constants.AMS_API_VERSION, asmContext, bytecodeAssembler, true);
                }
                return mv;
            }
        };

        cr.accept(cv, 0);

        return cw.toByteArray();
    }

    private boolean shouldInjectIntoMethod(String name, String descriptor, AsmInjectionContext asmContext) {
        // 检查注入目标是否为行号目标
        if (asmContext.getInjectionTarget() instanceof LineNumberTarget) {
            LineNumberTarget lineNumberTarget = (LineNumberTarget) asmContext.getInjectionTarget();
            // 这里可以添加更多的方法匹配逻辑
            // 目前简化处理，假设目标方法已经通过其他方式确定
            return true;
        }
        return false;
    }
}