package fun.efto.luna.core.bytecode.asm.injector;

import fun.efto.luna.core.bytecode.asm.AsmInjectionContext;
import fun.efto.luna.core.bytecode.BytecodeAssembler;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
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

    public static InsnList assemble(AsmInjectionContext asmContext, byte[] bytecode, BytecodeAssembler bytecodeAssembler) {
        InsnListCollector collector = new InsnListCollector();
        MethodVisitor originalMv = asmContext.getMethodVisitor();
        asmContext.setMethodVisitor(collector);
        bytecodeAssembler.assemble(asmContext, bytecode);
        asmContext.setMethodVisitor(originalMv);
        return collector.toInsnList();
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
