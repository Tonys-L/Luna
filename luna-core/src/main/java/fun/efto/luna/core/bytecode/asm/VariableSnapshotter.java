package fun.efto.luna.core.bytecode.asm;

import java.util.List;

/**
 * @author : Tony.L(286269159@qq.com)
 * @since  : 2026/05/31 21:00
 */
public interface VariableSnapshotter {

    List<VariableInfo> snapshot(byte[] bytecode, String method, String desc, int line);
}
