package fun.efto.luna.core.injection.code;

import fun.efto.luna.core.injection.code.type.CodeType;

/**
 * @author ：Tony.L(286269159@qq.com)
 * @since ：2025/10/4 2:51
 */
public interface InjectableCode {
    CodeType getType();

    String getContent();

    boolean isValid();
}
