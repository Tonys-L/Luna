package fun.efto.luna.agent;

import java.lang.instrument.Instrumentation;

/**
 * @author ：Tony.L(286269159@qq.com)
 * @since ：2025/10/3 18:52
 */
public class Agent {
    private Agent() {
    }

    public static void premain(String args, Instrumentation inst) {
        agentmain(args, inst);
    }

    public static void agentmain(String args, Instrumentation inst) {
        // todo :
    }
}

