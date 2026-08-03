package fun.efto.luna.core.plugin.builtin.invocation.analysis;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Handle;

import fun.efto.luna.core.plugin.PluginContext;

import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * @author : Tony.L(286269159@qq.com)
 * @since : 2026/07/05 20:00
 */
public class StaticCallGraphAnalyzer {

    private static final Logger LOGGER = Logger.getLogger(StaticCallGraphAnalyzer.class.getName());

    private final PluginContext ctx;
    private final int maxDepth;
    private final CallGraphFilter filter;

    public StaticCallGraphAnalyzer(PluginContext ctx, int maxDepth) {
        this.ctx = ctx;
        this.maxDepth = maxDepth;
        this.filter = new CallGraphFilter();
    }

    public StaticCallGraphAnalyzer(PluginContext ctx) {
        this(ctx, 5); // default maxDepth = 5
    }

    /**
     * Analyze the call graph starting from the given method.
     */
    public CallGraph analyze(String className, String methodName, String methodDesc) {
        CallGraph root = new CallGraph(className, methodName, methodDesc);
        Set<String> visited = new HashSet<>();
        visited.add(key(className, methodName, methodDesc));
        analyzeRecursive(root, 0, visited);
        return root;
    }

    private void analyzeRecursive(CallGraph node, int depth, Set<String> visited) {
        if (depth >= maxDepth) return;

        byte[] classBytes = ctx.getClassBytes(node.getClassName());
        if (classBytes == null) {
            LOGGER.warning("analyzeRecursive: classBytes is null for " + node.getClassName());
            return;
        }

        List<MethodCall> calls = scanMethodCalls(classBytes, node.getMethodName(), node.getMethodDesc());
        LOGGER.fine("analyzeRecursive: " + node.getClassName() + "." + node.getMethodName()
            + " -> found " + calls.size() + " method calls");

        for (MethodCall call : calls) {
            // Skip constructors and static initializers
            if ("<init>".equals(call.name) || "<clinit>".equals(call.name)) continue;

            String callKey = key(call.owner, call.name, call.desc);
            if (visited.contains(callKey)) continue;
            if (!filter.shouldTrace(call.owner)) continue;

            visited.add(callKey);
            CallGraph callee = new CallGraph(call.owner, call.name, call.desc);
            node.addCallee(callee);

            // Handle interface/abstract method - find implementations
            if (isInterfaceOrAbstract(call.owner)) {
                findImplementations(call.owner, call.name, call.desc, callee, depth, visited);
            } else {
                analyzeRecursive(callee, depth + 1, visited);
            }
        }
    }

    private boolean isInterfaceOrAbstract(String className) {
        byte[] bytes = ctx.getClassBytes(className);
        if (bytes == null) return false;
        try {
            boolean[] result = new boolean[1];
            new ClassReader(bytes).accept(new ClassVisitor(Opcodes.ASM9) {
                @Override
                public void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
                    result[0] = (access & Opcodes.ACC_INTERFACE) != 0 || (access & Opcodes.ACC_ABSTRACT) != 0;
                }
            }, ClassReader.SKIP_CODE | ClassReader.SKIP_DEBUG | ClassReader.SKIP_FRAMES);
            return result[0];
        } catch (Exception e) {
            return false;
        }
    }

    private void findImplementations(String interfaceName, String methodName, String methodDesc,
                                      CallGraph calleeNode, int depth, Set<String> visited) {
        Set<String> loadedClasses = ctx.getLoadedClassNames();
        for (String className : loadedClasses) {
            if (!filter.shouldTrace(className)) continue;
            if (visited.contains(key(className, methodName, methodDesc))) continue;

            byte[] bytes = ctx.getClassBytes(className);
            if (bytes == null) continue;

            try {
                boolean[] isImpl = new boolean[1];
                new ClassReader(bytes).accept(new ClassVisitor(Opcodes.ASM9) {
                    String superName;
                    String[] interfaces;
                    @Override
                    public void visit(int version, int access, String name, String signature, String sName, String[] ifaces) {
                        this.superName = sName;
                        this.interfaces = ifaces;
                    }
                    @Override
                    public void visitEnd() {
                        // Check if this class implements the interface or extends the abstract class
                        if (interfaceName.equals(superName)) {
                            isImpl[0] = true;
                            return;
                        }
                        if (interfaces != null) {
                            for (String iface : interfaces) {
                                if (interfaceName.equals(iface)) {
                                    isImpl[0] = true;
                                    return;
                                }
                            }
                        }
                    }
                }, ClassReader.SKIP_CODE | ClassReader.SKIP_DEBUG | ClassReader.SKIP_FRAMES);

                if (isImpl[0]) {
                    visited.add(key(className, methodName, methodDesc));
                    CallGraph impl = new CallGraph(className, methodName, methodDesc);
                    calleeNode.addCallee(impl);
                    analyzeRecursive(impl, depth + 1, visited);
                }
            } catch (Exception ignored) {}
        }
    }

    /**
     * Scan method calls within a specific method using ASM.
     */
    private List<MethodCall> scanMethodCalls(byte[] classBytes, String targetMethodName, String targetMethodDesc) {
        List<MethodCall> calls = new ArrayList<>();
        try {
            ClassReader reader = new ClassReader(classBytes);
            reader.accept(new ClassVisitor(Opcodes.ASM9) {
                @Override
                public MethodVisitor visitMethod(int access, String name, String descriptor,
                                                   String signature, String[] exceptions) {
                    boolean matches = name.equals(targetMethodName)
                        && (targetMethodDesc == null || descriptor.equals(targetMethodDesc));
                    if (matches) {
                        return new MethodVisitor(Opcodes.ASM9) {
                            @Override
                            public void visitMethodInsn(int opcode, String owner, String name, String descriptor, boolean isInterface) {
                                calls.add(new MethodCall(owner.replace('/', '.'), name, descriptor));
                            }

                            @Override
                            public void visitInvokeDynamicInsn(String name, String descriptor, Handle bootstrapMethodHandle, Object... bootstrapMethodArguments) {
                                for (Object arg : bootstrapMethodArguments) {
                                    if (arg instanceof Handle) {
                                        Handle h = (Handle) arg;
                                        calls.add(new MethodCall(h.getOwner().replace('/', '.'), h.getName(), h.getDesc()));
                                    }
                                }
                            }
                        };
                    }
                    return null;
                }
            }, ClassReader.SKIP_DEBUG | ClassReader.SKIP_FRAMES);
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "scanMethodCalls: failed for " + targetMethodName + targetMethodDesc, e);
        }
        return calls;
    }

    private String key(String className, String methodName, String methodDesc) {
        return className + "." + methodName + (methodDesc != null ? methodDesc : "");
    }

    /**
     * Internal representation of a method call found in bytecode.
     */
    private static class MethodCall {
        final String owner;
        final String name;
        final String desc;

        MethodCall(String owner, String name, String desc) {
            this.owner = owner;
            this.name = name;
            this.desc = desc;
        }
    }
}
