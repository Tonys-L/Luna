package fun.efto.luna.core.asm.injector;

import fun.efto.luna.core.asm.AsmInjectionContext;
import fun.efto.luna.core.bytecode.BytecodeAssembler;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.InsnNode;
import org.objectweb.asm.tree.IntInsnNode;
import org.objectweb.asm.tree.LdcInsnNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.FieldInsnNode;
import org.objectweb.asm.tree.TypeInsnNode;
import org.objectweb.asm.tree.VarInsnNode;

import java.util.ArrayList;
import java.util.List;

/**
 * @author : Tony.L(<286269159@qq.com>)
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
        private final List<org.objectweb.asm.tree.AbstractInsnNode> nodes = new ArrayList<>();

        InsnListCollector() {
            super(Opcodes.ASM9);
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

        InsnList toInsnList() {
            InsnList list = new InsnList();
            for (org.objectweb.asm.tree.AbstractInsnNode node : nodes) {
                list.add(node);
            }
            return list;
        }
    }
}
