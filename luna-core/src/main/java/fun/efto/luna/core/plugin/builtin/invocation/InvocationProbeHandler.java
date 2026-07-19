package fun.efto.luna.core.plugin.builtin.invocation;

import fun.efto.luna.core.injection.*;
import fun.efto.luna.core.injection.code.CompiledCode;
import fun.efto.luna.core.plugin.*;
import fun.efto.luna.core.probe.invocation.InvocationTraceProbe;
import fun.efto.luna.core.plugin.builtin.invocation.analysis.*;
import org.objectweb.asm.Type;

import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * @author : Tony.L(286269159@qq.com)
 * @since : 2026/07/05 20:00
 */
public class InvocationProbeHandler extends AbstractProbeHandler {

    private static final Logger LOGGER = Logger.getLogger(InvocationProbeHandler.class.getName());

    private static final Set<String> SUPPORTED_LOCATIONS = Collections.unmodifiableSet(
        new HashSet<>(Collections.singletonList("method_around"))
    );

    @Override public String getProbeType() { return "INVOCATION"; }
    @Override public boolean usesCode() { return false; }
    @Override public Set<String> supportedInjectionLocations() { return SUPPORTED_LOCATIONS; }
    @Override public boolean capturesReturnValue() { return true; }

    @Override
    protected ValidationResult doValidate(InjectRequest request) {
        return ValidationResult.ok();
    }

    @Override public String getDisplayName() { return "调用链追踪"; }
    @Override public String getSyntax() { return "自动追踪方法调用链，展示调用关系、入参、返回值和耗时"; }
    @Override public String getIcon() { return "fas fa-project-diagram"; }
    @Override public String getGlyphColor() { return "#10b981"; }
    @Override public String getCategory() { return "observability"; }

    @Override
    public List<FormFieldSchema> getConfigSchema() {
        return Arrays.asList(
            new FormFieldSchema("maxDepth", "追踪深度", "number", "5", null, false, "调用图分析的最大递归深度"),
            new FormFieldSchema("captureArgs", "捕获入参", "select", "true",
                Arrays.asList(
                    new FormFieldSchema.Option("true", "是"),
                    new FormFieldSchema.Option("false", "否")
                ), false, "是否捕获方法入参"),
            new FormFieldSchema("captureReturn", "捕获返回值", "select", "true",
                Arrays.asList(
                    new FormFieldSchema.Option("true", "是"),
                    new FormFieldSchema.Option("false", "否")
                ), false, "是否捕获方法返回值")
        );
    }

    @Override
    public void handle(CompiledCode code, GenerateContext ctx) {
        BytecodeHelper h = ctx.helper();
        String className = ctx.asmContext().getInjectionPoint().getTarget().getTargetClass();
        String methodName = ctx.asmContext().getInjectionPoint().getTarget().getMethodName();

        // Distinguish root vs derived injection:
        // - Root (non-ephemeral): user explicitly injected → can start new traces
        // - Derived (ephemeral): auto-created by call graph analysis → only participate in existing traces
        boolean isDerived = isDerivedInjection(ctx);

        if (ctx.phase() == GenerateContext.Phase.ENTER) {
            h.loadString(className);
            h.loadString(methodName);
            // Create Object[] from method arguments
            String methodDesc = ctx.asmContext().getInjectionPoint().getTarget().getMethodDescriptor();
            Type[] argTypes = (methodDesc != null && !methodDesc.isEmpty())
                ? Type.getArgumentTypes(methodDesc) : new Type[0];
            h.newObjectArray(argTypes.length);
            for (int i = 0; i < argTypes.length; i++) {
                h.dup();
                h.loadInt(i);
                h.loadArgumentBoxed(i + 1); // 1-based index
                h.arrayStore();
            }
            String enterMethod = isDerived ? "onDerivedMethodEnter" : "onMethodEnter";
            h.invokeStatic(InvocationTraceProbe.INTERNAL_NAME, enterMethod,
                "(Ljava/lang/String;Ljava/lang/String;[Ljava/lang/Object;)V");
        } else {
            // EXIT: capture return value if available (non-void methods).
            // The injector's insertBeforeReturns generates pre-code that stores the
            // boxed return value (or null for void/ATHROW) into the returnCaptureSlot.
            h.loadString(className);
            h.loadString(methodName);
            int returnCaptureSlot = ctx.asmContext().getReturnCaptureSlot();
            if (returnCaptureSlot >= 0) {
                h.loadLocal(returnCaptureSlot);
                h.invokeStatic(InvocationTraceProbe.INTERNAL_NAME, "onMethodExit",
                    "(Ljava/lang/String;Ljava/lang/String;Ljava/lang/Object;)V");
            } else {
                // Void method — no return value to capture
                h.invokeStatic(InvocationTraceProbe.INTERNAL_NAME, "onMethodExit",
                    "(Ljava/lang/String;Ljava/lang/String;)V");
            }
        }
    }

