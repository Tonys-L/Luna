package fun.efto.luna.core.plugin.builtin.snapshot;

import fun.efto.luna.core.bytecode.asm.AsmInjectionContext;
import fun.efto.luna.core.bytecode.asm.AsmInjectionContext.LocalVarInfo;
import fun.efto.luna.core.bytecode.asm.AsmTypeHelper;
import fun.efto.luna.core.bytecode.asm.assembler.ReferenceExpressionParser;
import fun.efto.luna.core.bytecode.asm.assembler.ExpressionSegment;
import fun.efto.luna.core.plugin.BytecodeHelper;
import fun.efto.luna.core.plugin.ExpressionHandler;
import fun.efto.luna.core.plugin.GenerateContext;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;

import java.util.List;

/**
 * @author ：Tony.L(286269159@qq.com)
 * @since  : 2026/05/16 10:00
 */
public class SnapshotExpressionHandler implements ExpressionHandler {
    @Override public String getProtocol() { return "snapshot"; }

    @Override
    public void generateBytecode(GenerateContext ctx) {
        AsmInjectionContext asmContext = ctx.asmContext();
        MethodVisitor mv = ctx.mv();
        BytecodeHelper h = ctx.helper();

        String methodDesc = asmContext.getInjectionPoint().getTarget().getMethodDescriptor();
        boolean isStatic = isStaticMethod(asmContext);
        List<ExpressionSegment.ParameterSegment> params = ReferenceExpressionParser.parseMethodParams(methodDesc, isStatic);
        List<LocalVarInfo> localVars = asmContext.getLocalVariables();
        List<LocalVarInfo> excludedVars = asmContext.getExcludedSameLineVariables();

        Label endLabel = null;
        if (ctx.hasCondition()) {
            endLabel = new Label();
            generateConditionCheck(mv, asmContext, params, isStatic, endLabel);
        }

        int safeLocalCount = 0;
        if (localVars != null) {
            for (LocalVarInfo lv : localVars) {
                if (excludedVars != null && excludedVars.stream().anyMatch(e -> e.getName().equals(lv.getName()))) {
                    continue;
                }
                safeLocalCount++;
            }
        }

        int totalVars = params.size() + safeLocalCount;
        String pointId = asmContext.getInjectionPoint().getId();

        h.loadString(pointId);

        // Object[] array: values
        AsmTypeHelper.emitIntConstant(mv, totalVars);
        mv.visitTypeInsn(Opcodes.ANEWARRAY, "java/lang/Object");
        int arrayIndex = 0;
        for (ExpressionSegment.ParameterSegment param : params) {
            emitArrayStore(mv, arrayIndex++, () -> AsmTypeHelper.loadAndBox(mv, param.getType(), param.getSlot()));
        }
        if (localVars != null) {
            for (LocalVarInfo lv : localVars) {
                if (excludedVars != null && excludedVars.stream().anyMatch(e -> e.getName().equals(lv.getName()))) {
                    continue;
                }
                emitArrayStore(mv, arrayIndex++, () -> AsmTypeHelper.loadAndBox(mv, Type.getType(lv.getDescriptor()), lv.getSlot()));
            }
        }

        // String[] array: names
        AsmTypeHelper.emitIntConstant(mv, totalVars);
        mv.visitTypeInsn(Opcodes.ANEWARRAY, "java/lang/String");
        arrayIndex = 0;
        for (int i = 0; i < params.size(); i++) {
            mv.visitInsn(Opcodes.DUP);
            AsmTypeHelper.emitIntConstant(mv, arrayIndex++);
            h.loadString("param[" + (i + 1) + "]");
            mv.visitInsn(Opcodes.AASTORE);
        }
        if (localVars != null) {
            for (LocalVarInfo lv : localVars) {
                if (excludedVars != null && excludedVars.stream().anyMatch(e -> e.getName().equals(lv.getName()))) {
                    continue;
                }
                mv.visitInsn(Opcodes.DUP);
                AsmTypeHelper.emitIntConstant(mv, arrayIndex++);
                h.loadString(lv.getName());
                mv.visitInsn(Opcodes.AASTORE);
            }
        }

        h.invokeStatic(SnapshotProbe.INTERNAL_NAME, "onSnapshot",
                "(Ljava/lang/String;[Ljava/lang/Object;[Ljava/lang/String;)V");

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
}
