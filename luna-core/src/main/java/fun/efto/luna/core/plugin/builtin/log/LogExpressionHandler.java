package fun.efto.luna.core.plugin.builtin.log;

import fun.efto.luna.core.bytecode.asm.AsmInjectionContext;
import fun.efto.luna.core.bytecode.asm.AsmInjectionContext.LocalVarInfo;
import fun.efto.luna.core.bytecode.asm.AsmTypeHelper;
import fun.efto.luna.core.bytecode.asm.assembler.ReferenceExpressionParser;
import fun.efto.luna.core.bytecode.asm.assembler.ExpressionSegment;
import fun.efto.luna.core.plugin.BytecodeHelper;
import fun.efto.luna.core.plugin.ExpressionHandler;
import fun.efto.luna.core.plugin.GenerateContext;
import fun.efto.luna.core.plugin.builtin.line.LineNumberInjectionType;
import fun.efto.luna.core.plugin.builtin.method.MethodInjectionType;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;

import java.util.List;

/**
 * @author : Tony.L(286269159@qq.com)
 * @since  : 2026/05/16 10:00
 */
public class LogExpressionHandler implements ExpressionHandler {
    @Override public String getProtocol() { return "log"; }

    @Override
    public void generateBytecode(GenerateContext ctx) {
        AsmInjectionContext asmContext = ctx.asmContext();
        MethodVisitor mv = ctx.mv();
        BytecodeHelper h = ctx.helper();
        String expression = ctx.expression();

        String methodDesc = asmContext.getInjectionPoint().getTarget().getMethodDescriptor();
        boolean isStatic = isStaticMethod(asmContext);
        List<ExpressionSegment.ParameterSegment> params = ReferenceExpressionParser.parseMethodParams(methodDesc, isStatic);

        List<ExpressionSegment> segments = ReferenceExpressionParser.parseExpression(expression, methodDesc, isStatic,
                asmContext.getLocalVariables(), asmContext.getExcludedSameLineVariables());

        boolean hasRefs = segments.stream()
                .anyMatch(s -> s instanceof ExpressionSegment.ParameterSegment || s instanceof ExpressionSegment.LocalVariableSegment);

        Label endLabel = null;
        if (ctx.hasCondition()) {
            endLabel = new Label();
            generateConditionCheck(mv, asmContext, params, isStatic, endLabel);
        }

        String prefix = resolveLogPrefix(asmContext);

        if (!hasRefs) {
            h.loadString(prefix + expression);
            h.invokeStatic(LogProbe.INTERNAL_NAME, "onLog", "(Ljava/lang/String;)V");

            if (ctx.hasCondition()) {
                mv.visitLabel(endLabel);
            }
            return;
        }

        StringBuilder formatStr = new StringBuilder(prefix);
        for (ExpressionSegment seg : segments) {
            if (seg instanceof ExpressionSegment.StringSegment) {
                formatStr.append(escapeFormat(((ExpressionSegment.StringSegment) seg).getText()));
            } else {
                formatStr.append("%s");
            }
        }

        int refCount = (int) segments.stream()
                .filter(s -> s instanceof ExpressionSegment.ParameterSegment || s instanceof ExpressionSegment.LocalVariableSegment)
                .count();

        mv.visitLdcInsn(formatStr.toString());
        AsmTypeHelper.emitIntConstant(mv, refCount);
        mv.visitTypeInsn(Opcodes.ANEWARRAY, "java/lang/Object");

        int arrayIndex = 0;
        for (ExpressionSegment seg : segments) {
            if (seg instanceof ExpressionSegment.ParameterSegment) {
                ExpressionSegment.ParameterSegment param = (ExpressionSegment.ParameterSegment) seg;
                emitArrayStore(mv, arrayIndex, () -> AsmTypeHelper.loadAndBox(mv, param.getType(), param.getSlot()));
                arrayIndex++;
            } else if (seg instanceof ExpressionSegment.LocalVariableSegment) {
                ExpressionSegment.LocalVariableSegment lv = (ExpressionSegment.LocalVariableSegment) seg;
                emitArrayStore(mv, arrayIndex, () -> AsmTypeHelper.loadAndBox(mv, Type.getType(lv.getDescriptor()), lv.getSlot()));
                arrayIndex++;
            }
        }

        mv.visitMethodInsn(Opcodes.INVOKESTATIC, "java/lang/String", "format",
                "(Ljava/lang/String;[Ljava/lang/Object;)Ljava/lang/String;", false);

        h.invokeStatic(LogProbe.INTERNAL_NAME, "onLog", "(Ljava/lang/String;)V");

        if (ctx.hasCondition()) {
            mv.visitLabel(endLabel);
        }
    }

