package fun.efto.luna.core.plugin.builtin.snapshot;

/**
 * @author : Tony.L(<286269159@qq.com>)
 * @since  : 2026/05/10 00:00
 */
public class StackFrameCapture {

    public static String capture(String pointId, Object[] localVars, String[] varNames) {
        StringBuilder json = new StringBuilder();
        json.append("{");

        json.append("\"type\":\"SNAPSHOT\",");
        json.append("\"pointId\":\"").append(pointId).append("\",");
        json.append("\"threadName\":\"").append(Thread.currentThread().getName()).append("\",");
        json.append("\"threadId\":").append(Thread.currentThread().getId()).append(",");
        json.append("\"timestamp\":").append(System.currentTimeMillis()).append(",");

        json.append("\"stackTrace\":[");
        StackTraceElement[] stackTrace = Thread.currentThread().getStackTrace();
        boolean firstStack = true;
        for (StackTraceElement element : stackTrace) {
            String className = element.getClassName();
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
                json.append(SnapshotSerializer.serialize(value));
            }
        }
        json.append("}");

        json.append("}");
        return json.toString();
    }
}
