package fun.efto.luna.core.bytecode.asm.analyzer;

import fun.efto.luna.core.bytecode.asm.AsmInjectionContext;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
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
 * 局部变量可见性扫描器。
 *
 * 策略与机制分离：
 * - 机制（稳定）：scanAtPosition — 给定指令位置，判断变量可见性
 * - 策略（变化）：行号→位置的映射，由调用方决定（BeforeLine/AfterLine/前端点击）
 *
 * @author : Tony.L(286269159@qq.com)
 * @since : 2026/05/03 19:10
 */
public final class LocalVariableScanner {

    private static final Logger LOGGER = LoggerFactory.getLogger(LocalVariableScanner.class);

    private LocalVariableScanner() {
    }

    // ==================== 结果对象 ====================

    /**
     * 扫描结果，包含所有可见变量和排除的同行声明变量。
     * 一次遍历同时计算两个列表，避免重复遍历。
     */
    public static class ScanResult {
        private final List<AsmInjectionContext.LocalVarInfo> allVars;
        private final List<AsmInjectionContext.LocalVarInfo> sameLineStartVars;

        public ScanResult(List<AsmInjectionContext.LocalVarInfo> allVars,
                          List<AsmInjectionContext.LocalVarInfo> sameLineStartVars) {
            this.allVars = Collections.unmodifiableList(allVars);
            this.sameLineStartVars = Collections.unmodifiableList(sameLineStartVars);
        }

        /** 所有在 queryPos 处可见的变量（不含同行声明排除） */
        public List<AsmInjectionContext.LocalVarInfo> getAllVars() {
            return allVars;
        }

        /** 安全变量 = allVars - sameLineStartVars */
        public List<AsmInjectionContext.LocalVarInfo> getSafeVars() {
            List<AsmInjectionContext.LocalVarInfo> safe = new ArrayList<>(allVars);
            safe.removeIf(v -> sameLineStartVars.stream().anyMatch(s -> s.getSlot() == v.getSlot()));
            return safe;
        }

        /** 在查询行声明的变量（不含方法参数），需由调用方决定是否排除 */
        public List<AsmInjectionContext.LocalVarInfo> getSameLineStartVars() {
            return sameLineStartVars;
        }
    }

    // ==================== 机制（稳定） ====================

    /**
     * 基于指令位置判断变量可见性（纯机制）。
     *
     * 判断条件：startPos <= queryPos < endPos
     * 不涉及行号解析，由调用方确定 queryPos。
     *
     * @param mn       方法节点
     * @param queryPos 查询位置（指令列表中的索引）
     * @return 扫描结果，包含 allVars 和 sameLineStartVars
     */
    public static ScanResult scanAtPosition(MethodNode mn, int queryPos) {
        List<AsmInjectionContext.LocalVarInfo> allVars = new ArrayList<>();
        List<AsmInjectionContext.LocalVarInfo> sameLineStartVars = new ArrayList<>();

        if (mn.localVariables == null || mn.localVariables.isEmpty()) {
            LOGGER.warn("[LocalVarScanner] method={} has no local variable table (compiled without -g?)", mn.name);
            return new ScanResult(allVars, sameLineStartVars);
        }
        if (queryPos < 0) {
            return new ScanResult(allVars, sameLineStartVars);
        }

        int paramSlotEnd = computeParamSlotEnd(mn);

        Map<LabelNode, Integer> labelPosMap = new IdentityHashMap<>();
        int pos = 0;
        for (AbstractInsnNode insn = mn.instructions.getFirst(); insn != null; insn = insn.getNext()) {
            if (insn instanceof LabelNode) {
                labelPosMap.put((LabelNode) insn, pos);
            }
            pos++;
        }

        List<int[]> lineNodePositions = LineOffsetResolver.getLinePositions(mn);
        int queryLine = findLineAtOrBefore(lineNodePositions, queryPos);

        LOGGER.debug("[LocalVarScanner] method={} queryPos={} totalVars={} queryLine={}",
                mn.name, queryPos, mn.localVariables.size(), queryLine);

        for (LocalVariableNode lv : mn.localVariables) {
            if (lv.start == null || lv.end == null) continue;
            if (lv.name != null && lv.name.startsWith("$")) continue;

            int startPos = labelPosMap.containsKey(lv.start) ? labelPosMap.get(lv.start) : -1;
            int endPos = labelPosMap.containsKey(lv.end) ? labelPosMap.get(lv.end) : -1;
            if (endPos < 0) {
                LOGGER.warn("[LocalVarScanner] var={} end label not in labelPosMap, scope end unknown", lv.name);
            }
            int startLine = resolveStartLine(labelPosMap, lineNodePositions, lv.start);

            boolean isParameter = lv.index < paramSlotEnd;
            boolean visible = checkVisibility(queryPos, startPos, endPos);
            boolean isSameLineStart = !isParameter && startLine >= 0 && queryLine >= 0 && queryLine == startLine;

            LOGGER.debug("[LocalVarScanner]   var={} slot={} startPos={} endPos={} startLine={} queryPos={} queryLine={} isParam={} visible={} sameLineStart={}",
                    lv.name, lv.index, startPos, endPos, startLine, queryPos, queryLine, isParameter, visible, isSameLineStart);

            if (visible) {
                AsmInjectionContext.LocalVarInfo varInfo = new AsmInjectionContext.LocalVarInfo(lv.name, lv.desc, lv.index);
                allVars.add(varInfo);
                if (isSameLineStart) {
                    sameLineStartVars.add(varInfo);
                }
            }
        }

        LOGGER.debug("[LocalVarScanner] method={} queryPos={} allVars={} sameLineStartVars={}",
                mn.name, queryPos, allVars.size(), sameLineStartVars.size());
        return new ScanResult(allVars, sameLineStartVars);
    }

