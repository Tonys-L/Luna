package fun.efto.luna.core.plugin;

import fun.efto.luna.core.bytecode.asm.AsmInjectionContext;
import org.objectweb.asm.MethodVisitor;

/**
 * @author : Tony.L(286269159@qq.com)
 * @since  : 2026/05/11 22:00
 */
public interface GenerateContext {

    enum Phase { ENTER, EXIT }

    String expression();

    AsmInjectionContext asmContext();

    boolean hasCondition();

    BytecodeHelper helper();

    MethodVisitor mv();

    /**
     * 当前代码生成阶段。AROUND 注入时，handle() 会被调用两次（ENTER + EXIT），
     * 插件可据此生成不同的代码（如 TRACE 在 ENTER 记录起点，在 EXIT 计算耗时）。
     */
    default Phase phase() { return Phase.ENTER; }
}
