package fun.efto.luna.core.init;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.ServiceLoader;

/**
 * @author ：Tony.L(286269159@qq.com)
 * @since ：2025/10/4 17:32
 */
public final class InitializerManager {
    private static final Logger LOGGER = LoggerFactory.getLogger(InitializerManager.class);
    private static final InitializerManager INSTANCE = new InitializerManager();
    private final List<Initializer> initializers = new ArrayList<>();

    private InitializerManager() {
        initializers.add(new DefaultInitializer());
        loadInitializers();
    }

    public static InitializerManager getInstance() {
        return INSTANCE;
    }

    private void loadInitializers() {
        ServiceLoader.load(Initializer.class).forEach(initializers::add);
    }

    public void initializeAll() {
        for (Initializer initializer : initializers) {
            try {
                initializer.initialize();
                LOGGER.info("初始化完成: {}", initializer.getClass().getName());
            } catch (Exception e) {
                LOGGER.error("初始化失败: {}", initializer.getClass().getName(), e);
            }
        }
    }

    public List<Initializer> getInitializers() {
        return new ArrayList<>(initializers);
    }
}
