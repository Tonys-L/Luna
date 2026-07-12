/**
 * @author : Tony.L(286269159@qq.com)
 * @since : 2026/06/10 22:00
 */
package fun.efto.luna.core.bytecode.asm.analyzer;

import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.LineNumberNode;
import org.objectweb.asm.tree.MethodNode;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 统一的行号→注入点位置解析器。
 *
 * 职责：将源码行号映射为指令列表中的位置索引。
 * 这是分析能力，不属于注入逻辑。
 *
 * 提供 BeforeLine 和 AfterLine 两种策略：
 * - BeforeLine：注入点 = LineNumberNode 位置（该行代码执行之前）
 * - AfterLine：注入点 = 该行最后一条指令之后的位置（该行代码执行之后）
 */
public final class LineOffsetResolver {

    private LineOffsetResolver() {}

    /**
     * 解析 BeforeLine 注入点位置。
     * 返回 LineNumberNode 在指令列表中的位置索引。
     *
     * @param methodNode 方法节点
     * @param lineNumber 源码行号
     * @return LineNumberNode 位置索引，未找到返回 -1
     */
    public static int resolveBeforeLine(MethodNode methodNode, int lineNumber) {
        return resolve(methodNode, lineNumber);
    }

    /**
     * 解析 AfterLine 注入点位置。
     * 返回该行最后一条指令之后的位置索引。
     *
     * @param methodNode 方法节点
     * @param lineNumber 源码行号
     * @return 最后一条指令之后的位置索引，未找到返回 -1
     */
    public static int resolveAfterLine(MethodNode methodNode, int lineNumber) {
        int lineOffset = resolve(methodNode, lineNumber);
        if (lineOffset < 0) return -1;

        // 找到 LineNumberNode
        AbstractInsnNode insn = methodNode.instructions.getFirst();
        for (int i = 0; i < lineOffset; i++) {
            insn = insn.getNext();
        }
        if (!(insn instanceof LineNumberNode)) return -1;

        // 找到该行最后一条指令
        AbstractInsnNode lastInsn = findLastInsnOfLine((LineNumberNode) insn);
        if (lastInsn == null) return lineOffset; // 回退到 LineNumberNode 位置

        // 计算 lastInsn 之后的位置
        int pos = 0;
        for (AbstractInsnNode i = methodNode.instructions.getFirst(); i != null; i = i.getNext()) {
            if (i == lastInsn) {
                return pos + 1;
            }
            pos++;
        }
        return -1;
    }

    /**
     * 解析行号对应的第一个 LineNumberNode 位置（基础方法）。
     *
     * @param methodNode 方法节点
     * @param lineNumber 源码行号
     * @return 第一个匹配的 LineNumberNode 在指令列表中的位置，未找到返回 -1
     */
    public static int resolve(MethodNode methodNode, int lineNumber) {
        if (methodNode.instructions == null) return -1;
        int pos = 0;
        for (AbstractInsnNode insn = methodNode.instructions.getFirst(); insn != null; insn = insn.getNext()) {
            if (insn instanceof LineNumberNode) {
                if (((LineNumberNode) insn).line == lineNumber) {
                    return pos;
                }
            }
            pos++;
        }
        return -1;
    }

    /**
     * 解析行号对应的所有字节码偏移量（用于诊断）。
     */
    public static List<Integer> resolveAll(MethodNode methodNode, int lineNumber) {
        if (methodNode.instructions == null) return Collections.emptyList();
        List<Integer> positions = new ArrayList<>();
        int pos = 0;
        for (AbstractInsnNode insn = methodNode.instructions.getFirst(); insn != null; insn = insn.getNext()) {
            if (insn instanceof LineNumberNode) {
                if (((LineNumberNode) insn).line == lineNumber) {
                    positions.add(pos);
                }
            }
            pos++;
        }
        return positions;
    }

    /**
     * 获取方法中所有行号→偏移量的映射（用于诊断）。
     */
    public static List<int[]> getLinePositions(MethodNode methodNode) {
        if (methodNode.instructions == null) return Collections.emptyList();
        List<int[]> positions = new ArrayList<>();
        int pos = 0;
        for (AbstractInsnNode insn = methodNode.instructions.getFirst(); insn != null; insn = insn.getNext()) {
            if (insn instanceof LineNumberNode) {
                positions.add(new int[]{pos, ((LineNumberNode) insn).line});
            }
            pos++;
        }
        return positions;
    }

    /**
     * 找到 LineNumberNode 对应行内最后一条真实指令。
     * 从 startLnn 开始，直到遇到下一个 LineNumberNode 为止。
     *
     * @param startLnn 起始 LineNumberNode
     * @return 该行最后一条真实指令，无则返回 null
     */
    public static AbstractInsnNode findLastInsnOfLine(LineNumberNode startLnn) {
        AbstractInsnNode insn = startLnn;
        AbstractInsnNode lastRealInsn = null;

        while (insn != null) {
            if (insn instanceof LineNumberNode && insn != startLnn) {
                break;
            }
            if (!(insn instanceof LineNumberNode) && insn.getOpcode() != -1) {
                lastRealInsn = insn;
            }
            insn = insn.getNext();
        }

        return lastRealInsn;
    }
}
