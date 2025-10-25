package fun.efto.luna.agent.clazz;

import java.security.ProtectionDomain;
import java.util.Arrays;
import java.util.List;

/**
 * @author ：Tony.L(286269159@qq.com)
 * @since ：2025/10/25 21:04
 */
public class ExcludeGeneratedClassFilter implements ExcludeClassFilter {
    private static final List<String> PROXY_CLASS_NAME_FEATURES = Arrays.asList(
            "$$EnhancerBySpringCGLIB",
            "$Proxy",
            "$$Lambda"

    );

    @Override
    public boolean filter(String fqn, ClassLoader classLoader, ProtectionDomain protectionDomain) {
        for (String proxyClassNameFeature : PROXY_CLASS_NAME_FEATURES) {
            if (fqn.contains(proxyClassNameFeature)) {
                return true;
            }
        }
        return false;
    }
}
