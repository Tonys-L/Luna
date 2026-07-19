package fun.efto.luna.core.infra.util;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * 标记仅用于测试的方法，表明其可见性高于正常需要。
 *
 * @author : Tony.L(286269159@qq.com)
 * @since  : 2026/05/17 18:50
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
public @interface VisibleForTesting {
}
