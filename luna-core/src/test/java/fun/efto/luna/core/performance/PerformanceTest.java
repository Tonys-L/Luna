package fun.efto.luna.core.performance;

import fun.efto.luna.core.expression.ast.ExpressionNode;
import fun.efto.luna.core.expression.bytecode.ExpressionBytecodeGenerator;
import fun.efto.luna.core.expression.parser.ExpressionParser;
import fun.efto.luna.core.expression.Token;
import fun.efto.luna.core.expression.Tokenizer;
import fun.efto.luna.core.rule.InjectionRule;
import fun.efto.luna.core.rule.RuleManager;
import org.junit.jupiter.api.Test;
import java.util.List;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 性能测试类，测试系统性能是否满足 < 0.1ms 要求
 * @author ：Tony.L(<286269159@qq.com>)
 * @since ：2026/03/29 02:30
 */
public class PerformanceTest {

    private static final long PERFORMANCE_THRESHOLD_NS = 100_000; // 0.1ms = 100,000 ns

    @Test
    public void testExpressionParserPerformance() {
        String expression = "(10 + 5) * 2 > 20 && length(\"test\") == 4";
        
        // 测试多次执行的平均时间
        int iterations = 1000;
        long totalTime = 0;
        
        for (int i = 0; i < iterations; i++) {
            long startTime = System.nanoTime();
            
            // 词法分析
            Tokenizer tokenizer = new Tokenizer(expression);
            tokenizer.tokenize();
            List<Token> tokens = tokenizer.getTokens();
            
            // 语法分析
            ExpressionParser parser = new ExpressionParser(tokens);
            ExpressionNode rootNode = parser.parse();
            
            // 求值
            rootNode.evaluate(null);
            
            long endTime = System.nanoTime();
            totalTime += (endTime - startTime);
        }
        
        long averageTime = totalTime / iterations;
        System.out.println("表达式解析和求值平均时间: " + averageTime + " ns");
        assertTrue(averageTime < PERFORMANCE_THRESHOLD_NS, "表达式解析和求值时间超过 0.1ms");
    }

    @Test
    public void testRuleManagementPerformance() {
        RuleManager ruleManager = RuleManager.getInstance();
        
        // 测试规则添加性能
        int iterations = 1000;
        long totalTime = 0;
        
        for (int i = 0; i < iterations; i++) {
            long startTime = System.nanoTime();
            
            // 创建测试规则
            InjectionRule rule = new InjectionRule();
            rule.setTargetClass("TestApp");
            rule.setTargetMethod("main");
            rule.setInjectionType("method");
            rule.setExpression("true");
            rule.setLogContent("Injected log");
            
            // 添加规则
            long ruleId = ruleManager.addRule(rule);
            
            // 删除规则
            ruleManager.deleteRule(ruleId);
            
            long endTime = System.nanoTime();
            totalTime += (endTime - startTime);
        }
        
        long averageTime = totalTime / iterations;
        System.out.println("规则添加和删除平均时间: " + averageTime + " ns");
        assertTrue(averageTime < PERFORMANCE_THRESHOLD_NS, "规则管理时间超过 0.1ms");
    }

    @Test
    public void testBytecodeGenerationPerformance() {
        String expression = "(x + y) * 2 > 20 && length(\"test\") == 4";
        ExpressionBytecodeGenerator generator = new ExpressionBytecodeGenerator();
        
        // 测试字节码生成性能
        int iterations = 100;
        long totalTime = 0;
        
        for (int i = 0; i < iterations; i++) {
            long startTime = System.nanoTime();
            
            // 生成字节码
            byte[] bytecode = generator.generate(expression);
            
            long endTime = System.nanoTime();
            totalTime += (endTime - startTime);
        }
        
        long averageTime = totalTime / iterations;
        System.out.println("字节码生成平均时间: " + averageTime + " ns");
        assertTrue(averageTime < PERFORMANCE_THRESHOLD_NS, "字节码生成时间超过 0.1ms");
    }
}
