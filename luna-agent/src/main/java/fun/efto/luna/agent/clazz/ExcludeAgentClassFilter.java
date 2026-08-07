package fun.efto.luna.agent.clazz;


import java.security.ProtectionDomain;

/**
 * 按类名前缀排除 Luna 自身的类（含 shade 后的 shadow 包）。
 * 不依赖 ProtectionDomain 比较，因为核心类可能由不同 ClassLoader 加载，ProtectionDomain 不一致。
 *
 * @author ：Tony.L(286269159@qq.com)
 * @since ：2025/10/2 21:13
 */
public class ExcludeAgentClassFilter implements ExcludeClassFilter {
    private static final String LUNA_PACKAGE_PREFIX = "fun.efto.luna.";

    @Override
    public boolean filter(String fqn, ClassLoader classLoader, ProtectionDomain protectionDomain) {
        return fqn != null && fqn.startsWith(LUNA_PACKAGE_PREFIX);
    }

}
