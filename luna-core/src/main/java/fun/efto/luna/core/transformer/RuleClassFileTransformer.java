package fun.efto.luna.core.transformer;

import fun.efto.luna.core.injection.InjectionPoint;
import fun.efto.luna.core.injection.PersistentInjection;
import fun.efto.luna.core.injection.rule.InjectionRule;
import fun.efto.luna.core.plugin.registry.RuleConverterRegistry;
import fun.efto.luna.core.injection.rule.RuleManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.instrument.ClassFileTransformer;
import java.security.ProtectionDomain;
import java.util.ArrayList;
import java.util.List;

/**
 * @author : Tony.L(286269159@qq.com)
 * @since  : 2026/05/02 16:00
 */
public class RuleClassFileTransformer implements ClassFileTransformer {
    private static final Logger LOGGER = LoggerFactory.getLogger(RuleClassFileTransformer.class);
    private final ClassTransformer classTransformer = new DefaultClassTransformer();

    @Override
    public byte[] transform(ClassLoader loader, String className, Class<?> classBeingRedefined,
                            ProtectionDomain protectionDomain, byte[] classfileBuffer) {
        if (className == null) return null;
        
        String normalizedClassName = className.replace('/', '.');
        
        List<InjectionRule> matchedRules = RuleManager.getInstance().findRulesForClass(normalizedClassName);
        if (matchedRules.isEmpty()) {
            return null;
        }

        LOGGER.info("Applying {} rule(s) to newly loaded class: {}", matchedRules.size(), normalizedClassName);

        List<InjectionPoint> points = new ArrayList<>();
        for (InjectionRule rule : matchedRules) {
            try {
                points.add(RuleConverterRegistry.getInstance().convert(toPersistentInjection(rule)));
            } catch (Exception e) {
                LOGGER.error("Failed to convert rule to injection point for class: {}", normalizedClassName, e);
            }
        }

        if (points.isEmpty()) return null;

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

    private PersistentInjection toPersistentInjection(InjectionRule rule) {
        PersistentInjection injection = new PersistentInjection();
        injection.setClazz(rule.getTargetClass());
        injection.setMethodName(rule.getTargetMethod());
        injection.setMethodDescriptor(rule.getMethodDescriptor());
        injection.setInjectionLocation(rule.getInjectionLocation());
        injection.setLineNumber(rule.getLineNumber());
        injection.setExpression(rule.getExpression());
        injection.setCode(rule.getLogContent());
        injection.setCodeType(rule.getCodeType());
        injection.setEnabled(rule.isEnabled());
        return injection;
    }
}
