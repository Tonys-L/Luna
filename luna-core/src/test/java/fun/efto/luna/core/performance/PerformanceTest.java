package fun.efto.luna.core.performance;

import fun.efto.luna.core.expression.ast.ExpressionNode;
import fun.efto.luna.core.expression.bytecode.ExpressionBytecodeGenerator;
import fun.efto.luna.core.expression.parser.ExpressionParser;
import fun.efto.luna.core.expression.Token;
import fun.efto.luna.core.expression.Tokenizer;
import org.junit.jupiter.api.Test;
import java.util.List;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * @author : Tony.L(286269159@qq.com)
 * @since  : 2026/03/29 02:30
 */
public class PerformanceTest {

    private static final long SINGLE_OPERATION_THRESHOLD_NS = 100_000;
    private static final long COMPOSITE_OPERATION_THRESHOLD_NS = 500_000;
    private static final int WARMUP_ITERATIONS = 100;
    private static final int MEASURE_ITERATIONS = 1000;

    @Test
    public void testExpressionParserPerformance() {
        String expression = "(10 + 5) * 2 > 20 && length(\"test\") == 4";

        for (int i = 0; i < WARMUP_ITERATIONS; i++) {
            Tokenizer tokenizer = new Tokenizer(expression);
            tokenizer.tokenize();
            ExpressionParser parser = new ExpressionParser(tokenizer.getTokens());
            parser.parse().evaluate(null);
        }

        long totalTime = 0;
        for (int i = 0; i < MEASURE_ITERATIONS; i++) {
            long startTime = System.nanoTime();
            Tokenizer tokenizer = new Tokenizer(expression);
            tokenizer.tokenize();
            List<Token> tokens = tokenizer.getTokens();
            ExpressionParser parser = new ExpressionParser(tokens);
            ExpressionNode rootNode = parser.parse();
            rootNode.evaluate(null);
            totalTime += System.nanoTime() - startTime;
        }

        long averageTime = totalTime / MEASURE_ITERATIONS;
        System.out.println("表达式解析和求值平均时间: " + averageTime + " ns");
        assertTrue(averageTime < SINGLE_OPERATION_THRESHOLD_NS, "表达式解析和求值时间超过 0.1ms");
    }

    @Test
    public void testBytecodeGenerationPerformance() {
        String expression = "(x + y) * 2 > 20 && length(\"test\") == 4";
        ExpressionBytecodeGenerator generator = new ExpressionBytecodeGenerator();

        for (int i = 0; i < WARMUP_ITERATIONS; i++) {
            generator.generate(expression);
        }

        long totalTime = 0;
        for (int i = 0; i < MEASURE_ITERATIONS; i++) {
            long startTime = System.nanoTime();
            generator.generate(expression);
            totalTime += System.nanoTime() - startTime;
        }

        long averageTime = totalTime / MEASURE_ITERATIONS;
        System.out.println("字节码生成平均时间: " + averageTime + " ns");
        assertTrue(averageTime < COMPOSITE_OPERATION_THRESHOLD_NS, "字节码生成时间超过 0.5ms");
    }
}
