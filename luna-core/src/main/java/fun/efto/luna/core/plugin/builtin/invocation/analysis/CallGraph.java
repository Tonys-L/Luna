package fun.efto.luna.core.plugin.builtin.invocation.analysis;

import java.util.ArrayList;
import java.util.List;

/**
 * @author : Tony.L(286269159@qq.com)
 * @since : 2026/07/05 20:00
 */
public class CallGraph {
    private final String className;
    private final String methodName;
    private final String methodDesc;
    private final List<CallGraph> callees = new ArrayList<>();

    public CallGraph(String className, String methodName, String methodDesc) {
        this.className = className;
        this.methodName = methodName;
        this.methodDesc = methodDesc;
    }

    public String getClassName() { return className; }
    public String getMethodName() { return methodName; }
    public String getMethodDesc() { return methodDesc; }
    public List<CallGraph> getCallees() { return callees; }

    public void addCallee(CallGraph callee) {
        callees.add(callee);
    }
}
