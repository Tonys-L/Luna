package fun.efto.luna.agent.clazz;

import java.security.ProtectionDomain;

/**
 * @author ：Tony.L(286269159@qq.com)
 * @since ：2025/10/2 0:52
 */
public class ExcludeArrayClassFilter implements ExcludeClassFilter {
    @Override
    public boolean filter(String fqn, ClassLoader classLoader, ProtectionDomain protectionDomain) {
        return fqn.startsWith("[");
    }
}
