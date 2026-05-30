package fun.efto.luna.core.injection;

/**
 * @author : Tony.L(286269159@qq.com)
 * @since  : 2026/05/31 21:00
 */
public interface CodeInjector {

    byte[] inject(InjectionPoint point, byte[] bytecode);
}
