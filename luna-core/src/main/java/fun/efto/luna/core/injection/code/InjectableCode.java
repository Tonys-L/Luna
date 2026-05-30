package fun.efto.luna.core.injection.code;

import fun.efto.luna.core.injection.code.CodeType;

/**
 * 可注入代码接口
 * @author : Tony.L(286269159@qq.com>)
 * @since ：2026/03/29 02:30
 */
public interface InjectableCode {

    /**
     * 获取代码内容
     * @return 代码内容
     */
    String getCode();

    /**
     * 获取代码类型
     * @return 代码类型
     */
    CodeType getCodeType();
}
