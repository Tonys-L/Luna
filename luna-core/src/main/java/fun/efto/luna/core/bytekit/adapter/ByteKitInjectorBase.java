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
import fun.efto.luna.core.asm.injector.BytecodeInjector;
import fun.efto.luna.core.bytecode.BytecodeAssembler;
import fun.efto.luna.core.injection.InjectionContext;

import java.util.List;

public abstract class ByteKitInjectorBase implements BytecodeInjector {

    @Override
    public byte[] inject(InjectionContext injectionContext, byte[] bytecode, BytecodeAssembler bytecodeAssembler) {
        AsmInjectionContext asmContext = new AsmInjectionContext(injectionContext, bytecode);

        String targetMethodName = injectionContext.getInjectionTarget().getMethodName();
        String targetMethodDesc = injectionContext.getInjectionTarget().getMethodDescriptor();

        ClassNode classNode = new ClassNode(Opcodes.ASM9);
        new ClassReader(bytecode).accept(classNode, ClassReader.SKIP_FRAMES);

        for (MethodNode methodNode : classNode.methods) {
            if (shouldProcessMethod(methodNode, targetMethodName, targetMethodDesc)) {
                MethodProcessor methodProcessor = new MethodProcessor(classNode, methodNode);
                List<InterceptorProcessor> processors = createInterceptorProcessors(methodProcessor, asmContext);
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
        ByteKitClassLoaderAwareClassWriter cw = new ByteKitClassLoaderAwareClassWriter(ClassWriter.COMPUTE_MAXS | ClassWriter.COMPUTE_FRAMES, loader);
        classNode.accept(cw);
        return cw.toByteArray();
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
