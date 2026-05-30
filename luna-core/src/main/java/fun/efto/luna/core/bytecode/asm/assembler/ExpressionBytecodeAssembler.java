package fun.efto.luna.core.bytecode.asm.assembler;

import fun.efto.luna.core.bytecode.asm.AsmInjectionContext;
import fun.efto.luna.core.expression.ConditionRegistry;
import fun.efto.luna.core.plugin.BytecodeHelper;
import fun.efto.luna.core.plugin.ExpressionHandler;
import fun.efto.luna.core.plugin.GenerateContext;
import fun.efto.luna.core.plugin.codegen.DefaultBytecodeHelper;
import fun.efto.luna.core.plugin.registry.ExpressionHandlerRegistry;
import org.objectweb.asm.MethodVisitor;

/**
 * @author : Tony.L(286269159@qq.com)
 * @since  : 2025/10/3 18:04
 */
public class ExpressionBytecodeAssembler extends BaseAsmBytecodeAssembler {

    @Override
    protected void doAssemble(AsmInjectionContext asmContext, byte[] bytecode) {
        MethodVisitor mv = asmContext.getMethodVisitor();
        String content = asmContext.getInjectableCode().getCode();

        String condition = null;
        String trimmed = content.trim();
        if (trimmed.startsWith("${") && trimmed.contains("}::")) {
            int idx = trimmed.indexOf("}::");
            condition = trimmed.substring(0, idx + 1).trim();
            content = trimmed.substring(idx + 3).trim();
        }

        String injectionId = asmContext.getInjectionPoint().getId();
        if (condition != null) {
            ConditionRegistry.register(injectionId, condition);
        } else {
            ConditionRegistry.unregister(injectionId);
        }

        String[] split = content.split(":", 2);
        String type = split[0];
        final String finalExpression = split.length > 1 ? split[1] : "";
        final boolean finalHasCondition = condition != null;
        ExpressionHandler handler = ExpressionHandlerRegistry.getInstance().get(type).orElse(null);
        if (handler != null) {
            handler.generateBytecode(new GenerateContext() {
                @Override public String expression() { return finalExpression; }
                @Override public AsmInjectionContext asmContext() { return asmContext; }
                @Override public boolean hasCondition() { return finalHasCondition; }
                @Override public BytecodeHelper helper() { return new DefaultBytecodeHelper(mv, asmContext); }
                @Override public MethodVisitor mv() { return mv; }
                @Override public Phase injectionPhase() { return Phase.ENTER; }
            });
        } else {
            throw new IllegalArgumentException("unsupported expression protocol " + type);
        }
    }
}
