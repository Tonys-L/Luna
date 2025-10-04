package fun.efto.luna.asm;

import fun.efto.luna.core.InjectionContext;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.MethodVisitor;

/**
 * @author ：Tony.L(286269159@qq.com)
 * @since ：2025/10/4 19:08
 */
public class AsmInjectionContext extends InjectionContext {
    private final byte[] bytecode;
    private ClassVisitor classVisitor;
    private MethodVisitor methodVisitor;

    public AsmInjectionContext(InjectionContext injectionContext, byte[] bytecode) {
        super(injectionContext.getInjectionPoint());
        this.bytecode = bytecode;
    }

    public byte[] getBytecode() {
        return bytecode;
    }

    public ClassVisitor getClassVisitor() {
        return classVisitor;
    }

    public void setClassVisitor(ClassVisitor classVisitor) {
        this.classVisitor = classVisitor;
    }

    public MethodVisitor getMethodVisitor() {
        return methodVisitor;
    }

    public void setMethodVisitor(MethodVisitor methodVisitor) {
        this.methodVisitor = methodVisitor;
    }


}
