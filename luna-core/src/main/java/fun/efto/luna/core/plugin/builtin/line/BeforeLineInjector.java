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

import java.util.List;
import java.util.Optional;

/**
 * @author : Tony.L(286269159@qq.com)
 * @since : 2025/10/4 18:00
 */
public class BeforeLineInjector implements BytecodeInjector {
    private static final Logger LOGGER = LoggerFactory.getLogger(BeforeLineInjector.class);

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

        // BeforeLine 策略：注入点 = LineNumberNode 位置
        int queryPos = LineOffsetResolver.resolveBeforeLine(mn, target.getLineNumber());
        if (queryPos < 0) {
            throw new RuntimeException("line " + target.getLineNumber() + " not found in method " + target.getMethodName());
        }

        // 一次遍历获取完整结果
        LocalVariableScanner.ScanResult scanResult = LocalVariableScanner.scanAtPosition(mn, queryPos);
        List<AsmInjectionContext.LocalVarInfo> allVars = scanResult.getAllVars();
        List<AsmInjectionContext.LocalVarInfo> excludedVars = scanResult.getSameLineStartVars();

        asmContext.setLocalVariables(allVars);
        asmContext.setExcludedSameLineVariables(excludedVars);

        asmContext.setMethodAccess(mn.access);
        asmContext.setMaxLocals(mn.maxLocals);

        LOGGER.debug("[BeforeLine] method={}{} maxLocals={} allVars={} safeVars={} excludedSameLine={}",
                mn.name, mn.desc, mn.maxLocals, allVars.size(), scanResult.getSafeVars().size(), excludedVars.size());

        // 找到 LineNumberNode 用于插入
        AbstractInsnNode insn = mn.instructions.getFirst();
        for (int i = 0; i < queryPos; i++) {
            insn = insn.getNext();
        }
        if (!(insn instanceof LineNumberNode)) {
            throw new RuntimeException("Expected LineNumberNode at offset " + queryPos + " in method " + target.getMethodName());
        }
        LineNumberNode lnn = (LineNumberNode) insn;
        InsnList injectedCode = TreeApiBytecodeHelper.assemble(asmContext, compiledCode, probeHandler, bytecode);

        int maxVarInCode = LineInjectorHelper.computeMaxLocalIndex(asmContext, injectedCode);
        int neededLocals = Math.max(mn.maxLocals, maxVarInCode + 1);
        if (neededLocals > mn.maxLocals) {
            LOGGER.debug("[BeforeLine] Updating maxLocals from {} to {}", mn.maxLocals, neededLocals);
            mn.maxLocals = neededLocals;
        }

        mn.instructions.insertBefore(lnn, injectedCode);

        LineInjectorHelper.removeFrameNodes(cn);

        ClassLoader loader = Thread.currentThread().getContextClassLoader();
        ClassWriter cw = new ClassLoaderAwareClassWriter(ClassWriter.COMPUTE_MAXS | ClassWriter.COMPUTE_FRAMES, loader);
        cn.accept(cw);
        return cw.toByteArray();
    }
}
