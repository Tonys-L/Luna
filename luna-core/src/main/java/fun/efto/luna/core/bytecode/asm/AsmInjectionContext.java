package fun.efto.luna.core.bytecode.asm;

import fun.efto.luna.core.injection.InjectionContext;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.MethodVisitor;

import java.util.Collections;
import java.util.List;

/**
 * @author ：Tony.L(286269159@qq.com)
 * @since ：2025/10/4 19:08
 */
public class AsmInjectionContext extends InjectionContext {
    private final byte[] bytecode;
    private ClassVisitor classVisitor;
    private MethodVisitor methodVisitor;
    private List<LocalVarInfo> localVariables = Collections.emptyList();
    private List<LocalVarInfo> excludedSameLineVariables = Collections.emptyList();
    private int methodAccess;
    private int maxLocals;

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

    public List<LocalVarInfo> getLocalVariables() {
        return localVariables;
    }

    public void setLocalVariables(List<LocalVarInfo> localVariables) {
        this.localVariables = localVariables != null ? localVariables : Collections.emptyList();
    }

    public List<LocalVarInfo> getExcludedSameLineVariables() {
        return excludedSameLineVariables;
    }

    public void setExcludedSameLineVariables(List<LocalVarInfo> excludedSameLineVariables) {
        this.excludedSameLineVariables = excludedSameLineVariables != null ? excludedSameLineVariables : Collections.emptyList();
    }

    public int getMethodAccess() {
        return methodAccess;
    }

    public void setMethodAccess(int methodAccess) {
        this.methodAccess = methodAccess;
    }

    public int getMaxLocals() {
        return maxLocals;
    }

    public void setMaxLocals(int maxLocals) {
        this.maxLocals = maxLocals;
    }

    public static class LocalVarInfo {
        private final String name;
        private final String descriptor;
        private final int slot;

        public LocalVarInfo(String name, String descriptor, int slot) {
            this.name = name;
            this.descriptor = descriptor;
            this.slot = slot;
        }

        public String getName() {
            return name;
        }

        public String getDescriptor() {
            return descriptor;
        }

        public int getSlot() {
            return slot;
        }
    }
}
