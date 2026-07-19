package fun.efto.luna.core.probe.invocation;

import fun.efto.luna.core.probe.invocation.model.*;
import fun.efto.luna.core.probe.BootstrapClassRegistry;
import fun.efto.luna.core.probe.ProbeMessage;
import fun.efto.luna.core.probe.ProbeOutput;
import fun.efto.luna.core.probe.ValueSerializer;

import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

/**
 * @author : Tony.L(286269159@qq.com)
 * @since  : 2026/07/05 20:00
 */
public class InvocationTraceProbe {

    public static final String INTERNAL_NAME = InvocationTraceProbe.class.getName().replace('.', '/');

    static {
        BootstrapClassRegistry.register(InvocationTraceProbe.class.getName());
        BootstrapClassRegistry.register(InvocationTrace.class.getName());
        BootstrapClassRegistry.register(InvocationSpan.class.getName());
        BootstrapClassRegistry.register(ActiveInvocation.class.getName());
        BootstrapClassRegistry.register(InvocationContextCarrier.class.getName());
        BootstrapClassRegistry.register(ThreadLocalInvocationCarrier.class.getName());
    }

    private static final ThreadLocalInvocationCarrier CARRIER = new ThreadLocalInvocationCarrier();
    private static final ThreadLocal<List<InvocationSpan>> PENDING_SPANS =
        ThreadLocal.withInitial(ArrayList::new);

    private InvocationTraceProbe() {}

    public static void onMethodEnter(String className, String methodName, Object[] args) {
        try {
            ActiveInvocation parent = CARRIER.current();
            String traceId;
            String parentSpanId;

            if (parent == null) {
                // Root method - generate new traceId
                traceId = generateTraceId();
                parentSpanId = null;
            } else {
                traceId = parent.getTraceId();
                parentSpanId = parent.getSpanId();
            }

            String spanId = generateSpanId();
            String serializedArgs = serializeArgs(args);

            ActiveInvocation invocation = new ActiveInvocation(
                traceId, spanId, parentSpanId,
                className, methodName,
                System.nanoTime(), serializedArgs
            );

            CARRIER.push(invocation);
        } catch (Throwable t) {
            // INV-002: Probe exceptions must not propagate to business code
        }
    }

    /**
     * Entry point for derived (ephemeral) INVOCATION injections.
     * Only participates in an existing trace context — does NOT start a new trace
     * if called outside an active trace. This prevents spurious independent traces
     * when a derived method is called from non-traced code paths.
     */
    public static void onDerivedMethodEnter(String className, String methodName, Object[] args) {
        try {
            ActiveInvocation parent = CARRIER.current();
            if (parent == null) return;  // Not within an active trace, skip

            String traceId = parent.getTraceId();
            String parentSpanId = parent.getSpanId();
            String spanId = generateSpanId();
            String serializedArgs = serializeArgs(args);

            ActiveInvocation invocation = new ActiveInvocation(
                traceId, spanId, parentSpanId,
                className, methodName,
                System.nanoTime(), serializedArgs
            );

            CARRIER.push(invocation);
        } catch (Throwable t) {
            // INV-002: Probe exceptions must not propagate to business code
        }
    }

    public static void onMethodExit(String className, String methodName, Object returnValue, Throwable thrown) {
        doMethodExit(className, methodName, serializeValue(returnValue), thrown != null,
            thrown != null ? thrown.getClass().getName() + ": " + thrown.getMessage() : null);
    }

    public static void onMethodExit(String className, String methodName, Object returnValue) {
        // When ATHROW captures the exception reference, returnValue is a Throwable
        if (returnValue instanceof Throwable) {
            Throwable thrown = (Throwable) returnValue;
            doMethodExit(className, methodName, serializeValue(returnValue), true,
                thrown.getClass().getName() + ": " + thrown.getMessage());
        } else {
            doMethodExit(className, methodName, serializeValue(returnValue), false, null);
        }
    }

