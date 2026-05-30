package fun.efto.luna.core.plugin.builtin.method.visitor;

import fun.efto.luna.core.asm.AsmInjectionContext;
import fun.efto.luna.core.bytecode.BytecodeAssembler;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

/**
 * 使用 try-finally 模式保证退出代码在正常返回和异常抛出时均被执行的 MethodVisitor。
 *
 * <p>ASM 字节码模式对应的 Java 伪代码：
 * <pre>
 * try {
 *     // enterAssembler 生成的内容（方法入口）
 *     // 原始方法体
 *     // exitAssembler 生成的内容（在 RETURN 指令之前）
 * } catch (Throwable t) {
 *     // exitAssembler 生成的内容（异常路径）
 *     throw t;
 * }
 * </pre>
 *
 * @author : Tony.L(&lt;286269159@qq.com&gt;)
 * @since  : 2026/05/25 10:00
 */
public class TryFinallyMethodVisitor extends MethodVisitor {

    private final BytecodeAssembler enterAssembler;
    private final BytecodeAssembler exitAssembler;
    private final AsmInjectionContext context;

    private final Label tryStart = new Label();
    private final Label tryEnd = new Label();
    private final Label handler = new Label();
    private final Label endLabel = new Label();

    public TryFinallyMethodVisitor(int api, AsmInjectionContext context,
                                   BytecodeAssembler enterAssembler, BytecodeAssembler exitAssembler) {
        super(api, context.getMethodVisitor());
        this.context = context;
        this.enterAssembler = enterAssembler;
        this.exitAssembler = exitAssembler;
    }

    @Override
    public void visitCode() {
        super.visitCode();
        // 注入入口代码
        enterAssembler.assemble(context, context.getBytecode());
        // 标记 try 块起始
        mv.visitLabel(tryStart);
    }

    @Override
    public void visitInsn(int opcode) {
        if (isReturnOpcode(opcode)) {
            // 正常返回路径：在 RETURN 之前注入退出代码
            exitAssembler.assemble(context, context.getBytecode());
        }
        super.visitInsn(opcode);
    }

    @Override
    public void visitEnd() {
        // 标记 try 块结束
        mv.visitLabel(tryEnd);
        // 正常路径跳过 handler
        mv.visitJumpInsn(Opcodes.GOTO, endLabel);

        // 异常处理器：catch (any)
        mv.visitLabel(handler);
        // 异常路径：注入退出代码
        exitAssembler.assemble(context, context.getBytecode());
        // 重新抛出异常
        mv.visitInsn(Opcodes.ATHROW);

        // 方法结束标签
        mv.visitLabel(endLabel);

        // 注册 try-catch 块（null 表示 catch any，即 catch Throwable）
        mv.visitTryCatchBlock(tryStart, tryEnd, handler, null);

        super.visitEnd();
    }

    /**
     * 判断是否为方法正常返回字节码（IRETURN ~ RETURN）。
     * 不包含 ATHROW，因为 ATHROW 路径由 try-catch handler 处理。
     */
    private static boolean isReturnOpcode(int opcode) {
        return opcode >= Opcodes.IRETURN && opcode <= Opcodes.RETURN;
    }
}
