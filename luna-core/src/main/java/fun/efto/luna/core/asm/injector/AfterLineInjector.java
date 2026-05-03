package fun.efto.luna.core.asm.injector;

import fun.efto.luna.core.InjectionContext;
import fun.efto.luna.core.asm.AsmInjectionContext;
import fun.efto.luna.core.asm.ClassLoaderAwareClassWriter;
import fun.efto.luna.core.asm.Constants;
import fun.efto.luna.core.asm.injector.visitor.LineNumberVisitor;
import fun.efto.luna.core.bytecode.BytecodeAssembler;
import fun.efto.luna.core.injection.target.LineNumberTarget;
import fun.efto.luna.core.injector.BytecodeInjector;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.MethodVisitor;

import java.util.ArrayList;
import java.util.List;

/**
 * @author ：Tony.L(286269159@qq.com)
 * @since ：2025/10/4 18:00
 */
public class AfterLineInjector implements BytecodeInjector {

    @Override
    public byte[] inject(InjectionContext injectionContext, byte[] bytecode, BytecodeAssembler bytecodeAssembler) {
        AsmInjectionContext asmContext = new AsmInjectionContext(injectionContext, bytecode);

        ClassReader cr = new ClassReader(bytecode);
        ClassLoader loader = Thread.currentThread().getContextClassLoader();
        ClassWriter cw = new ClassLoaderAwareClassWriter(cr, ClassWriter.COMPUTE_MAXS | ClassWriter.COMPUTE_FRAMES, loader);
        asmContext.setClassVisitor(cw);

        final List<LineNumberVisitor> visitors = new ArrayList<>();

        ClassVisitor cv = new ClassVisitor(Constants.AMS_API_VERSION, cw) {
            @Override
            public MethodVisitor visitMethod(int access, String name, String descriptor, String signature, String[] exceptions) {
                MethodVisitor mv = super.visitMethod(access, name, descriptor, signature, exceptions);
                asmContext.setMethodVisitor(mv);

                if (shouldInjectIntoMethod(name, descriptor, asmContext)) {
                    LineNumberVisitor visitor = new LineNumberVisitor(Constants.AMS_API_VERSION, asmContext, bytecodeAssembler, false);
                    visitors.add(visitor);
                    return visitor;
                }
                return mv;
            }
        };

        cr.accept(cv, ClassReader.EXPAND_FRAMES);

        boolean anyInjected = visitors.stream().anyMatch(LineNumberVisitor::isInjected);
        if (!anyInjected) {
            LineNumberTarget target = (LineNumberTarget) asmContext.getInjectionTarget();
            throw new RuntimeException("行号 " + target.getLineNumber() + " 在方法 " + target.getMethodName() + " 中不存在");
        }

        return cw.toByteArray();
    }

    private boolean shouldInjectIntoMethod(String name, String descriptor, AsmInjectionContext asmContext) {
        if (asmContext.getInjectionTarget() instanceof LineNumberTarget) {
            LineNumberTarget target = (LineNumberTarget) asmContext.getInjectionTarget();
            String targetMethodName = target.getMethodName();
            if (targetMethodName != null && !targetMethodName.isEmpty()) {
                if (!name.equals(targetMethodName)) {
                    return false;
                }
                String targetDesc = target.getMethodDescriptor();
                if (targetDesc != null && !targetDesc.isEmpty()) {
                    return descriptor.equals(targetDesc);
                }
                return true;
            }
            return true;
        }
        return false;
    }
}
