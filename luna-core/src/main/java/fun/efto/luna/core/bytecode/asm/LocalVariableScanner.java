package fun.efto.luna.core.bytecode.asm;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.LabelNode;
import org.objectweb.asm.tree.LineNumberNode;
import org.objectweb.asm.tree.LocalVariableNode;
import org.objectweb.asm.tree.MethodNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;

/**
 * @author ：Tony.L(286269159@qq.com)
 * @since  : 2026/05/03 19:10
 */
public final class LocalVariableScanner {

    private static final Logger LOGGER = LoggerFactory.getLogger(LocalVariableScanner.class);

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

        LOGGER.warn("[LocalVarScanner] No method found: {} desc={} for line {}", methodName, methodDescriptor, lineNumber);
        return Collections.emptyList();
    }

    private static List<AsmInjectionContext.LocalVarInfo> scanMethod(MethodNode mn, int lineNumber, boolean excludeSameLineStart) {
        List<AsmInjectionContext.LocalVarInfo> result = new ArrayList<>();

        if (mn.localVariables == null || mn.localVariables.isEmpty()) {
            LOGGER.warn("[LocalVarScanner] method={} has no local variable table (compiled without -g?)", mn.name);
            return result;
        }

        Map<LabelNode, Integer> labelPosMap = new IdentityHashMap<>();
        List<int[]> lineNodePositions = new ArrayList<>();
        int pos = 0;
        for (AbstractInsnNode insn = mn.instructions.getFirst(); insn != null; insn = insn.getNext()) {
            if (insn instanceof LabelNode) {
                labelPosMap.put((LabelNode) insn, pos);
            } else if (insn instanceof LineNumberNode) {
                lineNodePositions.add(new int[]{pos, ((LineNumberNode) insn).line});
            }
            pos++;
        }

        LOGGER.debug("[LocalVarScanner] method={} line={} excludeSameLineStart={} totalVars={} lineNodeCount={}",
                mn.name, lineNumber, excludeSameLineStart, mn.localVariables.size(), lineNodePositions.size());

        for (LocalVariableNode lv : mn.localVariables) {
            if (lv.start == null || lv.end == null) continue;

            int startLine = resolveStartLine(labelPosMap, lineNodePositions, lv.start);
            int endLine = resolveEndLine(labelPosMap, lineNodePositions, lv.end);
            int startPos = labelPosMap.containsKey(lv.start) ? labelPosMap.get(lv.start) : -1;

            boolean visible = checkVisibility(lineNumber, startPos, startLine, endLine, excludeSameLineStart);

            LOGGER.debug("[LocalVarScanner]   var={} slot={} startLine={} endLine={} queryLine={} visible={}",
                    lv.name, lv.index, startLine, endLine, lineNumber, visible);

            if (visible) {
                result.add(new AsmInjectionContext.LocalVarInfo(lv.name, lv.desc, lv.index));
            }
        }

        LOGGER.debug("[LocalVarScanner] method={} line={} visibleVars={}", mn.name, lineNumber, result.size());
        return result;
    }

    private static int resolveStartLine(Map<LabelNode, Integer> labelPosMap,
                                         List<int[]> lineNodePositions,
                                         LabelNode label) {
        Integer labelPos = labelPosMap.get(label);
        if (labelPos == null) {
            return -1;
        }
        return findLineAtOrBefore(lineNodePositions, labelPos);
    }

    private static int resolveEndLine(Map<LabelNode, Integer> labelPosMap,
                                       List<int[]> lineNodePositions,
                                       LabelNode label) {
        Integer labelPos = labelPosMap.get(label);
        if (labelPos == null) {
            return -1;
        }
        return findLineAtOrAfter(lineNodePositions, labelPos);
    }

    private static boolean checkVisibility(int queryLine, int startPos, int startLine, int endLine, boolean excludeSameLineStart) {
        if (startPos < 0) {
            return true;
        }
        if (startLine < 0) {
            return true;
        }
        if (queryLine < startLine) {
            return false;
        }
        if (excludeSameLineStart && queryLine == startLine) {
            return false;
        }
        if (endLine < 0 || endLine <= startLine) {
            return true;
        }
        return queryLine < endLine;
    }

    private static int findLineAtOrBefore(List<int[]> lineNodePositions, int pos) {
        if (pos < 0) return -1;

        int bestLine = -1;
        for (int[] entry : lineNodePositions) {
            if (entry[0] <= pos) {
                bestLine = entry[1];
            } else {
                break;
            }
        }
        return bestLine;
    }

    private static int findLineAtOrAfter(List<int[]> lineNodePositions, int pos) {
        if (pos < 0) return -1;

        for (int[] entry : lineNodePositions) {
            if (entry[0] >= pos) {
                return entry[1];
            }
        }
        return -1;
    }
}
