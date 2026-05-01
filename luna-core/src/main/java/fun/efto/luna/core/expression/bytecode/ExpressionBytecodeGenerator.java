package fun.efto.luna.core.expression.bytecode;

import fun.efto.luna.core.expression.Token;
import fun.efto.luna.core.expression.Tokenizer;
import fun.efto.luna.core.expression.ast.*;
import fun.efto.luna.core.expression.parser.ExpressionParser;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import java.util.List;

/**
 * 表达式字节码生成器，将表达式编译为字节码
 * @author ：Tony.L(<286269159@qq.com>)
 * @since ：2026/03/29 02:30
 */
public class ExpressionBytecodeGenerator {
    private VariableSlotResolver variableSlotResolver;

    public ExpressionBytecodeGenerator() {
    }

    public ExpressionBytecodeGenerator(VariableSlotResolver variableSlotResolver) {
        this.variableSlotResolver = variableSlotResolver;
    }

    /**
     * 生成表达式的字节码
     * @param expression 表达式字符串
     * @return 生成的字节码
     */
    public byte[] generate(String expression) {
        // 词法分析
        Tokenizer tokenizer = new Tokenizer(expression);
        tokenizer.tokenize();
        List<Token> tokens = tokenizer.getTokens();

        // 语法分析
        ExpressionParser parser = new ExpressionParser(tokens);
        ExpressionNode rootNode = parser.parse();

        // 生成字节码
        return generateBytecode(rootNode);
    }

    /**
     * 生成字节码
     * @param node 表达式节点
     * @return 生成的字节码
     */
    private byte[] generateBytecode(ExpressionNode node) {
        // 使用ASM生成字节码
        ClassWriter cw = new ClassWriter(ClassWriter.COMPUTE_FRAMES | ClassWriter.COMPUTE_MAXS);
        cw.visit(Opcodes.V1_8, Opcodes.ACC_PUBLIC, "ExpressionEvaluator", null, "java/lang/Object", null);

        // 生成默认构造函数
        MethodVisitor mv = cw.visitMethod(Opcodes.ACC_PUBLIC, "<init>", "()V", null, null);
        mv.visitCode();
        mv.visitVarInsn(Opcodes.ALOAD, 0);
        mv.visitMethodInsn(Opcodes.INVOKESPECIAL, "java/lang/Object", "<init>", "()V", false);
        mv.visitInsn(Opcodes.RETURN);
        mv.visitMaxs(1, 1);
        mv.visitEnd();

        // 生成evaluate方法
        mv = cw.visitMethod(Opcodes.ACC_PUBLIC | Opcodes.ACC_STATIC, "evaluate", "(Ljava/lang/Object;)Ljava/lang/Object;", null, null);
        mv.visitCode();

        // 生成表达式的字节码
        generateExpressionBytecode(mv, node);

        // 结束方法
        mv.visitInsn(Opcodes.ARETURN);
        mv.visitMaxs(10, 2); // 预留足够的栈空间
        mv.visitEnd();

        cw.visitEnd();
        return cw.toByteArray();
    }

    /**
     * 生成表达式节点的字节码
     * @param mv 方法访问器
     * @param node 表达式节点
     */
    public void generateExpressionBytecode(MethodVisitor mv, ExpressionNode node) {
        if (node instanceof ConstantNode) {
            generateConstantBytecode(mv, (ConstantNode) node);
        } else if (node instanceof VariableNode) {
            generateVariableBytecode(mv, (VariableNode) node);
        } else if (node instanceof BinaryExpressionNode) {
            generateBinaryExpressionBytecode(mv, (BinaryExpressionNode) node);
        } else if (node instanceof UnaryExpressionNode) {
            generateUnaryExpressionBytecode(mv, (UnaryExpressionNode) node);
        } else if (node instanceof FunctionCallNode) {
            generateFunctionCallBytecode(mv, (FunctionCallNode) node);
        }
    }

    /**
     * 生成常量节点的字节码
     */
    private void generateConstantBytecode(MethodVisitor mv, ConstantNode node) {
        Object value = node.getValue();
        if (value instanceof Integer) {
            mv.visitLdcInsn(value);
            mv.visitMethodInsn(Opcodes.INVOKESTATIC, "java/lang/Integer", "valueOf", "(I)Ljava/lang/Integer;", false);
        } else if (value instanceof Double) {
            mv.visitLdcInsn(value);
            mv.visitMethodInsn(Opcodes.INVOKESTATIC, "java/lang/Double", "valueOf", "(D)Ljava/lang/Double;", false);
        } else if (value instanceof String) {
            mv.visitLdcInsn(value);
        } else if (value instanceof Boolean) {
            mv.visitLdcInsn(value);
            mv.visitMethodInsn(Opcodes.INVOKESTATIC, "java/lang/Boolean", "valueOf", "(Z)Ljava/lang/Boolean;", false);
        }
    }

