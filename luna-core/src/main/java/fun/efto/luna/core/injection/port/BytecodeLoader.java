package fun.efto.luna.core.injection.port;

/**
 * BytecodeLoader（出站端口）。
 * 领域层需要"获取类字节码"的能力，不关心底层是 ClassLoader 读取还是其他方式。
 *
 * @author : Tony.L(<286269159@qq.com>)
 * @since  : 2026/05/27 20:00
 */
public interface BytecodeLoader {

    /**
     * 加载指定类的字节码
     *
     * @param className 全限定类名
     * @return 类字节码
     * @throws Exception 加载失败时抛出
     */
    byte[] loadBytecode(String className) throws Exception;
}
