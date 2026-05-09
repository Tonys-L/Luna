package fun.efto.luna.core.asm.assmebler;

import fun.efto.luna.core.asm.AsmInjectionContext;
import fun.efto.luna.core.asm.AsmInjectionContext.LocalVarInfo;
import fun.efto.luna.core.injection.target.type.LineNumberInjectionType;
import fun.efto.luna.core.injection.target.type.MethodInjectionType;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author : Tony.L(286269159@qq.com)
 * @since : 2025/10/3 18:04
 */
public class ExpressionBytecodeAssembler extends BaseAsmBytecodeAssembler {

    private static final Pattern COMBINED_REF_PATTERN = Pattern.compile("\\$(\\d+|[a-zA-Z_]\\w*)");

    @Override
    protected void doAssemble(AsmInjectionContext asmContext, byte[] bytecode) {
        MethodVisitor mv = asmContext.getMethodVisitor();
        String content = asmContext.getInjectableCode().getCode();
        String[] split = content.split(":", 2);
        if (split.length != 2) {
            throw new IllegalArgumentException("invalid expression " + content);
        }

        String type = split[0];
        String expression = split[1];
        switch (type) {
            case "log":
                generateLogBytecode(mv, expression, asmContext);
                break;
            default:
                throw new IllegalArgumentException("unsupported expression " + content);
        }
    }

    private void generateLogBytecode(MethodVisitor mv, String expression, AsmInjectionContext asmContext) {
        String methodDesc = asmContext.getInjectionPoint().getTarget().getMethodDescriptor();
        if (methodDesc == null || methodDesc.isEmpty()) {
            mv.visitFieldInsn(Opcodes.GETSTATIC, "java/lang/System", "out", "Ljava/io/PrintStream;");
            String prefix = resolveLogPrefix(asmContext);
            mv.visitLdcInsn(prefix + expression);
            mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/io/PrintStream", "println", "(Ljava/lang/String;)V", false);
            return;
        }
        boolean isStatic = isStaticMethod(asmContext);
        List<ParameterInfo> params = parseMethodParams(methodDesc, isStatic);

        List<Object> segments = parseExpression(expression, params, asmContext.getLocalVariables());

        boolean hasRefs = segments.stream().anyMatch(s -> s instanceof ParameterInfo || s instanceof LocalVariableInfo);

        String prefix = resolveLogPrefix(asmContext);

        if (!hasRefs) {
            mv.visitFieldInsn(Opcodes.GETSTATIC, "java/lang/System", "out", "Ljava/io/PrintStream;");
            mv.visitLdcInsn(prefix + expression);
            mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/io/PrintStream", "println", "(Ljava/lang/String;)V", false);
            return;
        }

        StringBuilder formatStr = new StringBuilder(prefix);
        for (Object seg : segments) {
            if (seg instanceof String) {
                formatStr.append(escapeFormat((String) seg));
            } else {
                formatStr.append("%s");
            }
        }

        int refCount = (int) segments.stream()
                .filter(s -> s instanceof ParameterInfo || s instanceof LocalVariableInfo)
                .count();

        mv.visitLdcInsn(formatStr.toString());

        mv.visitInsn(Opcodes.ICONST_0 + refCount);
        mv.visitTypeInsn(Opcodes.ANEWARRAY, "java/lang/Object");

        int arrayIndex = 0;
        for (Object seg : segments) {
            if (seg instanceof ParameterInfo) {
                emitArrayStore(mv, arrayIndex, () -> loadParameterAsObject(mv, (ParameterInfo) seg));
                arrayIndex++;
            } else if (seg instanceof LocalVariableInfo) {
                emitArrayStore(mv, arrayIndex, () -> loadLocalVariableAsObject(mv, (LocalVariableInfo) seg));
                arrayIndex++;
            }
        }

        mv.visitMethodInsn(Opcodes.INVOKESTATIC, "java/lang/String", "format",
                "(Ljava/lang/String;[Ljava/lang/Object;)Ljava/lang/String;", false);

        mv.visitFieldInsn(Opcodes.GETSTATIC, "java/lang/System", "out", "Ljava/io/PrintStream;");
        mv.visitInsn(Opcodes.SWAP);
        mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/io/PrintStream", "println", "(Ljava/lang/String;)V", false);
    }

    private void emitArrayStore(MethodVisitor mv, int index, Runnable valueLoader) {
        mv.visitInsn(Opcodes.DUP);
        if (index <= 5) {
            mv.visitInsn(Opcodes.ICONST_0 + index);
        } else {
            mv.visitIntInsn(Opcodes.BIPUSH, index);
        }
        valueLoader.run();
        mv.visitInsn(Opcodes.AASTORE);
    }

    private void loadParameterAsObject(MethodVisitor mv, ParameterInfo param) {
        loadTypedAsObject(mv, param.getType(), param.getSlot());
    }

    private void loadLocalVariableAsObject(MethodVisitor mv, LocalVariableInfo localVar) {
        Type type = Type.getType(localVar.getDescriptor());
        loadTypedAsObject(mv, type, localVar.getSlot());
    }

