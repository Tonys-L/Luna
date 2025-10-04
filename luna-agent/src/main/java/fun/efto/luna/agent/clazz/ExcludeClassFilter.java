package fun.efto.luna.agent.clazz;

import java.security.ProtectionDomain;

/**
 * @author ：Tony.L(286269159@qq.com)
 * @since ：2025/10/2 21:12
 */
public interface ExcludeClassFilter {
    boolean filter(String fqn, ClassLoader classLoader, ProtectionDomain protectionDomain);
}
