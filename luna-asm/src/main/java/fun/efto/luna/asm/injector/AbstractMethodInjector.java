package fun.efto.luna.asm.injector;

import fun.efto.luna.asm.AsmInjectionContext;
import fun.efto.luna.asm.Constants;
import fun.efto.luna.core.InjectionContext;
import fun.efto.luna.core.bytecode.BytecodeAssembler;
import fun.efto.luna.core.injection.target.MethodTarget;
import fun.efto.luna.core.injector.BytecodeInjector;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.MethodVisitor;

/**
 * @author ：Tony.L(286269159@qq.com)
 * @since ：2025/10/2 20:46
 */
public abstract class AbstractMethodInjector implements BytecodeInjector {

    protected AbstractMethodInjector() {

    }

    @Override
    public byte[] inject(InjectionContext injectionContext, byte[] bytecode, BytecodeAssembler bytecodeAssembler) {
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
                if (shouldInjectIntoMethod(name, descriptor, asmContext)) {
                    return createMethodVisitor(asmContext, bytecodeAssembler);
                }
                return mv;
            }
        };

        cr.accept(cv, 0);

        return cw.toByteArray();
    }

    private boolean shouldInjectIntoMethod(String name, String descriptor, AsmInjectionContext asmContext) {
        if (asmContext.getInjectionTarget() instanceof MethodTarget) {
            MethodTarget methodTarget = (MethodTarget) asmContext.getInjectionTarget();
            return name.equals(methodTarget.getMethodName()) && descriptor.equals(methodTarget.getMethodDescriptor());
        }
        return false;
    }

    protected abstract MethodVisitor createMethodVisitor(AsmInjectionContext asmContext, BytecodeAssembler bytecodeAssembler);
}