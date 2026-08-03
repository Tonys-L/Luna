package fun.efto.luna.core.plugin;

import java.util.List;

/**
 * @author : Tony.L(286269159@qq.com)
 * @since  : 2026/05/11 22:00
 */
public interface BytecodeHelper {

    void loadArgument(int paramIndex);

    void loadLocalVar(String varName);

    void loadString(String value);

    void loadLong(long value);

    void buildFormattedString(String template, List<String> varRefs);

    void invokeStatic(String owner, String name, String descriptor);

    void invokeVirtual(String owner, String name, String descriptor);

    void invokeInterface(String owner, String name, String descriptor);

    void loadInt(int value);

    void newObject(String internalName, String descriptor);

    void storeLocal(int slot);

    void loadLocal(int slot);

    Object newLabel();

    void markLabel(Object label);

    void jump(Object label);

    void returnVoid();

    void loadNull();

    void newObjectArray(int size);

    void dup();

    void arrayStore();

    void loadArgumentBoxed(int paramIndex);
}
