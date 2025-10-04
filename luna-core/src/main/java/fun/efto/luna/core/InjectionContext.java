package fun.efto.luna.core;

import fun.efto.luna.core.injection.InjectionPoint;
import fun.efto.luna.core.injection.code.InjectableCode;
import fun.efto.luna.core.injection.target.InjectionTarget;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author ：Tony.L(286269159@qq.com)
 * @since ：2025/10/4 17:20
 */
public class InjectionContext {
    private final InjectionPoint injectionPoint;
    private final Map<String, Object> attributes = new ConcurrentHashMap<>();

    public InjectionContext(InjectionPoint injectionPoint) {
        this.injectionPoint = injectionPoint;
    }


    public InjectionPoint getInjectionPoint() {
        return injectionPoint;
    }

    public InjectionTarget getInjectionTarget() {
        return injectionPoint.getTarget();
    }

    public InjectableCode getInjectableCode() {
        return injectionPoint.getCode();
    }

    public Object getAttribute(String key) {
        return attributes.get(key);
    }

    public void setAttribute(String key, Object value) {
        attributes.put(key, value);
    }

    public void removeAttribute(String key) {
        attributes.remove(key);
    }

    public void clearAttributes() {
        attributes.clear();
    }


}
