/**
 * @author ：Tony.L(286269159@qq.com)
 * @since  : 2026/05/26 00:00
 */
package fun.efto.luna.core.analyzer;

import java.util.Collections;
import java.util.Map;

/**
 * 注解信息
 * 包含注解描述符、可见性及键值对属性
 */
public class AnnotationInfo {
    private final String descriptor;
    private final boolean visible;
    private final Map<String, Object> values;

    public AnnotationInfo(String descriptor, boolean visible, Map<String, Object> values) {
        this.descriptor = descriptor;
        this.visible = visible;
        this.values = values != null ? values : Collections.emptyMap();
    }

    public String getDescriptor() { return descriptor; }
    public boolean isVisible() { return visible; }
    public Map<String, Object> getValues() { return values; }
}
