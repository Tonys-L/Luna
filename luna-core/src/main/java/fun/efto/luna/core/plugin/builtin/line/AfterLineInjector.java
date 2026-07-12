package fun.efto.luna.core.plugin.builtin.line;

import fun.efto.luna.core.injection.InjectionContext;
import fun.efto.luna.core.injection.code.CompiledCode;
import fun.efto.luna.core.bytecode.asm.AsmInjectionContext;
import fun.efto.luna.core.bytecode.asm.ClassLoaderAwareClassWriter;
import fun.efto.luna.core.bytecode.asm.analyzer.BytecodeAnalyzer;
import fun.efto.luna.core.bytecode.asm.analyzer.LineOffsetResolver;
import fun.efto.luna.core.bytecode.asm.analyzer.LocalVariableScanner;
import fun.efto.luna.core.injection.target.LineNumberTarget;
import fun.efto.luna.core.bytecode.asm.injector.BytecodeInjector;
import fun.efto.luna.core.bytecode.asm.injector.TreeApiBytecodeHelper;
import fun.efto.luna.core.plugin.ProbeHandler;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.LineNumberNode;
import org.objectweb.asm.tree.MethodNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Optional;

/**
 * @author : Tony.L(286269159@qq.com)
 * @since : 2025/10/4 18:00
 */
public class AfterLineInjector implements BytecodeInjector {
    private static final Logger LOGGER = LoggerFactory.getLogger(AfterLineInjector.class);

    @Override
    public byte[] inject(CompiledCode compiledCode, ProbeHandler probeHandler, InjectionContext injectionContext, byte[] bytecode) {
        AsmInjectionContext asmContext = new AsmInjectionContext(injectionContext, bytecode);

        LineNumberTarget target = (LineNumberTarget) asmContext.getInjectionTarget();

        BytecodeAnalyzer analyzer = BytecodeAnalyzer.parse(bytecode);
        ClassNode cn = analyzer.getClassNode();

        Optional<MethodNode> methodOpt = analyzer.findMethod(target.getMethodName(), target.getMethodDescriptor());
        if (!methodOpt.isPresent()) {
            throw new RuntimeException("line " + target.getLineNumber() + " not found in method " + target.getMethodName());
        }

        MethodNode mn = methodOpt.get();

        if (!LineInjectorHelper.hasLineNumberTable(mn)) {
            throw new RuntimeException("类缺少调试信息(LineNumberTable)，请使用 -g 或 -g:lines 编译。"
                    + "方法: " + mn.name + mn.desc);
        }

        // AfterLine 策略：注入点 = 该行最后一条指令之后的位置
        int queryPos = LineOffsetResolver.resolveAfterLine(mn, target.getLineNumber());
        if (queryPos < 0) {
            throw new RuntimeException("line " + target.getLineNumber() + " not found in method " + target.getMethodName());
        }

        // 一次遍历获取完整结果
        LocalVariableScanner.ScanResult scanResult = LocalVariableScanner.scanAtPosition(mn, queryPos);
        asmContext.setLocalVariables(scanResult.getAllVars());

        asmContext.setMethodAccess(mn.access);
        asmContext.setMaxLocals(mn.maxLocals);

        LOGGER.debug("[AfterLine] method={} maxLocals={} visibleVars={} queryPos={}", mn.name, mn.maxLocals, scanResult.getAllVars().size(), queryPos);

        InsnList injectedCode = TreeApiBytecodeHelper.assemble(asmContext, compiledCode, probeHandler, bytecode);

        int maxVarInCode = LineInjectorHelper.computeMaxLocalIndex(asmContext, injectedCode);
        int neededLocals = Math.max(mn.maxLocals, maxVarInCode + 1);
        if (neededLocals > mn.maxLocals) {
            LOGGER.debug("[AfterLine] Updating maxLocals from {} to {}", mn.maxLocals, neededLocals);
            mn.maxLocals = neededLocals;
        }

        // 找到 LineNumberNode 和该行最后一条指令，用于确定注入位置
        int lineOffset = LineOffsetResolver.resolveBeforeLine(mn, target.getLineNumber());
        AbstractInsnNode insn = mn.instructions.getFirst();
        for (int i = 0; i < lineOffset; i++) {
            insn = insn.getNext();
        }
        LineNumberNode lnn = (LineNumberNode) insn;
        AbstractInsnNode lastInsn = LineOffsetResolver.findLastInsnOfLine(lnn);

        if (lastInsn != null && LineInjectorHelper.isReturnOrThrow(lastInsn.getOpcode())) {
            mn.instructions.insertBefore(lastInsn, injectedCode);
            LOGGER.debug("[AfterLine] Inserted BEFORE return/throw at line {}", target.getLineNumber());
        } else if (lastInsn != null) {
            mn.instructions.insert(lastInsn, injectedCode);
            LOGGER.debug("[AfterLine] Inserted AFTER last instruction at line {}", target.getLineNumber());
        } else {
            mn.instructions.insertBefore(lnn, injectedCode);
            LOGGER.debug("[AfterLine] Inserted before LineNumberNode at line {}", target.getLineNumber());
        }

        LineInjectorHelper.removeFrameNodes(cn);

        ClassLoader loader = Thread.currentThread().getContextClassLoader();
        ClassWriter cw = new ClassLoaderAwareClassWriter(ClassWriter.COMPUTE_MAXS | ClassWriter.COMPUTE_FRAMES, loader);
        cn.accept(cw);
        return cw.toByteArray();
    }
}
