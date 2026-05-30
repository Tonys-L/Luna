/**
 * @author : Tony.L(<286269159@qq.com>)
 * @since  : 2026/05/31 22:00
 */
package fun.efto.luna.core.bytekit.adapter;

import com.alibaba.bytekit.asm.MethodProcessor;
import com.alibaba.bytekit.asm.interceptor.InterceptorProcessor;
import com.alibaba.deps.org.objectweb.asm.ClassReader;
import com.alibaba.deps.org.objectweb.asm.ClassWriter;
import com.alibaba.deps.org.objectweb.asm.Opcodes;
import com.alibaba.deps.org.objectweb.asm.tree.ClassNode;
import com.alibaba.deps.org.objectweb.asm.tree.MethodNode;
import fun.efto.luna.core.asm.AsmInjectionContext;
import fun.efto.luna.core.asm.assembler.ExpressionBytecodeAssembler;
import fun.efto.luna.core.asm.injector.BytecodeInjector;
import fun.efto.luna.core.bytecode.BytecodeAssembler;
import fun.efto.luna.core.injection.InjectionContext;

import java.util.List;

public abstract class ByteKitInjectorBase implements BytecodeInjector {

    private volatile List<InterceptorProcessor> cachedProcessors;
    private volatile InjectionCacheEntry injectionCache;

    private static class InjectionCacheEntry {
        final byte[] inputBytecode;
        final byte[] outputBytecode;
        final String methodName;
        final String methodDesc;

        InjectionCacheEntry(byte[] inputBytecode, byte[] outputBytecode, String methodName, String methodDesc) {
            this.inputBytecode = inputBytecode;
            this.outputBytecode = outputBytecode;
            this.methodName = methodName;
            this.methodDesc = methodDesc;
        }

        boolean matches(byte[] bytecode, String name, String desc) {
            return inputBytecode == bytecode
                    && methodName.equals(name)
                    && methodDesc.equals(desc);
        }
    }

    @Override
    public byte[] inject(InjectionContext injectionContext, byte[] bytecode, BytecodeAssembler bytecodeAssembler) {
        if (bytecodeAssembler instanceof ExpressionBytecodeAssembler) {
            return injectWithExpression(injectionContext, bytecode, bytecodeAssembler);
        }
        return injectWithByteKit(injectionContext, bytecode);
    }

    private byte[] injectWithExpression(InjectionContext injectionContext, byte[] bytecode, BytecodeAssembler bytecodeAssembler) {
        AsmMethodExpressionInjector asmInjector = new AsmMethodExpressionInjector(getExpressionPhase());
        return asmInjector.inject(injectionContext, bytecode, bytecodeAssembler);
    }

    protected AsmMethodExpressionInjector.Phase getExpressionPhase() {
        return AsmMethodExpressionInjector.Phase.ENTER;
    }

    private byte[] injectWithByteKit(InjectionContext injectionContext, byte[] bytecode) {
        String targetMethodName = injectionContext.getInjectionTarget().getMethodName();
        String targetMethodDesc = injectionContext.getInjectionTarget().getMethodDescriptor();

        InjectionCacheEntry entry = injectionCache;
        if (entry != null && entry.matches(bytecode, targetMethodName, targetMethodDesc)) {
            return entry.outputBytecode;
        }

        AsmInjectionContext asmContext = new AsmInjectionContext(injectionContext, bytecode);

        ClassNode classNode = new ClassNode(Opcodes.ASM9);
        ClassReader classReader = new ClassReader(bytecode);
        classReader.accept(classNode, ClassReader.SKIP_FRAMES);

        for (MethodNode methodNode : classNode.methods) {
            if (shouldProcessMethod(methodNode, targetMethodName, targetMethodDesc)) {
                MethodProcessor methodProcessor = new MethodProcessor(classNode, methodNode);
                List<InterceptorProcessor> processors = resolveProcessors(methodProcessor, asmContext);
                for (InterceptorProcessor processor : processors) {
                    try {
                        processor.process(methodProcessor);
                    } catch (Exception e) {
                        throw new RuntimeException("ByteKit processing failed for method " + methodNode.name, e);
                    }
                }
            }
        }

        ClassLoader loader = Thread.currentThread().getContextClassLoader();
        ByteKitClassLoaderAwareClassWriter cw = new ByteKitClassLoaderAwareClassWriter(classReader, ClassWriter.COMPUTE_MAXS | ClassWriter.COMPUTE_FRAMES, loader);
        classNode.accept(cw);
        byte[] result = cw.toByteArray();

        injectionCache = new InjectionCacheEntry(bytecode, result, targetMethodName, targetMethodDesc);

        return result;
    }

    private List<InterceptorProcessor> resolveProcessors(MethodProcessor methodProcessor, AsmInjectionContext context) {
        List<InterceptorProcessor> processors = cachedProcessors;
        if (processors == null) {
            synchronized (this) {
                processors = cachedProcessors;
                if (processors == null) {
                    processors = createInterceptorProcessors(methodProcessor, context);
                    cachedProcessors = processors;
                }
            }
        }
        return processors;
    }

    protected boolean shouldProcessMethod(MethodNode methodNode, String targetMethodName, String targetMethodDesc) {
        if (!methodNode.name.equals(targetMethodName)) {
            return false;
        }
        if (targetMethodDesc != null && !targetMethodDesc.isEmpty() && !methodNode.desc.equals(targetMethodDesc)) {
            return false;
        }
        return true;
    }

    protected abstract List<InterceptorProcessor> createInterceptorProcessors(MethodProcessor methodProcessor, AsmInjectionContext context);
}
