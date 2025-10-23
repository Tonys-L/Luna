package fun.efto.luna.core.decompile;

import org.benf.cfr.reader.api.CfrDriver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * @author ：Tony.L(286269159@qq.com)
 * @since ：2025/10/19 14:12
 */
public class CfrDecompiler implements Decompiler {
    private static final Logger logger = LoggerFactory.getLogger(CfrDecompiler.class);

    @Override
    public String decompile(String className) {
        try {
            logger.debug("Starting to decompile class: {}", className);
            // 配置 CFR 参数
            Map<String, String> options = new HashMap<>();

            // 获取当前类路径
            StringBuilder classPath = new StringBuilder();
            classPath.append(System.getProperty("java.class.path"));

            options.put("c", classPath.toString());
            logger.debug("Using classpath: {}", classPath);

            SimpleOutputSinkFactory simpleOutputSinkFactory = new SimpleOutputSinkFactory();

            // 创建 CFR 驱动
            CfrDriver driver = new CfrDriver.Builder()
                    .withOptions(options)
                    .withOutputSink(simpleOutputSinkFactory)
                    .build();

            // 执行反编译
            driver.analyse(Collections.singletonList(className));

            if (Objects.nonNull(simpleOutputSinkFactory.getException())) {
                throw simpleOutputSinkFactory.getException();
            }

            String decompiledCode = simpleOutputSinkFactory.getDecompile();
            logger.debug("Decompilation completed for class: {}, result length: {}", className, decompiledCode.length());

            // 如果结果为空，提供更多信息
            if (decompiledCode.isEmpty()) {
                logger.warn("Decompilation returned empty result for class: {}", className);
                return "// Decompilation returned empty result for class: " + className + "\n" +
                        "// This may happen for several reasons:\n" +
                        "// 1. The class file was not found in the classpath\n" +
                        "// 2. The class has complex dependencies that CFR cannot resolve\n" +
                        "// 3. The class is an interface or abstract class with no implementation\n" +
                        "// 4. The class is a synthetic or generated class\n" +
                        "//\n" +
                        "// Current classpath: " + classPath.toString().substring(0, Math.min(200, classPath.length())) +
                        (classPath.length() > 200 ? "..." : "");
            }

            // 返回反编译结果
            return decompiledCode;
        } catch (Exception e) {
            logger.error("Failed to decompile class: " + className, e);
            return "// Decompilation failed for class: " + className + "\n" +
                    "// Error: " + e.getMessage() + "\n" +
                    "// Note: This may happen if the class has complex dependencies or if it's not accessible in the classpath";
        } catch (Throwable t) {
            logger.error("Unexpected error while decompiling class: " + className, t);
            return "// Decompilation failed for class: " + className + "\n" +
                    "// Unexpected error: " + t.getMessage() + "\n" +
                    "// Note: This may happen if the class has complex dependencies or if it's not accessible in the classpath";
        }
    }
}