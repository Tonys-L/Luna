package fun.efto.luna.core.asm.injector.visitor;

import fun.efto.luna.core.asm.AsmInjectionContext;
import fun.efto.luna.core.bytecode.BytecodeAssembler;
import fun.efto.luna.core.injection.target.LineNumberTarget;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;

/**
 * 行号访问器，用于在特定行号前或后注入代码
 *
 * @author ：Tony.L(286269159@qq.com)
 * @since ：2025/11/5 15:30
 */
public class LineNumberVisitor extends MethodVisitor {
    private final BytecodeAssembler bytecodeAssembler;
    private final AsmInjectionContext context;
    private final boolean beforeLine; // true表示在行前注入，false表示在行后注入
    private int targetLineNumber;
    private boolean lineNumberVisited = false;
    private boolean injected = false;

    public LineNumberVisitor(int api, AsmInjectionContext context, BytecodeAssembler bytecodeAssembler, boolean beforeLine) {
        super(api, context.getMethodVisitor());
        this.bytecodeAssembler = bytecodeAssembler;
        this.context = context;
        this.beforeLine = beforeLine;

        // 获取目标行号
        if (context.getInjectionTarget() instanceof LineNumberTarget) {
            LineNumberTarget lineNumberTarget = (LineNumberTarget) context.getInjectionTarget();
            this.targetLineNumber = lineNumberTarget.getLineNumber();
        }
    }

    @Override
    public void visitLineNumber(int line, Label start) {
        // 如果是在行前注入且当前行号匹配目标行号
        if (beforeLine && line == targetLineNumber && !injected) {
            bytecodeAssembler.assemble(context, context.getBytecode());
            injected = true;
        }

        // 标记已访问行号
        if (line == targetLineNumber) {
            lineNumberVisited = true;
        }

        super.visitLineNumber(line, start);
    }

    @Override
    public void visitInsn(int opcode) {
        // 如果是在行后注入，且已访问目标行号但尚未注入
        if (!beforeLine && lineNumberVisited && !injected) {
            // 在下一个指令之前注入代码
            bytecodeAssembler.assemble(context, context.getBytecode());
            injected = true;
        }
        super.visitInsn(opcode);
    }

    @Override
    public void visitVarInsn(int opcode, int var) {
        // 如果是在行后注入，且已访问目标行号但尚未注入
        if (!beforeLine && lineNumberVisited && !injected) {
            // 在下一个指令之前注入代码
            bytecodeAssembler.assemble(context, context.getBytecode());
            injected = true;
        }
        super.visitVarInsn(opcode, var);
    }

    @Override
    public void visitTypeInsn(int opcode, String type) {
        // 如果是在行后注入，且已访问目标行号但尚未注入
        if (!beforeLine && lineNumberVisited && !injected) {
            // 在下一个指令之前注入代码
            bytecodeAssembler.assemble(context, context.getBytecode());
            injected = true;
        }
        super.visitTypeInsn(opcode, type);
    }

    @Override
    public void visitFieldInsn(int opcode, String owner, String name, String descriptor) {
        // 如果是在行后注入，且已访问目标行号但尚未注入
        if (!beforeLine && lineNumberVisited && !injected) {
            // 在下一个指令之前注入代码
            bytecodeAssembler.assemble(context, context.getBytecode());
            injected = true;
        }
        super.visitFieldInsn(opcode, owner, name, descriptor);
    }

    @Override
    public void visitMethodInsn(int opcode, String owner, String name, String descriptor, boolean isInterface) {
        // 如果是在行后注入，且已访问目标行号但尚未注入
        if (!beforeLine && lineNumberVisited && !injected) {
            // 在下一个指令之前注入代码
            bytecodeAssembler.assemble(context, context.getBytecode());
            injected = true;
        }
        super.visitMethodInsn(opcode, owner, name, descriptor, isInterface);
    }

    @Override
    public void visitJumpInsn(int opcode, Label label) {
        // 如果是在行后注入，且已访问目标行号但尚未注入
        if (!beforeLine && lineNumberVisited && !injected) {
            // 在下一个指令之前注入代码
            bytecodeAssembler.assemble(context, context.getBytecode());
            injected = true;
        }
        super.visitJumpInsn(opcode, label);
    }

    @Override
    public void visitLabel(Label label) {
        // 如果是在行后注入，且已访问目标行号但尚未注入
        if (!beforeLine && lineNumberVisited && !injected) {
            // 在下一个指令之前注入代码
            bytecodeAssembler.assemble(context, context.getBytecode());
            injected = true;
        }
        super.visitLabel(label);
    }
}