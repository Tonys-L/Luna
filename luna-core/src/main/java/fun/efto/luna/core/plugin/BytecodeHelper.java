package fun.efto.luna.core.plugin;

import java.util.List;

/**
 * @author : Tony.L(<286269159@qq.com>)
 * @since  : 2026/05/11 22:00
 */
public interface BytecodeHelper {

    void loadArgument(int paramIndex);

    void loadLocalVar(String varName);

    void loadString(String value);

    void loadLong(long value);

    void buildFormattedString(String template, List<String> varRefs);

    void invokeStatic(String owner, String name, String descriptor);

    void returnVoid();
}
