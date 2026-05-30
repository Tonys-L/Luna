package fun.efto.luna.core.plugin.codegen;

import fun.efto.luna.core.asm.AsmInjectionContext;
import fun.efto.luna.core.plugin.BytecodeHelper;
import fun.efto.luna.core.plugin.GenerateContext;
import org.objectweb.asm.MethodVisitor;

/**
 * @author : Tony.L(<286269159@qq.com>)
 * @since  : 2026/05/11 22:00
 */
public class DefaultGenerateContext implements GenerateContext {
    private final String expression;
    private final AsmInjectionContext asmContext;
    private final boolean hasCondition;
    private final MethodVisitor mv;
    private final Phase phase;
    private BytecodeHelper helper;

    public DefaultGenerateContext(String expression, AsmInjectionContext asmContext, boolean hasCondition, MethodVisitor mv) {
        this(expression, asmContext, hasCondition, mv, Phase.ENTER);
    }

    public DefaultGenerateContext(String expression, AsmInjectionContext asmContext, boolean hasCondition, MethodVisitor mv, Phase phase) {
        this.expression = expression;
        this.asmContext = asmContext;
        this.hasCondition = hasCondition;
        this.mv = mv;
        this.phase = phase;
    }

    @Override public String expression() { return expression; }
    @Override public AsmInjectionContext asmContext() { return asmContext; }
    @Override public boolean hasCondition() { return hasCondition; }
    @Override public MethodVisitor mv() { return mv; }
    @Override public Phase injectionPhase() { return phase; }
    @Override
    public BytecodeHelper helper() {
        if (helper == null) {
            helper = new DefaultBytecodeHelper(mv, asmContext);
        }
        return helper;
    }
}
