package fun.efto.luna.core.injection.rule;

import java.util.regex.Pattern;

/**
 * @author : Tony.L(286269159@qq.com)
 * @since  : 2026/05/15 10:30
 */
public final class ClassNameMatcher {

    private ClassNameMatcher() {
    }

    public static boolean matches(String className, String pattern) {
        if (className == null || pattern == null) return false;
        if (className.equals(pattern)) return true;
        if (!pattern.contains("*") && !pattern.contains("?")) return false;
        String regex = pattern.replace(".", "\\.").replace("*", ".*").replace("?", ".");
        return className.matches(regex);
    }

    public static Pattern toRegex(String pattern) {
        if (pattern == null) return Pattern.compile("");
        String regex = pattern.replace(".", "\\.").replace("*", ".*").replace("?", ".");
        return Pattern.compile(regex);
    }
}
