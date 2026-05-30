package fun.efto.luna.core.plugin;

/**
 * @author ：Tony.L(286269159@qq.com)
 * @since  : 2026/05/11 22:00
 */

public interface ExpressionHandler {

    String getProtocol();

    void generateBytecode(GenerateContext ctx);
}
