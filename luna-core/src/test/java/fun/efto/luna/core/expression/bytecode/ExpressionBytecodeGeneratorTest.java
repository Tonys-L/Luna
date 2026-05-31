package fun.efto.luna.core.expression.bytecode;

import fun.efto.luna.core.expression.ast.VariableNode;
import fun.efto.luna.core.expression.ast.ConstantNode;
import fun.efto.luna.core.expression.ast.BinaryExpressionNode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

import static org.junit.jupiter.api.Assertions.*;

/**
 * @author ：Tony.L(<286269159@qq.com>)
 * @since ：2026/03/29 02:30
 */
public class ExpressionBytecodeGeneratorTest {

    private ExpressionBytecodeGenerator generator;
    private VariableSlotResolver resolver;

    @BeforeEach
    public void setUp() {
        resolver = new VariableSlotResolver();
        generator = new ExpressionBytecodeGenerator(resolver);
        // 注册一些测试变量
        resolver.registerVariable("userId", 0);
        resolver.registerVariable("age", 1);
    }

    @Test
    public void testGenerateVariableAccess() {
        // 测试生成变量访问的字节码
        VariableNode variableNode = new VariableNode("userId");
        
        // 创建一个简单的类和方法来测试字节码生成
        ClassWriter cw = new ClassWriter(ClassWriter.COMPUTE_FRAMES);
        cw.visit(Opcodes.V1_8, Opcodes.ACC_PUBLIC, "TestClass", null, "java/lang/Object", null);
        
        MethodVisitor mv = cw.visitMethod(Opcodes.ACC_PUBLIC, "test", "()V", null, null);
        mv.visitCode();
        
        // 生成字节码
        generator.generateExpressionBytecode(mv, variableNode);
        
        mv.visitInsn(Opcodes.RETURN);
        mv.visitMaxs(1, 1);
        mv.visitEnd();
        
        cw.visitEnd();
        
        // 验证字节码不为空
        byte[] bytecode = cw.toByteArray();
        assertNotNull(bytecode, "字节码应该不为空");
        assertTrue(bytecode.length > 0, "字节码长度应该大于 0");
    }

    @Test
    public void testGenerateLiteral() {
        // 测试生成字面量的字节码
        ConstantNode literalNode = new ConstantNode(100, Integer.class);
        
        // 创建一个简单的类和方法来测试字节码生成
        ClassWriter cw = new ClassWriter(ClassWriter.COMPUTE_FRAMES);
        cw.visit(Opcodes.V1_8, Opcodes.ACC_PUBLIC, "TestClass", null, "java/lang/Object", null);
        
        MethodVisitor mv = cw.visitMethod(Opcodes.ACC_PUBLIC, "test", "()V", null, null);
        mv.visitCode();
        
        // 生成字节码
        generator.generateExpressionBytecode(mv, literalNode);
        
        mv.visitInsn(Opcodes.RETURN);
        mv.visitMaxs(1, 1);
        mv.visitEnd();
        
        cw.visitEnd();
        
        // 验证字节码不为空
        byte[] bytecode = cw.toByteArray();
        assertNotNull(bytecode, "字节码应该不为空");
        assertTrue(bytecode.length > 0, "字节码长度应该大于 0");
    }

    @Test
    public void testGenerateBinaryExpression() {
        // 测试生成二元表达式的字节码
        VariableNode left = new VariableNode("userId");
        ConstantNode right = new ConstantNode(100, Integer.class);
        BinaryExpressionNode binaryNode = new BinaryExpressionNode(left, "==", right);
        
        // 创建一个简单的类和方法来测试字节码生成
        ClassWriter cw = new ClassWriter(ClassWriter.COMPUTE_FRAMES);
        cw.visit(Opcodes.V1_8, Opcodes.ACC_PUBLIC, "TestClass", null, "java/lang/Object", null);
        
        MethodVisitor mv = cw.visitMethod(Opcodes.ACC_PUBLIC, "test", "()V", null, null);
        mv.visitCode();
        
        // 生成字节码
        generator.generateExpressionBytecode(mv, binaryNode);
        
        mv.visitInsn(Opcodes.RETURN);
        mv.visitMaxs(2, 1);
        mv.visitEnd();
        
        cw.visitEnd();
        
        // 验证字节码不为空
        byte[] bytecode = cw.toByteArray();
        assertNotNull(bytecode, "字节码应该不为空");
        assertTrue(bytecode.length > 0, "字节码长度应该大于 0");
    }

    @Test
    public void testGeneratePerformance() {
        // 测试字节码生成性能
        VariableNode left = new VariableNode("userId");
        ConstantNode right = new ConstantNode(100, Integer.class);
        BinaryExpressionNode binaryNode = new BinaryExpressionNode(left, "==", right);
        
        // 创建一个简单的类和方法来测试字节码生成
        ClassWriter cw = new ClassWriter(ClassWriter.COMPUTE_FRAMES);
        cw.visit(Opcodes.V1_8, Opcodes.ACC_PUBLIC, "TestClass", null, "java/lang/Object", null);
        
        MethodVisitor mv = cw.visitMethod(Opcodes.ACC_PUBLIC, "test", "()V", null, null);
        mv.visitCode();
        
        long startTime = System.nanoTime();
        generator.generateExpressionBytecode(mv, binaryNode);
        long endTime = System.nanoTime();
        
        mv.visitInsn(Opcodes.RETURN);
        mv.visitMaxs(2, 1);
        mv.visitEnd();
        cw.visitEnd();
        
        long duration = endTime - startTime;
        double milliseconds = duration / 1_000_000.0;
        
        assertTrue(milliseconds < 1.0, "字节码生成过程耗时应该小于 1ms，实际耗时: " + milliseconds + "ms");
    }
}
