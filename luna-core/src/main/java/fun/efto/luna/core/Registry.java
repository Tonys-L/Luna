package fun.efto.luna.core;

import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * @author ：Tony.L(286269159@qq.com)
 * @since ：2025/10/4 16:04
 */
public interface Registry<K, V> {

    Map<K, V> getRegistry();

    default V register(K k, V v) {
        getRegistry().put(k, v);
        return v;
    }

    default Optional<V> get(K k) {
        V v = getRegistry().get(k);
        if (Objects.isNull(v)) {
            return Optional.empty();
        }
        return Optional.of(v);
    }
}
