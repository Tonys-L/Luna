package fun.efto.luna.core.asm;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.LabelNode;
import org.objectweb.asm.tree.LineNumberNode;
import org.objectweb.asm.tree.LocalVariableNode;
import org.objectweb.asm.tree.MethodNode;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * @author : Tony.L(<286269159@qq.com>)
 * @since  : 2026/05/03 19:10
 */
public final class LocalVariableScanner {

    private LocalVariableScanner() {
    }

    public static List<AsmInjectionContext.LocalVarInfo> scanVisibleLocalVariables(
            byte[] bytecode, String methodName, String methodDescriptor, int lineNumber) {
        if (bytecode == null || methodName == null || lineNumber < 1) {
            return Collections.emptyList();
        }

        ClassNode cn = new ClassNode();
        new ClassReader(bytecode).accept(cn, ClassReader.EXPAND_FRAMES);

        for (MethodNode mn : cn.methods) {
            if (!mn.name.equals(methodName)) continue;
            if (methodDescriptor != null && !methodDescriptor.isEmpty() && !mn.desc.equals(methodDescriptor)) continue;

            return scanMethod(mn, lineNumber);
        }

        return Collections.emptyList();
    }

    private static List<AsmInjectionContext.LocalVarInfo> scanMethod(MethodNode mn, int lineNumber) {
        Map<LabelNode, Integer> labelLines = new LinkedHashMap<>();

        AbstractInsnNode insn = mn.instructions.getFirst();
        while (insn != null) {
            if (insn instanceof LineNumberNode) {
                LineNumberNode lnn = (LineNumberNode) insn;
                labelLines.put(lnn.start, lnn.line);
            }
            insn = insn.getNext();
        }

        List<AsmInjectionContext.LocalVarInfo> result = new ArrayList<>();

        if (mn.localVariables == null) {
            return result;
        }

        for (LocalVariableNode lv : mn.localVariables) {
            if (!(lv.start instanceof LabelNode)) continue;

            LabelNode startLabel = (LabelNode) lv.start;
            LabelNode endLabel = (LabelNode) lv.end;

            if (isVariableVisibleAtLine(labelLines, startLabel, endLabel, lineNumber)) {
                result.add(new AsmInjectionContext.LocalVarInfo(lv.name, lv.desc, lv.index));
            }
        }

        return result;
    }

    private static boolean isVariableVisibleAtLine(Map<LabelNode, Integer> labelLines,
                                                    LabelNode start, LabelNode end, int line) {
        Integer startLine = labelLines.get(start);
        Integer endLine = labelLines.get(end);

        if (startLine == null) {
            return true;
        }
        if (line < startLine) {
            return false;
        }
        if (endLine == null || endLine <= startLine) {
            return true;
        }
        return line <= endLine;
    }
}
