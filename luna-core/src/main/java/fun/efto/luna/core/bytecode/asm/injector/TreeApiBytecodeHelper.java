package fun.efto.luna.core.bytecode.asm.injector;

import fun.efto.luna.core.bytecode.asm.AsmInjectionContext;
import fun.efto.luna.core.bytecode.asm.AsmInjectionContext.LocalVarInfo;
import fun.efto.luna.core.bytecode.asm.AsmTypeHelper;
import fun.efto.luna.core.bytecode.asm.assembler.ExpressionSegment;
import fun.efto.luna.core.bytecode.asm.assembler.ReferenceExpressionParser;
import fun.efto.luna.core.injection.code.CompiledCode;
import fun.efto.luna.core.plugin.GenerateContext;
import fun.efto.luna.core.plugin.ProbeHandler;
import fun.efto.luna.core.plugin.codegen.DefaultGenerateContext;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author : Tony.L(286269159@qq.com)
 * @since  : 2026/05/04 13:30
 */
public class TreeApiBytecodeHelper {

    public static InsnList assemble(AsmInjectionContext asmContext, CompiledCode compiledCode, ProbeHandler probeHandler, byte[] bytecode) {
        return assemble(asmContext, compiledCode, probeHandler, bytecode, null);
    }

    public static InsnList assemble(AsmInjectionContext asmContext, CompiledCode compiledCode, ProbeHandler probeHandler, byte[] bytecode, GenerateContext.Phase phase) {
        InsnListCollector collector = new InsnListCollector();
        MethodVisitor originalMv = asmContext.getMethodVisitor();
        asmContext.setMethodVisitor(collector);

        String content = compiledCode != null && compiledCode.getContent() != null ? compiledCode.getContent() : "";
        String condition = compiledCode != null ? compiledCode.getCondition() : null;
        boolean hasCondition = compiledCode != null && compiledCode.hasCondition();

        GenerateContext ctx = new DefaultGenerateContext(content, asmContext, hasCondition, collector, phase);

        if (hasCondition) {
            String methodDesc = asmContext.getInjectionPoint().getTarget().getMethodDescriptor();
            boolean isStatic = isStaticMethod(asmContext);
            List<ExpressionSegment.ParameterSegment> params = ReferenceExpressionParser.parseMethodParams(methodDesc, isStatic);

            Label skipLabel = new Label();
            generateConditionCheck(collector, asmContext, params, isStatic, skipLabel);
            probeHandler.handle(compiledCode, ctx);
            collector.visitLabel(skipLabel);
        } else {
            probeHandler.handle(compiledCode, ctx);
        }

        asmContext.setMethodVisitor(originalMv);
        return collector.toInsnList();
    }

    private static void generateConditionCheck(MethodVisitor mv, AsmInjectionContext asmContext,
                                                List<ExpressionSegment.ParameterSegment> params, boolean isStatic,
                                                Label skipLabel) {
        String injectionId = asmContext.getInjectionPoint().getId();

        mv.visitMethodInsn(Opcodes.INVOKESTATIC, "fun/efto/luna/core/expression/context/EvaluationContext",
                "create", "()Lfun/efto/luna/core/expression/context/EvaluationContext;", false);
        int contextVarIndex = asmContext.getMaxLocals();
        mv.visitVarInsn(Opcodes.ASTORE, contextVarIndex);

        if (!isStatic) {
            mv.visitVarInsn(Opcodes.ALOAD, contextVarIndex);
            mv.visitLdcInsn("this");
            mv.visitVarInsn(Opcodes.ALOAD, 0);
            mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "fun/efto/luna/core/expression/context/EvaluationContext",
                    "bind", "(Ljava/lang/String;Ljava/lang/Object;)Lfun/efto/luna/core/expression/context/EvaluationContext;", false);
            mv.visitInsn(Opcodes.POP);
        }

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
        mv.visitJumpInsn(Opcodes.IFEQ, skipLabel);
    }

    private static boolean isStaticMethod(AsmInjectionContext asmContext) {
        return (asmContext.getMethodAccess() & Opcodes.ACC_STATIC) != 0;
    }

    private static class InsnListCollector extends MethodVisitor {
        private final List<AbstractInsnNode> nodes = new ArrayList<>();
        private final Map<Label, LabelNode> labelMap = new HashMap<>();

        InsnListCollector() {
            super(Opcodes.ASM9);
        }

        private LabelNode getLabelNode(Label label) {
            return labelMap.computeIfAbsent(label, k -> new LabelNode());
        }

        @Override
        public void visitInsn(int opcode) {
            nodes.add(new InsnNode(opcode));
        }

        @Override
        public void visitIntInsn(int opcode, int operand) {
            nodes.add(new IntInsnNode(opcode, operand));
        }

        @Override
        public void visitVarInsn(int opcode, int var) {
            nodes.add(new VarInsnNode(opcode, var));
        }

        @Override
        public void visitTypeInsn(int opcode, String type) {
            nodes.add(new TypeInsnNode(opcode, type));
        }

        @Override
        public void visitFieldInsn(int opcode, String owner, String name, String descriptor) {
            nodes.add(new FieldInsnNode(opcode, owner, name, descriptor));
        }

        @Override
        public void visitMethodInsn(int opcode, String owner, String name, String descriptor, boolean isInterface) {
            nodes.add(new MethodInsnNode(opcode, owner, name, descriptor, isInterface));
        }

        @Override
        public void visitLdcInsn(Object value) {
            nodes.add(new LdcInsnNode(value));
        }

        @Override
        public void visitJumpInsn(int opcode, Label label) {
            nodes.add(new JumpInsnNode(opcode, getLabelNode(label)));
        }

        @Override
        public void visitLabel(Label label) {
            nodes.add(getLabelNode(label));
        }

        @Override
        public void visitLineNumber(int line, Label start) {
            nodes.add(new LineNumberNode(line, getLabelNode(start)));
        }

        @Override
        public void visitFrame(int type, int numLocal, Object[] local, int numStack, Object[] stack) {
            nodes.add(new FrameNode(type, numLocal, local, numStack, stack));
        }

        InsnList toInsnList() {
            InsnList list = new InsnList();
            for (AbstractInsnNode node : nodes) {
                list.add(node);
            }
            return list;
        }
    }
}
