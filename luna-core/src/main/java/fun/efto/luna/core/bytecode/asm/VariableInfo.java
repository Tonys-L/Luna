package fun.efto.luna.core.bytecode.asm;

import java.util.Objects;

/**
 * @author ：Tony.L(286269159@qq.com)
 * @since  : 2026/05/31 21:00
 */
public final class VariableInfo {

    private final String name;
    private final String descriptor;
    private final int slot;

    public VariableInfo(String name, String descriptor, int slot) {
        this.name = name;
        this.descriptor = descriptor;
        this.slot = slot;
    }

    public String getName() {
        return name;
    }

    public String getDescriptor() {
        return descriptor;
    }

    public int getSlot() {
        return slot;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        VariableInfo that = (VariableInfo) o;
        return slot == that.slot && Objects.equals(name, that.name) && Objects.equals(descriptor, that.descriptor);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, descriptor, slot);
    }

    @Override
    public String toString() {
        return "VariableInfo{name=" + name + ", descriptor=" + descriptor + ", slot=" + slot + "}";
    }
}
