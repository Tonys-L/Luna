package fun.efto.luna.core.plugin.builtin.snapshot;

import fun.efto.luna.core.bytecode.asm.AsmInjectionContext;
import fun.efto.luna.core.bytecode.asm.AsmInjectionContext.LocalVarInfo;
import fun.efto.luna.core.bytecode.asm.AsmTypeHelper;
import fun.efto.luna.core.bytecode.asm.assembler.ReferenceExpressionParser;
import fun.efto.luna.core.bytecode.asm.assembler.ExpressionSegment;
import fun.efto.luna.core.plugin.BytecodeHelper;
import fun.efto.luna.core.plugin.GenerateContext;
import fun.efto.luna.core.probe.snapshot.SnapshotProbe;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;

import java.util.List;

/**
 * @author : Tony.L(286269159@qq.com)
 * @since  : 2026/05/16 10:00
 */
public class SnapshotExpressionHandler {

    public void generateBytecode(GenerateContext ctx) {
        AsmInjectionContext asmContext = ctx.asmContext();
        MethodVisitor mv = ctx.mv();
        BytecodeHelper h = ctx.helper();

        String methodDesc = asmContext.getInjectionPoint().getTarget().getMethodDescriptor();
        boolean isStatic = isStaticMethod(asmContext);
        List<ExpressionSegment.ParameterSegment> params = ReferenceExpressionParser.parseMethodParams(methodDesc, isStatic);
        List<LocalVarInfo> localVars = asmContext.getLocalVariables();
        List<LocalVarInfo> excludedVars = asmContext.getExcludedSameLineVariables();

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
