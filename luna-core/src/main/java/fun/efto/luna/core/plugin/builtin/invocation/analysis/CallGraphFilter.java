package fun.efto.luna.core.plugin.builtin.invocation.analysis;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * @author : Tony.L(286269159@qq.com)
 * @since : 2026/07/05 20:00
 */
public class CallGraphFilter {

    private static final Set<String> SKIP_PREFIXES = Collections.unmodifiableSet(
        new HashSet<>(Arrays.asList(
            "java.", "javax.", "sun.", "com.sun.",
            "fun.efto.luna.core.", "fun.efto.luna.agent.",
            "fun.efto.luna.shadow."
        ))
    );

    public boolean shouldTrace(String className) {
        for (String prefix : SKIP_PREFIXES) {
            if (className.startsWith(prefix)) return false;
        }
        return true;
    }
}
