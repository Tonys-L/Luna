package fun.efto.luna.agent.web.vo;

import java.util.List;

/**
 * 局部变量列表 VO
 *
 * @author : Tony.L(<286269159@qq.com>)
 * @since  : 2026/05/27 12:00
 */
public class LocalVariablesVO {
    private final List<LocalVarInfo> variables;

    public LocalVariablesVO(List<LocalVarInfo> variables) {
        this.variables = variables;
    }

    public List<LocalVarInfo> getVariables() {
        return variables;
    }

    /**
     * 局部变量信息
     *
     * @author : Tony.L(<286269159@qq.com>)
     * @since  : 2026/05/27 12:00
     */
    public static class LocalVarInfo {
        private final String name;
        private final String descriptor;
        private final int slot;

        public LocalVarInfo(String name, String descriptor, int slot) {
            this.name = name;
            this.descriptor = descriptor;
            this.slot = slot;
        }

        public String getName() {
            return name;
        }

        public String getDescriptor() {
            return descriptor;
        }

        public int getSlot() {
            return slot;
        }
    }
}
