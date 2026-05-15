package fun.efto.luna.core.plugin;

import fun.efto.luna.core.asm.AsmInjectionContext;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;

import java.util.List;

/**
 * @author : Tony.L(<286269159@qq.com>)
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
    @Override
    public void invokeStatic(String owner, String name, String descriptor) {
        mv.visitMethodInsn(Opcodes.INVOKESTATIC, owner, name, descriptor, false);
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
        switch (type.getSort()) {
            case Type.BOOLEAN: case Type.BYTE: case Type.CHAR: case Type.SHORT: case Type.INT:
                mv.visitVarInsn(Opcodes.ILOAD, slot); break;
            case Type.LONG:
                mv.visitVarInsn(Opcodes.LLOAD, slot); break;
            case Type.FLOAT:
                mv.visitVarInsn(Opcodes.FLOAD, slot); break;
            case Type.DOUBLE:
                mv.visitVarInsn(Opcodes.DLOAD, slot); break;
            default:
                mv.visitVarInsn(Opcodes.ALOAD, slot); break;
        }
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
        switch (type.getSort()) {
            case Type.BOOLEAN:
                mv.visitMethodInsn(Opcodes.INVOKESTATIC, "java/lang/Boolean", "valueOf", "(Z)Ljava/lang/Boolean;", false);
                break;
            case Type.BYTE:
                mv.visitMethodInsn(Opcodes.INVOKESTATIC, "java/lang/Byte", "valueOf", "(B)Ljava/lang/Byte;", false);
                break;
            case Type.CHAR:
                mv.visitMethodInsn(Opcodes.INVOKESTATIC, "java/lang/Character", "valueOf", "(C)Ljava/lang/Character;", false);
                break;
            case Type.SHORT:
                mv.visitMethodInsn(Opcodes.INVOKESTATIC, "java/lang/Short", "valueOf", "(S)Ljava/lang/Short;", false);
                break;
            case Type.INT:
                mv.visitMethodInsn(Opcodes.INVOKESTATIC, "java/lang/Integer", "valueOf", "(I)Ljava/lang/Integer;", false);
                break;
            case Type.LONG:
                mv.visitMethodInsn(Opcodes.INVOKESTATIC, "java/lang/Long", "valueOf", "(J)Ljava/lang/Long;", false);
                break;
            case Type.FLOAT:
                mv.visitMethodInsn(Opcodes.INVOKESTATIC, "java/lang/Float", "valueOf", "(F)Ljava/lang/Float;", false);
                break;
            case Type.DOUBLE:
                mv.visitMethodInsn(Opcodes.INVOKESTATIC, "java/lang/Double", "valueOf", "(D)Ljava/lang/Double;", false);
                break;
            default:
                break;
        }
    }

    private void emitIntConstant(int value) {
        if (value >= -1 && value <= 5) {
            mv.visitInsn(Opcodes.ICONST_0 + value);
        } else if (value >= Byte.MIN_VALUE && value <= Byte.MAX_VALUE) {
            mv.visitIntInsn(Opcodes.BIPUSH, value);
        } else if (value >= Short.MIN_VALUE && value <= Short.MAX_VALUE) {
            mv.visitIntInsn(Opcodes.SIPUSH, value);
        } else {
            mv.visitLdcInsn(value);
        }
    }
}
