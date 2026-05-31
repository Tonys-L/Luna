package fun.efto.luna.core.plugin.builtin.line.visitor;

import fun.efto.luna.core.bytecode.asm.AsmInjectionContext;
import fun.efto.luna.core.injection.code.CompiledCode;
import fun.efto.luna.core.injection.target.LineNumberTarget;
import fun.efto.luna.core.plugin.ProbeHandler;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

/**
 * @author : Tony.L(286269159@qq.com)
 * @since ：2025/11/5 15:30
 */
public class LineNumberVisitor extends MethodVisitor {
    private final CompiledCode compiledCode;
    private final ProbeHandler probeHandler;
    private final AsmInjectionContext context;
    private final boolean beforeLine;
    private int targetLineNumber;
    private boolean lineNumberVisited = false;
    private boolean injected = false;

    public LineNumberVisitor(int api, AsmInjectionContext context, CompiledCode compiledCode, ProbeHandler probeHandler, boolean beforeLine) {
        super(api, context.getMethodVisitor());
        this.compiledCode = compiledCode;
        this.probeHandler = probeHandler;
        this.context = context;
        this.beforeLine = beforeLine;

        if (context.getInjectionTarget() instanceof LineNumberTarget) {
            LineNumberTarget lineNumberTarget = (LineNumberTarget) context.getInjectionTarget();
            this.targetLineNumber = lineNumberTarget.getLineNumber();
        }
    }

    @Override
    public void visitLineNumber(int line, Label start) {
        if (beforeLine && line == targetLineNumber && !injected) {
            probeHandler.handle(compiledCode, buildGenerateContext());
            injected = true;
        }

        if (!beforeLine && lineNumberVisited && !injected) {
            probeHandler.handle(compiledCode, buildGenerateContext());
            injected = true;
        }

        if (line == targetLineNumber) {
            lineNumberVisited = true;
        }

        super.visitLineNumber(line, start);
    }

    private fun.efto.luna.core.plugin.GenerateContext buildGenerateContext() {
        return new fun.efto.luna.core.plugin.codegen.DefaultGenerateContext(
            "", context, compiledCode != null && compiledCode.hasCondition(), context.getMethodVisitor());
    }

    private void tryInjectBeforeReturn(int opcode) {
        if (!beforeLine && lineNumberVisited && !injected) {
            probeHandler.handle(compiledCode, buildGenerateContext());
            injected = true;
        }
    }

    @Override
    public void visitInsn(int opcode) {
        if (isReturnOpcode(opcode)) {
            tryInjectBeforeReturn(opcode);
        }
        super.visitInsn(opcode);
    }

    @Override
    public void visitVarInsn(int opcode, int var) {
        super.visitVarInsn(opcode, var);
    }

    @Override
    public void visitTypeInsn(int opcode, String type) {
        super.visitTypeInsn(opcode, type);
    }

    @Override
    public void visitFieldInsn(int opcode, String owner, String name, String descriptor) {
        super.visitFieldInsn(opcode, owner, name, descriptor);
    }

    @Override
    public void visitMethodInsn(int opcode, String owner, String name, String descriptor, boolean isInterface) {
        super.visitMethodInsn(opcode, owner, name, descriptor, isInterface);
    }

    @Override
    public void visitJumpInsn(int opcode, Label label) {
        super.visitJumpInsn(opcode, label);
    }

    @Override
    public void visitLabel(Label label) {
        super.visitLabel(label);
    }

    @Override
    public void visitEnd() {
        if (!beforeLine && lineNumberVisited && !injected) {
            probeHandler.handle(compiledCode, buildGenerateContext());
            injected = true;
        }
        super.visitEnd();
    }

    private boolean isReturnOpcode(int opcode) {
        return opcode == Opcodes.RETURN
                || opcode == Opcodes.IRETURN
                || opcode == Opcodes.LRETURN
                || opcode == Opcodes.FRETURN
                || opcode == Opcodes.DRETURN
                || opcode == Opcodes.ARETURN;
    }

    public boolean isInjected() {
        return injected;
    }
}
