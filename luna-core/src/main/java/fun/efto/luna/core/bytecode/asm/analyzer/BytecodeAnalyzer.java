/**
 * @author : Tony.L(286269159@qq.com)
 * @since : 2026/06/11 00:45
 */
package fun.efto.luna.core.bytecode.asm.analyzer;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodNode;

import java.util.Optional;

/**
 * 统一字节码解析和方法定位。
 *
 * 职责：
 * - 解析字节码为 ClassNode
 * - 根据方法名和描述符定位 MethodNode
 *
 * 这是分析能力，不属于注入逻辑。
 */
public final class BytecodeAnalyzer {

    private final ClassNode classNode;

    BytecodeAnalyzer(ClassNode classNode) {
        this.classNode = classNode;
    }

    /**
     * 解析字节码为 ClassNode。
     * 统一使用 SKIP_FRAMES 标志（注入器需要 COMPUTE_FRAMES 重新计算）。
     */
    public static BytecodeAnalyzer parse(byte[] bytecode) {
        ClassNode cn = new ClassNode();
        new ClassReader(bytecode).accept(cn, ClassReader.SKIP_FRAMES);
        return new BytecodeAnalyzer(cn);
    }

    /**
     * 获取解析后的 ClassNode。
     */
    public ClassNode getClassNode() {
        return classNode;
    }

    /**
     * 根据方法名和描述符定位 MethodNode。
     * 如果描述符为 null 或空，匹配第一个同名方法。
     */
    public Optional<MethodNode> findMethod(String methodName, String methodDescriptor) {
        for (MethodNode mn : classNode.methods) {
            if (!mn.name.equals(methodName)) continue;
            if (methodDescriptor != null && !methodDescriptor.isEmpty() && !mn.desc.equals(methodDescriptor)) continue;
            return Optional.of(mn);
        }
        return Optional.empty();
    }
}
