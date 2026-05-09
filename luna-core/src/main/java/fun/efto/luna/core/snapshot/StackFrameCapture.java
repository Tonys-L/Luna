package fun.efto.luna.core.snapshot;

/**
 * 调用栈与快照结构捕获器
 *
 * @author : Tony.L(<286269159@qq.com>)
 * @since  : 2026/05/10 00:00
 */
public class StackFrameCapture {

    /**
     * 捕获当前的快照信息并转为 JSON
     * 
     * @param pointId 注入点 ID
     * @param localVars 当前拦截到的所有局部变量的值数组
     * @param varNames 局部变量的名称数组 (长度应与 localVars 相同)
     * @return 序列化后的快照 JSON 字符串
     */
    public static String capture(String pointId, Object[] localVars, String[] varNames) {
        StringBuilder json = new StringBuilder();
        json.append("{");
        
        // 基础信息
        json.append("\"type\":\"SNAPSHOT\",");
        json.append("\"pointId\":\"").append(pointId).append("\",");
        json.append("\"threadName\":\"").append(Thread.currentThread().getName()).append("\",");
        json.append("\"threadId\":").append(Thread.currentThread().getId()).append(",");
        json.append("\"timestamp\":").append(System.currentTimeMillis()).append(",");
        
        // 提取调用栈 (跳过当前类和 LunaSpy 的栈帧，保留真实的业务调用链)
        json.append("\"stackTrace\":[");
        StackTraceElement[] stackTrace = Thread.currentThread().getStackTrace();
        boolean firstStack = true;
        for (StackTraceElement element : stackTrace) {
            String className = element.getClassName();
            // 过滤内部调用
            if (className.startsWith("java.lang.Thread") || 
                className.startsWith("fun.efto.luna.core.")) {
                continue;
            }
            if (!firstStack) {
                json.append(",");
            }
            firstStack = false;
            
            json.append("{");
            json.append("\"class\":\"").append(className).append("\",");
            json.append("\"method\":\"").append(element.getMethodName()).append("\",");
            json.append("\"line\":").append(element.getLineNumber());
            json.append("}");
        }
        json.append("],");
        
        // 局部变量
        json.append("\"localVars\":{");
        if (localVars != null && varNames != null) {
            int length = Math.min(localVars.length, varNames.length);
            boolean firstVar = true;
            for (int i = 0; i < length; i++) {
                if (!firstVar) {
                    json.append(",");
                }
                firstVar = false;
                
                String name = varNames[i];
                Object value = localVars[i];
                
                json.append("\"").append(name).append("\":");
                // 调用防御式序列化器
                json.append(SnapshotSerializer.serialize(value));
            }
        }
        json.append("}");
        
        json.append("}");
        return json.toString();
    }
}
