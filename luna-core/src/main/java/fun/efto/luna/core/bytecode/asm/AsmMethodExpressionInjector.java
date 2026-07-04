package fun.efto.luna.core.bytecode.asm;

import fun.efto.luna.core.bytecode.asm.analyzer.BytecodeAnalyzer;
import fun.efto.luna.core.bytecode.asm.injector.BytecodeInjector;
import fun.efto.luna.core.bytecode.asm.injector.TreeApiBytecodeHelper;
import fun.efto.luna.core.injection.InjectionContext;
import fun.efto.luna.core.injection.code.CompiledCode;
import fun.efto.luna.core.injection.target.MethodTarget;
import fun.efto.luna.core.plugin.GenerateContext;
import fun.efto.luna.core.plugin.ProbeHandler;
import fun.efto.luna.core.plugin.builtin.line.LineInjectorHelper;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.InsnNode;
import org.objectweb.asm.tree.LocalVariableNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.VarInsnNode;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * @author : Tony.L(286269159@qq.com)
 * @since : 2026/06/01 00:20
 */
public class AsmMethodExpressionInjector implements BytecodeInjector {

    public enum Phase {
        ENTER, EXIT, AROUND
    }

    private final Phase phase;

    public AsmMethodExpressionInjector(Phase phase) {
        this.phase = phase;
    }

    @Override
    public byte[] inject(CompiledCode compiledCode, ProbeHandler probeHandler, InjectionContext injectionContext, byte[] bytecode) {
        AsmInjectionContext asmContext = new AsmInjectionContext(injectionContext, bytecode);

        MethodTarget target = (MethodTarget) asmContext.getInjectionTarget();
        String targetMethodName = target.getMethodName();
        String targetMethodDesc = target.getMethodDescriptor();

        BytecodeAnalyzer analyzer = BytecodeAnalyzer.parse(bytecode);
        ClassNode cn = analyzer.getClassNode();

        // 扫描方法参数/局部变量
        List<AsmInjectionContext.LocalVarInfo> vars = scanMethodVariables(analyzer, targetMethodName, targetMethodDesc);
        asmContext.setLocalVariables(vars);

        Optional<MethodNode> methodOpt = analyzer.findMethod(targetMethodName, targetMethodDesc);
        if (!methodOpt.isPresent()) {
            throw new RuntimeException("method " + targetMethodName + targetMethodDesc + " not found");
        }

        MethodNode mn = methodOpt.get();
        asmContext.setMethodAccess(mn.access);
        asmContext.setMaxLocals(mn.maxLocals);

        if (phase == Phase.ENTER || phase == Phase.AROUND) {
            InsnList code = TreeApiBytecodeHelper.assemble(asmContext, compiledCode, probeHandler, bytecode, GenerateContext.Phase.ENTER);
            updateMaxLocals(mn, code, asmContext);
            mn.instructions.insert(code);
        }
        if (phase == Phase.EXIT || phase == Phase.AROUND) {
            InsnList code = TreeApiBytecodeHelper.assemble(asmContext, compiledCode, probeHandler, bytecode, GenerateContext.Phase.EXIT);
            updateMaxLocals(mn, code, asmContext);
            insertBeforeReturns(mn, code);
        }

        LineInjectorHelper.removeFrameNodes(cn);

        ClassLoader loader = Thread.currentThread().getContextClassLoader();
        ClassWriter cw = new ClassLoaderAwareClassWriter(ClassWriter.COMPUTE_MAXS | ClassWriter.COMPUTE_FRAMES, loader);
        cn.accept(cw);
        return cw.toByteArray();
    }

    /**
     * 使用 BytecodeAnalyzer + LocalVariableScanner 扫描方法变量。
     * ENTER 阶段只返回参数，EXIT/AROUND 阶段返回所有可见变量。
     */
    private List<AsmInjectionContext.LocalVarInfo> scanMethodVariables(BytecodeAnalyzer analyzer, String methodName, String methodDesc) {
        Optional<MethodNode> methodOpt = analyzer.findMethod(methodName, methodDesc);
        if (!methodOpt.isPresent()) return new ArrayList<>();

        MethodNode mn = methodOpt.get();
        if (mn.localVariables == null || mn.localVariables.isEmpty()) return new ArrayList<>();

        if (phase == Phase.ENTER) {
            // ENTER 阶段：只返回方法参数
            return scanParametersOnly(mn);
        } else {
            // EXIT/AROUND 阶段：返回所有局部变量（方法入口处全部可见）
            List<AsmInjectionContext.LocalVarInfo> result = new ArrayList<>();
            for (LocalVariableNode lv : mn.localVariables) {
                if (lv.name != null && !lv.name.startsWith("$")) {
                    result.add(new AsmInjectionContext.LocalVarInfo(lv.name, lv.desc, lv.index));
                }
            }
            return result;
        }
    }

    /**
     * 只扫描方法参数。
     * 使用 computeParamSlotEnd 计算参数 slot 范围。
     */
    private List<AsmInjectionContext.LocalVarInfo> scanParametersOnly(MethodNode mn) {
        List<AsmInjectionContext.LocalVarInfo> result = new ArrayList<>();
        int paramSlotEnd = computeParamSlotEnd(mn);
        for (LocalVariableNode lv : mn.localVariables) {
            if (lv.index < paramSlotEnd && lv.name != null && !lv.name.startsWith("$")) {
                result.add(new AsmInjectionContext.LocalVarInfo(lv.name, lv.desc, lv.index));
            }
        }
        return result;
    }

    private int computeParamSlotEnd(MethodNode mn) {
        boolean isStatic = (mn.access & Opcodes.ACC_STATIC) != 0;
        int slot = isStatic ? 0 : 1;
        for (Type argType : Type.getArgumentTypes(mn.desc)) {
            slot += argType.getSize();
        }
        return slot;
    }

    private void insertBeforeReturns(MethodNode mn, InsnList code) {
        List<AbstractInsnNode> returnInsns = new ArrayList<>();
        for (AbstractInsnNode insn = mn.instructions.getFirst(); insn != null; insn = insn.getNext()) {
            if (LineInjectorHelper.isReturnOrThrow(insn.getOpcode())) {
                returnInsns.add(insn);
            }
        }
        for (AbstractInsnNode ret : returnInsns) {
            InsnList copy = cloneInsnList(code);
            mn.instructions.insertBefore(ret, copy);
        }
    }

    private InsnList cloneInsnList(InsnList original) {
        InsnList copy = new InsnList();
        for (AbstractInsnNode insn = original.getFirst(); insn != null; insn = insn.getNext()) {
            copy.add(insn.clone(null));
        }
        return copy;
    }

    private void updateMaxLocals(MethodNode mn, InsnList code, AsmInjectionContext asmContext) {
        int maxVarInCode = LineInjectorHelper.computeMaxLocalIndex(asmContext, code);
        if (maxVarInCode > mn.maxLocals) {
            mn.maxLocals = maxVarInCode;
        }
    }
}
