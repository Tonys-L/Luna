package fun.efto.luna.core.injection;

import fun.efto.luna.core.injection.code.CompiledCode;

/**
 * @author : Tony.L(286269159@qq.com)
 * @since  : 2026/06/01 10:00
 */
public interface CodeEngine {
    String getCodeType();
    CompiledCode compile(PersistentInjection persistent);
}
