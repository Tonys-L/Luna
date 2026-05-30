package fun.efto.luna.core.plugin.builtin.method;

import fun.efto.luna.core.injection.InjectionContext;
import fun.efto.luna.core.asm.AsmInjectionContext;
import fun.efto.luna.core.asm.ClassLoaderAwareClassWriter;
import fun.efto.luna.core.asm.Constants;
import fun.efto.luna.core.bytecode.BytecodeAssembler;
import fun.efto.luna.core.injection.target.MethodTarget;
import fun.efto.luna.core.asm.injector.BytecodeInjector;
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
        AsmInjectionContext asmContext = new AsmInjectionContext(injectionContext, bytecode);

        ClassReader cr = new ClassReader(bytecode);
        ClassLoader loader = Thread.currentThread().getContextClassLoader();
        ClassWriter cw = new ClassLoaderAwareClassWriter(cr, ClassWriter.COMPUTE_MAXS | ClassWriter.COMPUTE_FRAMES, loader);
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

        cr.accept(cv, ClassReader.EXPAND_FRAMES);

        return cw.toByteArray();
    }

    private boolean shouldInjectIntoMethod(String name, String descriptor, AsmInjectionContext asmContext) {
        if (asmContext.getInjectionTarget() instanceof MethodTarget) {
            MethodTarget methodTarget = (MethodTarget) asmContext.getInjectionTarget();
            boolean nameMatches = name.equals(methodTarget.getMethodName());
            if (!nameMatches) {
                return false;
            }
            String targetDesc = methodTarget.getMethodDescriptor();
            if (targetDesc == null || targetDesc.isEmpty()) {
                return true;
            }
            return descriptor.equals(targetDesc);
        }
        return false;
    }

    protected abstract MethodVisitor createMethodVisitor(AsmInjectionContext asmContext, BytecodeAssembler bytecodeAssembler);
}