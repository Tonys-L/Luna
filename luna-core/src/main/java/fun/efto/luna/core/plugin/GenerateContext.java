package fun.efto.luna.core.plugin;

import fun.efto.luna.core.asm.AsmInjectionContext;
import org.objectweb.asm.MethodVisitor;

/**
 * @author : Tony.L(<286269159@qq.com>)
 * @since  : 2026/05/11 22:00
 */
public interface GenerateContext {

    enum Phase { ENTER, EXIT }

    String expression();

    AsmInjectionContext asmContext();

    boolean hasCondition();

    BytecodeHelper helper();

    MethodVisitor mv();

    Phase injectionPhase();
}
