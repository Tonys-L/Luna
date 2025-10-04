package fun.efto.luna.agent.clazz;

import java.security.ProtectionDomain;

/**
 * @author ：Tony.L(286269159@qq.com)
 * @since ：2025/10/2 22:30
 */
public class CompositeExcludeClassFilter implements ExcludeClassFilter {

    private ExcludeClassFilter[] filters;

    public CompositeExcludeClassFilter(ExcludeClassFilter... filters) {
        this.filters = filters;
    }

    @Override
    public boolean filter(String fqn, ClassLoader classLoader, ProtectionDomain protectionDomain) {
        for (ExcludeClassFilter filter : filters) {
            if (filter.filter(fqn, classLoader, protectionDomain)) {
                return true;
            }
        }
        return false;
    }
}
