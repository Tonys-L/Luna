package fun.efto.luna.core.plugin.codegen;

import fun.efto.luna.core.bytecode.asm.AsmInjectionContext;
import fun.efto.luna.core.bytecode.asm.AsmTypeHelper;
import fun.efto.luna.core.plugin.BytecodeHelper;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;

import java.util.List;

/**
 * @author : Tony.L(286269159@qq.com)
 * @since  : 2026/05/11 22:00
 */
public class DefaultBytecodeHelper implements BytecodeHelper {
    private final MethodVisitor mv;
    private final AsmInjectionContext asmContext;

    public DefaultBytecodeHelper(MethodVisitor mv, AsmInjectionContext asmContext) {
        this.mv = mv;
        this.asmContext = asmContext;
    }

    @Override public void loadString(String value) { mv.visitLdcInsn(value); }
    @Override public void loadLong(long value) { mv.visitLdcInsn(value); }
    @Override public void returnVoid() { mv.visitInsn(Opcodes.RETURN); }
    @Override public void loadNull() { mv.visitInsn(Opcodes.ACONST_NULL); }

    @Override
    public void newObjectArray(int size) {
        emitIntConstant(size);
        mv.visitTypeInsn(Opcodes.ANEWARRAY, "java/lang/Object");
    }

    @Override
    public void dup() { mv.visitInsn(Opcodes.DUP); }

    @Override
    public void arrayStore() { mv.visitInsn(Opcodes.AASTORE); }

    @Override
    public void loadArgumentBoxed(int paramIndex) {
        loadArgument(paramIndex);
        boxIfNeeded(asmContext.getInjectionTarget().getMethodDescriptor(), paramIndex);
    }
    @Override
    public void invokeStatic(String owner, String name, String descriptor) {
        mv.visitMethodInsn(Opcodes.INVOKESTATIC, owner, name, descriptor, false);
    }

    @Override
    public void invokeVirtual(String owner, String name, String descriptor) {
        mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, owner, name, descriptor, false);
    }

    @Override
    public void invokeInterface(String owner, String name, String descriptor) {
        mv.visitMethodInsn(Opcodes.INVOKEINTERFACE, owner, name, descriptor, true);
    }

    @Override
    public void loadInt(int value) {
        emitIntConstant(value);
    }

    @Override
    public void newObject(String internalName, String descriptor) {
        mv.visitTypeInsn(Opcodes.NEW, internalName);
        mv.visitInsn(Opcodes.DUP);
        mv.visitMethodInsn(Opcodes.INVOKESPECIAL, internalName, "<init>", descriptor, false);
    }

    @Override
    public void storeLocal(int slot) {
        mv.visitVarInsn(Opcodes.ASTORE, slot);
    }

    @Override
    public void loadLocal(int slot) {
        mv.visitVarInsn(Opcodes.ALOAD, slot);
    }

    @Override
    public Object newLabel() {
        return new Label();
    }

    @Override
    public void markLabel(Object label) {
        mv.visitLabel((Label) label);
    }

    @Override
    public void jump(Object label) {
        mv.visitJumpInsn(Opcodes.GOTO, (Label) label);
    }

    @Override
    public void loadArgument(int paramIndex) {
        String desc = asmContext.getInjectionTarget().getMethodDescriptor();
        boolean isStatic = (asmContext.getMethodAccess() & Opcodes.ACC_STATIC) != 0;
        Type[] argTypes = (desc != null && !desc.isEmpty())
                ? Type.getArgumentTypes(desc)
                : new Type[0];
        if (paramIndex < 1 || paramIndex > argTypes.length) {
            throw new IllegalArgumentException("paramIndex out of range: " + paramIndex);
        }
        int slot = isStatic ? 0 : 1;
        for (int i = 0; i < paramIndex - 1; i++) {
            slot += argTypes[i].getSize();
        }
        Type targetType = argTypes[paramIndex - 1];
        emitLoadByType(slot, targetType);
    }

    @Override
    public void loadLocalVar(String varName) {
        for (AsmInjectionContext.LocalVarInfo lv : asmContext.getLocalVariables()) {
            if (varName.equals(lv.getName())) {
                Type varType = Type.getType(lv.getDescriptor());
                emitLoadByType(lv.getSlot(), varType);
                return;
            }
        }
        throw new IllegalArgumentException("Local variable not found: " + varName);
    }

    @Override
    public void buildFormattedString(String template, List<String> varRefs) {
        String formatStr = template.replace("{", "%s").replace("}", "");
        mv.visitLdcInsn(formatStr);
        emitIntConstant(varRefs.size());
        mv.visitTypeInsn(Opcodes.ANEWARRAY, "java/lang/Object");
        for (int i = 0; i < varRefs.size(); i++) {
            mv.visitInsn(Opcodes.DUP);
            emitIntConstant(i);
            loadVarRefAsObject(varRefs.get(i));
            mv.visitInsn(Opcodes.AASTORE);
        }
        mv.visitMethodInsn(Opcodes.INVOKESTATIC, "java/lang/String", "format",
                "(Ljava/lang/String;[Ljava/lang/Object;)Ljava/lang/String;", false);
    }

    private void emitLoadByType(int slot, Type type) {
        AsmTypeHelper.load(mv, type, slot);
    }

    private void loadVarRefAsObject(String varRef) {
        if (varRef.startsWith("$") && varRef.length() > 1) {
            String rest = varRef.substring(1);
            try {
                int paramIndex = Integer.parseInt(rest);
                loadArgument(paramIndex);
                boxIfNeeded(asmContext.getInjectionTarget().getMethodDescriptor(), paramIndex);
            } catch (NumberFormatException e) {
                loadLocalVar(rest);
                boxLocalVarIfNeeded(rest);
            }
        } else {
            mv.visitLdcInsn(varRef);
        }
    }

    private void boxIfNeeded(String desc, int paramIndex) {
        boolean isStatic = (asmContext.getMethodAccess() & Opcodes.ACC_STATIC) != 0;
        Type[] argTypes = (desc != null && !desc.isEmpty()) ? Type.getArgumentTypes(desc) : new Type[0];
        if (paramIndex < 1 || paramIndex > argTypes.length) return;
        Type targetType = argTypes[paramIndex - 1];
        boxType(targetType);
    }

    private void boxLocalVarIfNeeded(String varName) {
        for (AsmInjectionContext.LocalVarInfo lv : asmContext.getLocalVariables()) {
            if (varName.equals(lv.getName())) {
                boxType(Type.getType(lv.getDescriptor()));
                return;
            }
        }
    }

    private void boxType(Type type) {
        AsmTypeHelper.box(mv, type);
    }

    private void emitIntConstant(int value) {
        AsmTypeHelper.emitIntConstant(mv, value);
    }
}
