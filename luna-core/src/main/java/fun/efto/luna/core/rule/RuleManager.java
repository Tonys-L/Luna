/**
 * 规则管理器
 * @author ：Tony.L(<286269159@qq.com>)
 * @since ：2026/03/29 02:30
 */
package fun.efto.luna.core.rule;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class RuleManager {
    private static final RuleManager INSTANCE = new RuleManager();
    private final ConcurrentHashMap<Long, InjectionRule> rules = new ConcurrentHashMap<>();
    private final AtomicLong idGenerator = new AtomicLong(1);
    private final RulePersistenceService persistenceService;
    private final ScheduledExecutorService saveScheduler;
    private volatile boolean dirty = false;

    private RuleManager() {
        persistenceService = new RulePersistenceService();
        persistenceService.initialize();
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
        return id;
    }

    public void updateRule(long id, InjectionRule rule) {
        if (rules.containsKey(id)) {
            rule.setId(id);
            rules.put(id, rule);
            saveRules();
        }
    }

    public void deleteRule(long id) {
        rules.remove(id);
        saveRules();
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
