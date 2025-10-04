package fun.efto.luna.agent.clazz;


import fun.efto.luna.agent.Agent;

import java.security.ProtectionDomain;

/**
 * @author ：Tony.L(286269159@qq.com)
 * @since ：2025/10/2 21:13
 */
public class ExcludeAgentClassFilter implements ExcludeClassFilter {
    private static final ProtectionDomain AGENT_PROTECTION_DOMAIN = Agent.class.getProtectionDomain();

    @Override
    public boolean filter(String fqn, ClassLoader classLoader, ProtectionDomain protectionDomain) {
        return AGENT_PROTECTION_DOMAIN == protectionDomain;
    }

}