    /**
     * 生成变量节点的字节码
     */
    private void generateVariableBytecode(MethodVisitor mv, VariableNode node) {
        String name = node.getName();
        // 从上下文获取变量值
        mv.visitVarInsn(Opcodes.ALOAD, 0); // 加载上下文对象
        mv.visitLdcInsn(name);
        mv.visitMethodInsn(Opcodes.INVOKESTATIC, "fun/efto/luna/core/expression/ExpressionUtils", "getVariableValue", "(Ljava/lang/Object;Ljava/lang/String;)Ljava/lang/Object;", false);
    }

    /**
     * 生成二元表达式节点的字节码
     */
    private void generateBinaryExpressionBytecode(MethodVisitor mv, BinaryExpressionNode node) {
        generateExpressionBytecode(mv, node.getLeft());
        generateExpressionBytecode(mv, node.getRight());
        String operator = node.getOperator();

        switch (operator) {
            case "+":
                // 字符串连接或数字加法
                mv.visitMethodInsn(Opcodes.INVOKESTATIC, "fun/efto/luna/core/expression/ExpressionUtils", "add", "(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;", false);
                break;
            case "-":
                mv.visitMethodInsn(Opcodes.INVOKESTATIC, "fun/efto/luna/core/expression/ExpressionUtils", "subtract", "(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;", false);
                break;
            case "*":
                mv.visitMethodInsn(Opcodes.INVOKESTATIC, "fun/efto/luna/core/expression/ExpressionUtils", "multiply", "(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;", false);
                break;
            case "/":
                mv.visitMethodInsn(Opcodes.INVOKESTATIC, "fun/efto/luna/core/expression/ExpressionUtils", "divide", "(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;", false);
                break;
            case "==":
                mv.visitMethodInsn(Opcodes.INVOKESTATIC, "fun/efto/luna/core/expression/ExpressionUtils", "equals", "(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Boolean;", false);
                break;
            case "!=":
                mv.visitMethodInsn(Opcodes.INVOKESTATIC, "fun/efto/luna/core/expression/ExpressionUtils", "notEquals", "(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Boolean;", false);
                break;
            case ">":
                mv.visitMethodInsn(Opcodes.INVOKESTATIC, "fun/efto/luna/core/expression/ExpressionUtils", "greaterThan", "(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Boolean;", false);
                break;
            case ">=":
                mv.visitMethodInsn(Opcodes.INVOKESTATIC, "fun/efto/luna/core/expression/ExpressionUtils", "greaterThanOrEqual", "(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Boolean;", false);
                break;
            case "<":
                mv.visitMethodInsn(Opcodes.INVOKESTATIC, "fun/efto/luna/core/expression/ExpressionUtils", "lessThan", "(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Boolean;", false);
                break;
            case "<=":
                mv.visitMethodInsn(Opcodes.INVOKESTATIC, "fun/efto/luna/core/expression/ExpressionUtils", "lessThanOrEqual", "(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Boolean;", false);
                break;
            case "&&":
                mv.visitMethodInsn(Opcodes.INVOKESTATIC, "fun/efto/luna/core/expression/ExpressionUtils", "logicalAnd", "(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Boolean;", false);
                break;
            case "||":
                mv.visitMethodInsn(Opcodes.INVOKESTATIC, "fun/efto/luna/core/expression/ExpressionUtils", "logicalOr", "(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Boolean;", false);
                break;
        }
    }

    /**
     * 生成一元表达式节点的字节码
     */
    private void generateUnaryExpressionBytecode(MethodVisitor mv, UnaryExpressionNode node) {
        generateExpressionBytecode(mv, node.getExpression());
        String operator = node.getOperator();

        if (operator.equals("!")) {
            mv.visitMethodInsn(Opcodes.INVOKESTATIC, "fun/efto/luna/core/expression/ExpressionUtils", "logicalNot", "(Ljava/lang/Object;)Ljava/lang/Boolean;", false);
        } else if (operator.equals("-")) {
            mv.visitMethodInsn(Opcodes.INVOKESTATIC, "fun/efto/luna/core/expression/ExpressionUtils", "negate", "(Ljava/lang/Object;)Ljava/lang/Object;", false);
        }
    }

    /**
     * 生成函数调用节点的字节码
     */
    private void generateFunctionCallBytecode(MethodVisitor mv, FunctionCallNode node) {
        String functionName = node.getFunctionName();
        List<ExpressionNode> arguments = node.getArguments();

        // 加载参数
        mv.visitInsn(Opcodes.ICONST_0 + arguments.size());
        mv.visitTypeInsn(Opcodes.ANEWARRAY, "java/lang/Object");

        for (int i = 0; i < arguments.size(); i++) {
            mv.visitInsn(Opcodes.DUP);
            mv.visitInsn(Opcodes.ICONST_0 + i);
            generateExpressionBytecode(mv, arguments.get(i));
            mv.visitInsn(Opcodes.AASTORE);
        }

        // 调用函数
        mv.visitLdcInsn(functionName);
        mv.visitVarInsn(Opcodes.ALOAD, 0); // 加载上下文
        mv.visitMethodInsn(Opcodes.INVOKESTATIC, "fun/efto/luna/core/expression/ExpressionUtils", "callFunction", "(Ljava/lang/String;[Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;", false);
    }
}
