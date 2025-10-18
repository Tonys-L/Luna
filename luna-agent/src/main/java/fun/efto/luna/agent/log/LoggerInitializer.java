package fun.efto.luna.agent.log;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.config.Configurator;

import java.net.URL;

/**
 * @author ：Tony.L(286269159@qq.com)
 * @since ：2025/10/7 21:38
 */
@SuppressWarnings("java:S106")
public final class LoggerInitializer {
    private LoggerInitializer() {

    }

    public static void initLoggerContext() {
        try {
            // 获取Log4j2配置文件
            URL configFile = LoggerInitializer.class.getClassLoader().getResource("luna-log4j2.xml");
            if (configFile != null) {
                System.out.println("[Luna] 成功找到日志配置: " + configFile);
                // 初始化Log4j2配置
                Configurator.initialize("luna-agent", configFile.toString());
                Logger logger = LogManager.getLogger(LoggerInitializer.class);
                logger.info("[Luna] Log4j2配置初始化成功");
                logger.error("[Luna] Log4j2配置初始化成功");
            } else {
                System.err.println("[Luna] 未找到 luna-log4j2.xml 配置文件，使用默认配置");
                // 使用默认配置初始化
                Configurator.initialize("luna-agent", (String) null);
            }
        } catch (Exception e) {
            System.err.println("[Luna] 初始化 logger context 失败: " + e.getMessage());
            e.printStackTrace();
        }
    }
}