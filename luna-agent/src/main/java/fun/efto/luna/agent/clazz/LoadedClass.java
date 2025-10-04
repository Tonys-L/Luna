package fun.efto.luna.agent.clazz;

import com.alibaba.fastjson.annotation.JSONField;

import java.util.Objects;

/**
 * @author ：Tony.L(286269159@qq.com)
 * @since ：2025/10/2 16:13
 */
public class LoadedClass {
    private final String className;
    @JSONField(serialize = false)
    private Class<?> clazz;

    public LoadedClass(String className) {
        this.className = className;
    }


    public String getClassName() {
        return className;
    }

    public Class<?> getClazz() {
        if (clazz == null) {
            try {
                clazz = Class.forName(className);
            } catch (ClassNotFoundException e) {
                return null;
            }
        }
        return clazz;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        LoadedClass that = (LoadedClass) o;
        return Objects.equals(className, that.className) && Objects.equals(clazz, that.clazz);
    }

    @Override
    public int hashCode() {
        return Objects.hash(className, clazz);
    }
}