    private void generateConditionCheck(MethodVisitor mv, AsmInjectionContext asmContext,
                                        List<ExpressionSegment.ParameterSegment> params,
                                        boolean isStatic, Label endLabel) {
        String injectionId = asmContext.getInjectionPoint().getId();

        mv.visitMethodInsn(Opcodes.INVOKESTATIC, "fun/efto/luna/core/expression/context/EvaluationContext",
                "getThreadLocal", "()Lfun/efto/luna/core/expression/context/EvaluationContext;", false);

        int maxSlot = isStatic ? 0 : 1;
        for (ExpressionSegment.ParameterSegment p : params) {
            maxSlot = Math.max(maxSlot, p.getSlot() + p.getType().getSize());
        }
        if (asmContext.getLocalVariables() != null) {
            List<LocalVarInfo> excluded = asmContext.getExcludedSameLineVariables();
            for (LocalVarInfo lv : asmContext.getLocalVariables()) {
                if (excluded != null && excluded.stream().anyMatch(e -> e.getName().equals(lv.getName()))) {
                    continue;
                }
                maxSlot = Math.max(maxSlot, lv.getSlot() + Type.getType(lv.getDescriptor()).getSize());
            }
        }
        int contextVarIndex = asmContext.getMaxLocals() > 0
                ? asmContext.getMaxLocals()
                : maxSlot + 1;

        mv.visitVarInsn(Opcodes.ASTORE, contextVarIndex);

        for (int i = 0; i < params.size(); i++) {
            mv.visitVarInsn(Opcodes.ALOAD, contextVarIndex);
            mv.visitLdcInsn("param[" + (i + 1) + "]");
            AsmTypeHelper.loadAndBox(mv, params.get(i).getType(), params.get(i).getSlot());
            mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "fun/efto/luna/core/expression/context/EvaluationContext",
                    "bind", "(Ljava/lang/String;Ljava/lang/Object;)Lfun/efto/luna/core/expression/context/EvaluationContext;", false);
            mv.visitInsn(Opcodes.POP);
        }
        if (asmContext.getLocalVariables() != null) {
            List<LocalVarInfo> excluded = asmContext.getExcludedSameLineVariables();
            for (LocalVarInfo lv : asmContext.getLocalVariables()) {
                if (excluded != null && excluded.stream().anyMatch(e -> e.getName().equals(lv.getName()))) {
                    continue;
                }
                mv.visitVarInsn(Opcodes.ALOAD, contextVarIndex);
                mv.visitLdcInsn(lv.getName());
                AsmTypeHelper.loadAndBox(mv, Type.getType(lv.getDescriptor()), lv.getSlot());
                mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "fun/efto/luna/core/expression/context/EvaluationContext",
                        "bind", "(Ljava/lang/String;Ljava/lang/Object;)Lfun/efto/luna/core/expression/context/EvaluationContext;", false);
                mv.visitInsn(Opcodes.POP);
            }
        }

        mv.visitLdcInsn(injectionId);
        mv.visitVarInsn(Opcodes.ALOAD, contextVarIndex);
        mv.visitMethodInsn(Opcodes.INVOKESTATIC, "fun/efto/luna/core/expression/ConditionRegistry",
                "test", "(Ljava/lang/String;Lfun/efto/luna/core/expression/context/EvaluationContext;)Z", false);
        mv.visitJumpInsn(Opcodes.IFEQ, endLabel);
    }

    private static void emitArrayStore(MethodVisitor mv, int index, Runnable valueLoader) {
        mv.visitInsn(Opcodes.DUP);
        AsmTypeHelper.emitIntConstant(mv, index);
        valueLoader.run();
        mv.visitInsn(Opcodes.AASTORE);
    }

    private static boolean isStaticMethod(AsmInjectionContext asmContext) {
        return (asmContext.getMethodAccess() & Opcodes.ACC_STATIC) != 0;
    }

    private static String resolveLogPrefix(AsmInjectionContext asmContext) {
        if (asmContext.getInjectionPoint().getInjectionType() instanceof MethodInjectionType) {
            MethodInjectionType type = (MethodInjectionType) asmContext.getInjectionPoint().getInjectionType();
            if (type.getName().equals(MethodInjectionType.EXIT.getName())) {
                return "method exit: ";
            } else if (type.getName().equals(MethodInjectionType.AROUND.getName())) {
                return "method around: ";
            }
        } else if (asmContext.getInjectionPoint().getInjectionType() instanceof LineNumberInjectionType) {
            LineNumberInjectionType type = (LineNumberInjectionType) asmContext.getInjectionPoint().getInjectionType();
            if (type.getName().equals(LineNumberInjectionType.AFTER.getName())) {
                return "line after: ";
            }
            return "line before: ";
        }
        return "method enter: ";
    }

    private static String escapeFormat(String s) {
        return s.replace("%", "%%");
    }
}
