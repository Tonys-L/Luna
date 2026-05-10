package fun.efto.luna.core.transformer;

import fun.efto.luna.core.injection.InjectionPoint;
import fun.efto.luna.core.rule.InjectionRule;
import fun.efto.luna.core.rule.RuleConverter;
import fun.efto.luna.core.rule.RuleManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.instrument.ClassFileTransformer;
import java.security.ProtectionDomain;
import java.util.ArrayList;
import java.util.List;

/**
 * 全局规则转换器：在类初次加载时根据策略自动注入代码
 */
public class RuleClassFileTransformer implements ClassFileTransformer {
    private static final Logger LOGGER = LoggerFactory.getLogger(RuleClassFileTransformer.class);
    private final ClassTransformer classTransformer = new DefaultClassTransformer();

    @Override
    public byte[] transform(ClassLoader loader, String className, Class<?> classBeingRedefined,
                            ProtectionDomain protectionDomain, byte[] classfileBuffer) {
        if (className == null) return null;
        if (classBeingRedefined != null) return null;
        
        String normalizedClassName = className.replace('/', '.');
        
        // 1. 查找匹配的规则
        List<InjectionRule> matchedRules = RuleManager.getInstance().findRulesForClass(normalizedClassName);
        if (matchedRules.isEmpty()) {
            return null;
        }

        LOGGER.info("Applying {} rule(s) to newly loaded class: {}", matchedRules.size(), normalizedClassName);

        // 2. 将规则转换为注入点
        List<InjectionPoint> points = new ArrayList<>();
        for (InjectionRule rule : matchedRules) {
            try {
                points.add(RuleConverter.convert(rule));
            } catch (Exception e) {
                LOGGER.error("Failed to convert rule to injection point for class: {}", normalizedClassName, e);
            }
        }

        if (points.isEmpty()) return null;

        // 3. 逐个执行字节码转换
        byte[] currentBytecode = classfileBuffer;
        for (InjectionPoint point : points) {
            try {
                TransformerResult result = classTransformer.transform(point, normalizedClassName, currentBytecode);
                if (result.isTransformed()) {
                    currentBytecode = result.getBytecode();
                }
            } catch (Exception e) {
                LOGGER.error("Failed to apply rule {} to class {}", point.getId(), normalizedClassName, e);
            }
        }

        return currentBytecode == classfileBuffer ? null : currentBytecode;
    }
}
