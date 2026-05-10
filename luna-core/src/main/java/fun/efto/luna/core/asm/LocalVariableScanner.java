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
        return scanVisibleLocalVariables(bytecode, methodName, methodDescriptor, lineNumber, false);
    }

    public static List<AsmInjectionContext.LocalVarInfo> scanVisibleLocalVariables(
            byte[] bytecode, String methodName, String methodDescriptor, int lineNumber, boolean excludeSameLineStart) {
        if (bytecode == null || methodName == null || lineNumber < 1) {
            return Collections.emptyList();
        }

        ClassNode cn = new ClassNode();
        new ClassReader(bytecode).accept(cn, ClassReader.EXPAND_FRAMES);

        for (MethodNode mn : cn.methods) {
            if (!mn.name.equals(methodName)) continue;
            if (methodDescriptor != null && !methodDescriptor.isEmpty() && !mn.desc.equals(methodDescriptor)) continue;

            return scanMethod(mn, lineNumber, excludeSameLineStart);
        }

        return Collections.emptyList();
    }

    private static List<AsmInjectionContext.LocalVarInfo> scanMethod(MethodNode mn, int lineNumber, boolean excludeSameLineStart) {
        Map<LabelNode, Integer> labelLines = buildLabelLineMap(mn);

        List<AsmInjectionContext.LocalVarInfo> result = new ArrayList<>();

        if (mn.localVariables == null) {
            return result;
        }

        for (LocalVariableNode lv : mn.localVariables) {
            if (lv.start == null || lv.end == null) continue;

            LabelNode startLabel = (lv.start instanceof LabelNode) ? (LabelNode) lv.start : null;
            LabelNode endLabel = (lv.end instanceof LabelNode) ? (LabelNode) lv.end : null;

            if (startLabel == null) continue;

            if (isVariableVisibleAtLine(mn, labelLines, startLabel, endLabel, lineNumber, excludeSameLineStart)) {
                result.add(new AsmInjectionContext.LocalVarInfo(lv.name, lv.desc, lv.index));
            }
        }

        return result;
    }

    private static Map<LabelNode, Integer> buildLabelLineMap(MethodNode mn) {
        Map<LabelNode, Integer> labelLines = new LinkedHashMap<>();
        AbstractInsnNode insn = mn.instructions.getFirst();
        while (insn != null) {
            if (insn instanceof LineNumberNode) {
                LineNumberNode lnn = (LineNumberNode) insn;
                labelLines.put(lnn.start, lnn.line);
            }
            insn = insn.getNext();
        }
        return labelLines;
    }

    private static boolean isVariableVisibleAtLine(MethodNode mn, Map<LabelNode, Integer> labelLines,
                                                    LabelNode start, LabelNode end, int line,
                                                    boolean excludeSameLineStart) {
        Integer startLine = labelLines.get(start);
        Integer endLine = (end != null) ? labelLines.get(end) : null;

        if (startLine == null) {
            startLine = findNearestPrecedingLine(mn, labelLines, start);
        }

        if (startLine == null) {
            return true;
        }
        if (line < startLine) {
            return false;
        }
        if (excludeSameLineStart && line == startLine) {
            return false;
        }
        if (endLine == null || endLine <= startLine) {
            return true;
        }
        return line <= endLine;
    }

    private static Integer findNearestPrecedingLine(MethodNode mn, Map<LabelNode, Integer> labelLines, LabelNode target) {
        int targetPos = getPosition(mn, target);
        if (targetPos < 0) return null;

        Integer nearestLine = null;
        int nearestPos = -1;

        for (Map.Entry<LabelNode, Integer> entry : labelLines.entrySet()) {
            int pos = getPosition(mn, entry.getKey());
            if (pos >= 0 && pos <= targetPos && pos > nearestPos) {
                nearestPos = pos;
                nearestLine = entry.getValue();
            }
        }

        return nearestLine;
    }

    private static int getPosition(MethodNode mn, LabelNode label) {
        int pos = 0;
        for (AbstractInsnNode insn = mn.instructions.getFirst(); insn != null; insn = insn.getNext()) {
            if (insn == label) return pos;
            pos++;
        }
        return -1;
    }
}