    // ==================== 便捷方法（策略：BeforeLine 语义） ====================

    public static List<AsmInjectionContext.LocalVarInfo> scanVisibleLocalVariables(
            byte[] bytecode, String methodName, String methodDescriptor, int lineNumber) {
        return scanVisibleLocalVariables(bytecode, methodName, methodDescriptor, lineNumber, false);
    }

    /**
     * 便捷方法：基于行号查询变量可见性。
     * 使用行号比较算法：变量的 startLine <= lineNumber && endLine >= lineNumber。
     * 用于前端点击行号查询变量等场景。
     */
    public static List<AsmInjectionContext.LocalVarInfo> scanVisibleLocalVariables(
            byte[] bytecode, String methodName, String methodDescriptor, int lineNumber, boolean excludeSameLineStart) {
        if (bytecode == null || bytecode.length == 0 || methodName == null || lineNumber < 1) {
            return Collections.emptyList();
        }

        ClassNode cn;
        try {
            cn = new ClassNode();
            new ClassReader(bytecode).accept(cn, ClassReader.EXPAND_FRAMES);
        } catch (IllegalArgumentException | ArrayIndexOutOfBoundsException e) {
            LOGGER.warn("[LocalVarScanner] Invalid bytecode for method={} line={}: {}", methodName, lineNumber, e.getMessage());
            return Collections.emptyList();
        }

        for (MethodNode mn : cn.methods) {
            if (!mn.name.equals(methodName)) continue;
            if (methodDescriptor != null && !methodDescriptor.isEmpty() && !mn.desc.equals(methodDescriptor)) continue;

            return scanByLineNumber(mn, lineNumber, excludeSameLineStart);
        }

        LOGGER.warn("[LocalVarScanner] No method found: {} desc={} for line {}", methodName, methodDescriptor, lineNumber);
        return Collections.emptyList();
    }

    /**
     * 基于行号的变量可见性判断。
     *
     * 单策略设计：统一使用 LineNumberNode 位置作为 queryPos，
     * 由 scanAtPosition 计算 allVars 和 sameLineStartVars。
     * - excludeSameLineStart=true：返回 safeVars（排除同行声明变量）
     * - excludeSameLineStart=false：返回 allVars（包含同行声明变量）
     */
    private static List<AsmInjectionContext.LocalVarInfo> scanByLineNumber(
            MethodNode mn, int lineNumber, boolean excludeSameLineStart) {
        int queryPos = LineOffsetResolver.resolve(mn, lineNumber);
        if (queryPos < 0) {
            return Collections.emptyList();
        }
        ScanResult result = scanAtPosition(mn, queryPos);
        return excludeSameLineStart ? result.getSafeVars() : result.getAllVars();
    }

    /**
     * 解析变量的起始行号：start label 之后（含）的第一个行号。
     *
     * 变量的 start label 标记作用域开始，通常位于 LineNumberNode 之前。
     * 使用 findLineAtOrAfter 可以正确找到声明行号，
     * 避免 findLineAtOrBefore 返回前一行行号的问题。
     *
     * 例如：id.startPos=4, LineNumberNode(line=30)在pos=5
     * - findLineAtOrBefore(4) → line 29（错误，前一行）
     * - findLineAtOrAfter(4)  → line 30（正确，声明行）
     */
    private static int resolveStartLine(Map<LabelNode, Integer> labelPosMap, List<int[]> lineNodePositions, LabelNode label) {
        if (label == null) return -1;
        Integer pos = labelPosMap.get(label);
        if (pos == null) return -1;
        return findLineAtOrAfter(lineNodePositions, pos);
    }

    /**
     * 纯位置判断：变量 scope 是 [startPos, endPos)，查询位置是 queryPos。
     */
    private static boolean checkVisibility(int queryPos, int startPos, int endPos) {
        if (startPos < 0) return false;
        if (queryPos < 0) return false;
        if (queryPos < startPos) return false;
        if (endPos >= 0 && queryPos >= endPos) return false;
        return true;
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

    private static int computeParamSlotEnd(MethodNode mn) {
        boolean isStatic = (mn.access & Opcodes.ACC_STATIC) != 0;
        int slot = isStatic ? 0 : 1;
        for (Type argType : Type.getArgumentTypes(mn.desc)) {
            slot += argType.getSize();
        }
        return slot;
    }
}