    public static void onMethodExit(String className, String methodName) {
        doMethodExit(className, methodName, null, false, null);
    }

    private static void doMethodExit(String className, String methodName,
                                      String serializedReturn, boolean threwException, String exceptionMessage) {
        try {
            ActiveInvocation invocation = CARRIER.pop();
            if (invocation == null) return;

            long durationMs = (System.nanoTime() - invocation.getStartTimeNanos()) / 1_000_000;

            InvocationSpan span = new InvocationSpan(
                invocation.getSpanId(),
                invocation.getParentSpanId(),
                invocation.getClassName(),
                invocation.getMethodName(),
                durationMs,
                invocation.getArgs(),
                serializedReturn,
                threwException,
                exceptionMessage
            );

            boolean isRoot = CARRIER.isEmpty();

            if (isRoot) {
                // Root EXIT - output complete InvocationTrace
                List<InvocationSpan> allSpans = PENDING_SPANS.get();
                allSpans.add(span);

                InvocationTrace trace = new InvocationTrace(
                    invocation.getTraceId(),
                    invocation.getClassName(),
                    invocation.getMethodName(),
                    durationMs,
                    System.currentTimeMillis() - durationMs,  // approximate start timestamp
                    new ArrayList<>(allSpans)
                );

                // Build structured payload
                Map<String, Object> structuredPayload = new LinkedHashMap<>();
                structuredPayload.put("traceId", trace.getTraceId());
                structuredPayload.put("rootClassName", trace.getRootClassName());
                structuredPayload.put("rootMethodName", trace.getRootMethodName());
                structuredPayload.put("totalDurationMs", trace.getTotalDurationMs());
                structuredPayload.put("startTimestamp", trace.getStartTimestamp());

                List<Map<String, Object>> spanList = new ArrayList<>();
                for (InvocationSpan s : allSpans) {
                    Map<String, Object> spanMap = new LinkedHashMap<>();
                    spanMap.put("spanId", s.getSpanId());
                    spanMap.put("parentSpanId", s.getParentSpanId());
                    spanMap.put("className", s.getClassName());
                    spanMap.put("methodName", s.getMethodName());
                    spanMap.put("durationMs", s.getDurationMs());
                    spanMap.put("args", s.getArgs());
                    spanMap.put("returnValue", s.getReturnValue());
                    spanMap.put("threwException", s.isThrewException());
                    spanMap.put("exceptionMessage", s.getExceptionMessage());
                    spanList.add(spanMap);
                }
                structuredPayload.put("spans", spanList);

                String payload = "[INVOCATION] " + invocation.getClassName() + "." + invocation.getMethodName()
                    + " took " + durationMs + " ms (" + allSpans.size() + " spans)";

                ProbeOutput.offer(new ProbeMessage("INVOCATION", payload, structuredPayload, "tree"));

                // Clean up ThreadLocals
                PENDING_SPANS.remove();
                CARRIER.clear();
            } else {
                // Non-root - add span to pending list
                PENDING_SPANS.get().add(span);
            }
        } catch (Throwable t) {
            // INV-002: Probe exceptions must not propagate to business code
        }
    }

    private static String generateTraceId() {
        return Long.toHexString(System.currentTimeMillis()) + "-"
            + Long.toHexString(ThreadLocalRandom.current().nextLong());
    }

    private static String generateSpanId() {
        return Long.toHexString(ThreadLocalRandom.current().nextLong());
    }

    // Defensive argument serialization
    private static String serializeArgs(Object[] args) {
        if (args == null) return "null";
        try {
            StringBuilder sb = new StringBuilder("[");
            for (int i = 0; i < args.length; i++) {
                if (i > 0) sb.append(", ");
                sb.append(serializeValue(args[i]));
            }
            sb.append("]");
            return sb.toString();
        } catch (Throwable t) {
            return "[serialization error]";
        }
    }

    private static String serializeValue(Object value) {
        return ValueSerializer.serialize(value);
    }
}
