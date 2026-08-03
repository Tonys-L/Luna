package fun.efto.luna.core.plugin.builtin.log;

import fun.efto.luna.core.bytecode.asm.AsmInjectionContext;
import fun.efto.luna.core.bytecode.asm.AsmTypeHelper;
import fun.efto.luna.core.bytecode.asm.assembler.ReferenceExpressionParser;
import fun.efto.luna.core.bytecode.asm.assembler.ExpressionSegment;
import fun.efto.luna.core.plugin.BytecodeHelper;
import fun.efto.luna.core.plugin.GenerateContext;
import fun.efto.luna.core.plugin.builtin.line.LineNumberInjectionLocation;
import fun.efto.luna.core.probe.ValueSerializer;
import fun.efto.luna.core.probe.log.LogProbe;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;

import java.util.List;

/**
 * @author : Tony.L(286269159@qq.com)
 * @since  : 2026/05/16 10:00
 */
public class LogExpressionHandler {

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

        String prefix = resolveLogPrefix(ctx);

        if (!hasRefs) {
            h.loadString(prefix + expression);
            h.invokeStatic(LogProbe.INTERNAL_NAME, "onLog", "(Ljava/lang/String;)V");
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
                emitArrayStore(mv, arrayIndex, () -> {
                    AsmTypeHelper.loadAndBox(mv, param.getType(), param.getSlot());
                    mv.visitMethodInsn(Opcodes.INVOKESTATIC, ValueSerializer.INTERNAL_NAME, "serialize",
                            "(Ljava/lang/Object;)Ljava/lang/String;", false);
                });
                arrayIndex++;
            } else if (seg instanceof ExpressionSegment.LocalVariableSegment) {
                ExpressionSegment.LocalVariableSegment lv = (ExpressionSegment.LocalVariableSegment) seg;
                emitArrayStore(mv, arrayIndex, () -> {
                    AsmTypeHelper.loadAndBox(mv, Type.getType(lv.getDescriptor()), lv.getSlot());
                    mv.visitMethodInsn(Opcodes.INVOKESTATIC, ValueSerializer.INTERNAL_NAME, "serialize",
                            "(Ljava/lang/Object;)Ljava/lang/String;", false);
                });
                arrayIndex++;
            }
        }

        mv.visitMethodInsn(Opcodes.INVOKESTATIC, "java/lang/String", "format",
                "(Ljava/lang/String;[Ljava/lang/Object;)Ljava/lang/String;", false);

        h.invokeStatic(LogProbe.INTERNAL_NAME, "onLog", "(Ljava/lang/String;)V");
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

    private static String resolveLogPrefix(GenerateContext ctx) {
        // AROUND 注入时，handle() 被调用两次（ENTER + EXIT），通过 phase 区分
        if (ctx.phase() == GenerateContext.Phase.EXIT) {
            return "method exit: ";
        }

        AsmInjectionContext asmContext = ctx.asmContext();
        if (asmContext.getInjectionPoint().getInjectionLocation() instanceof LineNumberInjectionLocation) {
            LineNumberInjectionLocation type = (LineNumberInjectionLocation) asmContext.getInjectionPoint().getInjectionLocation();
            if (type.getName().equals(LineNumberInjectionLocation.AFTER.getName())) {
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
