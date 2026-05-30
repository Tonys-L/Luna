/**
 * @author : Tony.L(<286269159@qq.com>)
 * @since  : 2026/05/17 18:20
 */
package fun.efto.luna.core.injection;

import fun.efto.luna.core.injection.code.InjectableCode;

public interface CodeCompilerStrategy {
    boolean supports(String codeType, String code);
    InjectableCode compile(PersistentInjection persistent);
}
