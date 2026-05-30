package fun.efto.luna.core.rule;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import fun.efto.luna.core.injection.InjectionManager;
import fun.efto.luna.core.injection.PersistentInjection;

/**
 * 规则管理器
 * @author : Tony.L(286269159@qq.com>)
 * @since ：2026/03/29 02:30
 */
public class RuleManager {
    private static final RuleManager INSTANCE = new RuleManager();
    private final ConcurrentHashMap<Long, InjectionRule> rules = new ConcurrentHashMap<>();
    private final AtomicLong idGenerator = new AtomicLong(1);
    private final RulePersistenceService persistenceService;
    private final ScheduledExecutorService saveScheduler;
    private volatile boolean dirty = false;

    private RuleManager() {
        persistenceService = new RulePersistenceService();
        List<InjectionRule> loadedRules = persistenceService.initialize();
        
        long maxId = 0;
        for (InjectionRule rule : loadedRules) {
            rules.put(rule.getId(), rule);
            if (rule.getId() > maxId) {
                maxId = rule.getId();
            }
        }
        idGenerator.set(maxId + 1);

        saveScheduler = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "luna-rule-persister");
            t.setDaemon(true);
            return t;
        });
        saveScheduler.scheduleWithFixedDelay(this::flushIfDirty, 5, 5, TimeUnit.SECONDS);
    }

    public static RuleManager getInstance() {
        return INSTANCE;
    }

    public List<InjectionRule> getRules() {
        return new ArrayList<>(rules.values());
    }

    public InjectionRule getRule(long id) {
        return rules.get(id);
    }

    public long addRule(InjectionRule rule) {
        long id = idGenerator.getAndIncrement();
        rule.setId(id);
        rules.put(id, rule);
        saveRules();
        if (rule.getTargetClass() != null && !rule.getTargetClass().isEmpty()) {
            InjectionManager.getInstance().addInjection(toPersistentInjection(rule));
        }
        return id;
    }

    public void updateRule(long id, InjectionRule rule) {
        if (rules.containsKey(id)) {
            rule.setId(id);
            rules.put(id, rule);
            saveRules();
            InjectionManager.getInstance().updateInjection("rule-" + id, toPersistentInjection(rule));
        }
    }

    public void deleteRule(long id) {
        InjectionRule rule = rules.remove(id);
        if (rule != null) {
            saveRules();
            InjectionManager.getInstance().removeInjection("rule-" + id);
        }
    }

    /**
     * 查找匹配指定类名的所有规则
     */
    public List<InjectionRule> findRulesForClass(String className) {
        List<InjectionRule> result = new ArrayList<>();
        for (InjectionRule rule : rules.values()) {
            if (!rule.isEnabled()) {
                continue;
            }
            // 支持简单的正则匹配或全路径匹配
            if (ClassNameMatcher.matches(className, rule.getTargetClass())) {
                result.add(rule);
            }
        }
        return result;
    }

    public List<InjectionRule> getSuspendedRules() {
        List<InjectionRule> result = new ArrayList<>();
        for (InjectionRule rule : rules.values()) {
            if (rule.getStatus() == RuleStatus.SUSPENDED) {
                result.add(rule);
            }
        }
        return result;
    }

    /**
     * 应用指定类的所有规则
     */
    public void applyRulesForClass(String className) {
        // 此方法通常用于初次加载，retransform 会走 Transformer 路径
    }

    public void syncRulesToInjectionManager() {
        for (InjectionRule rule : rules.values()) {
            if (rule.isEnabled() && rule.getTargetClass() != null && !rule.getTargetClass().isEmpty()) {
                InjectionManager.getInstance().addInjection(toPersistentInjection(rule));
            }
        }
    }

    private PersistentInjection toPersistentInjection(InjectionRule rule) {
        PersistentInjection injection = new PersistentInjection();
        injection.setId("rule-" + rule.getId());
        injection.setClazz(rule.getTargetClass());
        injection.setMethodName(rule.getTargetMethod());
        injection.setMethodDescriptor(rule.getMethodDescriptor());
        injection.setInjectionType(rule.getInjectionType());
        injection.setLineNumber(rule.getLineNumber());
        injection.setExpression(rule.getExpression());
        injection.setCode(rule.getLogContent());
        injection.setCodeType(rule.getCodeType());
        injection.setEnabled(rule.isEnabled());
        injection.setEphemeral(true);
        return injection;
    }

    private void saveRules() {
        dirty = true;
    }

    private void flushIfDirty() {
        if (dirty) {
            dirty = false;
            try {
                persistenceService.saveRules();
            } catch (Exception e) {
                System.err.println("保存规则失败: " + e.getMessage());
            }
        }
    }

    public void shutdown() {
        flushIfDirty();
        saveScheduler.shutdown();
        try {
            saveScheduler.awaitTermination(5, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        persistenceService.shutdown();
    }
}