    private boolean isDerivedInjection(GenerateContext ctx) {
        try {
            PersistentInjection source = ctx.asmContext().getInjectionPoint().toPersistentInjection();
            // Use groupId to distinguish root vs derived:
            // - Root injection (user-initiated): groupId == null
            // - Derived injection (auto-created by injectCallees): groupId != null
            return source != null && source.getGroupId() != null;
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "isDerivedInjection exception", e);
            return false;
        }
    }

    @Override
    public void onInject(PersistentInjection injection, PluginContext ctx) {
        try {
            int maxDepth = resolveMaxDepth(injection);
            StaticCallGraphAnalyzer analyzer = new StaticCallGraphAnalyzer(ctx, maxDepth);

            LOGGER.info("onInject: analyzing " + injection.getClazz() + "." + injection.getMethodName()
                + " desc=" + injection.getMethodDescriptor() + " maxDepth=" + maxDepth);

            CallGraph callGraph = analyzer.analyze(
                injection.getClazz(),
                injection.getMethodName(),
                injection.getMethodDescriptor()
            );

            LOGGER.info("onInject: found " + callGraph.getCallees().size() + " direct callees for "
                + injection.getClazz() + "." + injection.getMethodName());

            // Create derived injections for all callees (with deduplication)
            String groupId = injection.getGroupId() != null ? injection.getGroupId() : injection.getId();
            Set<String> injected = new HashSet<>();
            injectCallees(callGraph, groupId, ctx, injected);
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "onInject failed for " + injection.getClazz() + "." + injection.getMethodName(), e);
        }
    }

    private void injectCallees(CallGraph node, String groupId, PluginContext ctx, Set<String> injected) {
        for (CallGraph callee : node.getCallees()) {
            try {
                String key = callee.getClassName() + "." + callee.getMethodName() + callee.getMethodDesc();
                if (injected.contains(key)) {
                    LOGGER.fine("injectCallees: skipping duplicate " + key);
                    continue;
                }
                injected.add(key);

                LOGGER.info("injectCallees: injecting " + callee.getClassName() + "." + callee.getMethodName()
                    + " desc=" + callee.getMethodDesc() + " groupId=" + groupId);

                InjectRequest derivedRequest = new InjectRequest();
                derivedRequest.setClazz(callee.getClassName());
                derivedRequest.setMethod(callee.getMethodName());
                derivedRequest.setDesc(callee.getMethodDesc());
                derivedRequest.setProbeType("INVOCATION");
                derivedRequest.setInjectionLocation("method_around");
                derivedRequest.setCodeType("EXPRESSION");
                derivedRequest.setCode("");
                derivedRequest.setEphemeral(true);
                derivedRequest.setGroupId(groupId);

                String id = ctx.inject(derivedRequest);
                LOGGER.info("injectCallees: injected " + callee.getClassName() + "." + callee.getMethodName() + " -> id=" + id);

                // Recursively inject for sub-callees
                injectCallees(callee, groupId, ctx, injected);
            } catch (Exception e) {
                LOGGER.log(Level.WARNING, "injectCallees: failed for " + callee.getClassName() + "." + callee.getMethodName(), e);
            }
        }
    }

    @Override
    public void onDelete(PersistentInjection injection, InjectionRepository repository, InjectionRegistry registry) {
        String groupId = injection.getGroupId();
        if (groupId == null) return;

        // Find and delete all injections with the same groupId
        List<PersistentInjection> groupInjections = repository.findByGroupId(groupId);
        for (PersistentInjection groupInjection : groupInjections) {
            if (!groupInjection.getId().equals(injection.getId())) {
                repository.delete(groupInjection.getId());
                registry.unregister(groupInjection.getId());
            }
        }
    }

    private int resolveMaxDepth(PersistentInjection injection) {
        try {
            String code = injection.getCode();
            if (code != null && !code.isEmpty()) {
                return Integer.parseInt(code.trim());
            }
        } catch (NumberFormatException ignored) {}
        return 5; // default
    }
}
