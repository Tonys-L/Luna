package fun.efto.luna.core.injection;

import fun.efto.luna.core.injection.code.InjectableCode;

/**
 * @author : Tony.L(286269159@qq.com)
 * @since  : 2026/05/17 18:20
 */
public interface CodeCompilerStrategy {
    boolean supports(String codeType, String code);
    InjectableCode compile(PersistentInjection persistent);
}