    private void loadTypedAsObject(MethodVisitor mv, Type type, int slot) {
        switch (type.getSort()) {
            case Type.BOOLEAN:
                mv.visitVarInsn(Opcodes.ILOAD, slot);
                mv.visitMethodInsn(Opcodes.INVOKESTATIC, "java/lang/Boolean", "valueOf", "(Z)Ljava/lang/Boolean;", false);
                break;
            case Type.BYTE:
                mv.visitVarInsn(Opcodes.ILOAD, slot);
                mv.visitMethodInsn(Opcodes.INVOKESTATIC, "java/lang/Byte", "valueOf", "(B)Ljava/lang/Byte;", false);
                break;
            case Type.CHAR:
                mv.visitVarInsn(Opcodes.ILOAD, slot);
                mv.visitMethodInsn(Opcodes.INVOKESTATIC, "java/lang/Character", "valueOf", "(C)Ljava/lang/Character;", false);
                break;
            case Type.SHORT:
                mv.visitVarInsn(Opcodes.ILOAD, slot);
                mv.visitMethodInsn(Opcodes.INVOKESTATIC, "java/lang/Short", "valueOf", "(S)Ljava/lang/Short;", false);
                break;
            case Type.INT:
                mv.visitVarInsn(Opcodes.ILOAD, slot);
                mv.visitMethodInsn(Opcodes.INVOKESTATIC, "java/lang/Integer", "valueOf", "(I)Ljava/lang/Integer;", false);
                break;
            case Type.LONG:
                mv.visitVarInsn(Opcodes.LLOAD, slot);
                mv.visitMethodInsn(Opcodes.INVOKESTATIC, "java/lang/Long", "valueOf", "(J)Ljava/lang/Long;", false);
                break;
            case Type.FLOAT:
                mv.visitVarInsn(Opcodes.FLOAD, slot);
                mv.visitMethodInsn(Opcodes.INVOKESTATIC, "java/lang/Float", "valueOf", "(F)Ljava/lang/Float;", false);
                break;
            case Type.DOUBLE:
                mv.visitVarInsn(Opcodes.DLOAD, slot);
                mv.visitMethodInsn(Opcodes.INVOKESTATIC, "java/lang/Double", "valueOf", "(D)Ljava/lang/Double;", false);
                break;
            case Type.ARRAY:
            case Type.OBJECT:
                mv.visitVarInsn(Opcodes.ALOAD, slot);
                break;
            default:
                mv.visitVarInsn(Opcodes.ALOAD, slot);
                break;
        }
    }

    private List<Object> parseExpression(String expression, List<ParameterInfo> params, List<LocalVarInfo> localVars) {
        List<Object> segments = new ArrayList<>();
        Matcher matcher = COMBINED_REF_PATTERN.matcher(expression);
        int lastEnd = 0;

        while (matcher.find()) {
            if (matcher.start() > lastEnd) {
                segments.add(expression.substring(lastEnd, matcher.start()));
            }
            String ref = matcher.group(1);

            if (ref.matches("\\d+")) {
                int paramIndex = Integer.parseInt(ref);
                if (paramIndex < 1 || paramIndex > params.size()) {
                    throw new IllegalArgumentException("invalid parameter reference $" + paramIndex
                            + ", method has " + params.size() + " parameter(s)");
                }
                segments.add(params.get(paramIndex - 1));
            } else {
                LocalVarInfo matched = null;
                if (localVars != null) {
                    for (LocalVarInfo lv : localVars) {
                        if (lv.getName().equals(ref)) {
                            matched = lv;
                            break;
                        }
                    }
                }
                if (matched == null) {
                    segments.add("$" + ref);
                } else {
                    segments.add(new LocalVariableInfo(matched.getName(), matched.getDescriptor(), matched.getSlot()));
                }
            }
            lastEnd = matcher.end();
        }

        if (lastEnd < expression.length()) {
            segments.add(expression.substring(lastEnd));
        }

        if (segments.isEmpty()) {
            segments.add(expression);
        }

        return segments;
    }

    private List<ParameterInfo> parseMethodParams(String methodDescriptor, boolean isStatic) {
        List<ParameterInfo> params = new ArrayList<>();
        if (methodDescriptor == null || methodDescriptor.isEmpty()) {
            return params;
        }

        Type[] argTypes = Type.getArgumentTypes(methodDescriptor);
        int slot = isStatic ? 0 : 1;

        for (Type argType : argTypes) {
            params.add(new ParameterInfo(argType, slot));
            slot += argType.getSize();
        }

        return params;
    }

    private boolean isStaticMethod(AsmInjectionContext asmContext) {
        return (asmContext.getMethodAccess() & Opcodes.ACC_STATIC) != 0;
    }

    private String resolveLogPrefix(AsmInjectionContext asmContext) {
        if (asmContext.getInjectionPoint().getInjectionType() instanceof MethodInjectionType) {
            MethodInjectionType type = (MethodInjectionType) asmContext.getInjectionPoint().getInjectionType();
            if (type == MethodInjectionType.EXIT) {
                return "method exit ：";
            } else if (type == MethodInjectionType.AROUND) {
                return "method around ：";
            }
        } else if (asmContext.getInjectionPoint().getInjectionType() instanceof LineNumberInjectionType) {
            LineNumberInjectionType type = (LineNumberInjectionType) asmContext.getInjectionPoint().getInjectionType();
            if (type == LineNumberInjectionType.AFTER) {
                return "line after ：";
            }
            return "line before ：";
        }
        return "method enter ：";
    }

    private String escapeFormat(String s) {
        return s.replace("%", "%%");
    }

    private static class ParameterInfo {
        private final Type type;
        private final int slot;

        public ParameterInfo(Type type, int slot) {
            this.type = type;
            this.slot = slot;
        }

        public Type getType() {
            return type;
        }

        public int getSlot() {
            return slot;
        }
    }

    private static class LocalVariableInfo {
        private final String name;
        private final String descriptor;
        private final int slot;

        public LocalVariableInfo(String name, String descriptor, int slot) {
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
