package fun.efto.luna.agent.web;

import java.lang.management.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * JVM 指标采集服务
 *
 * @author : Tony.L(<286269159@qq.com>)
 * @since  : 2026/05/10 00:30
 */
public class MetricsService {

    public static Map<String, Object> getJvmMetrics() {
        Map<String, Object> metrics = new HashMap<>();

        // 1. 内存信息
        MemoryMXBean memoryMXBean = ManagementFactory.getMemoryMXBean();
        MemoryUsage heapUsage = memoryMXBean.getHeapMemoryUsage();
        MemoryUsage nonHeapUsage = memoryMXBean.getNonHeapMemoryUsage();

        Map<String, Object> memory = new HashMap<>();
        memory.put("heapUsed", heapUsage.getUsed());
        memory.put("heapMax", heapUsage.getMax());
        memory.put("heapCommitted", heapUsage.getCommitted());
        memory.put("nonHeapUsed", nonHeapUsage.getUsed());
        metrics.put("memory", memory);

        // 2. GC 信息
        List<GarbageCollectorMXBean> gcBeans = ManagementFactory.getGarbageCollectorMXBeans();
        List<Map<String, Object>> gcs = new ArrayList<>();
        for (GarbageCollectorMXBean gcBean : gcBeans) {
            Map<String, Object> gc = new HashMap<>();
            gc.put("name", gcBean.getName());
            gc.put("count", gcBean.getCollectionCount());
            gc.put("time", gcBean.getCollectionTime());
            gcs.add(gc);
        }
        metrics.put("gc", gcs);

        // 3. 线程概况
        ThreadMXBean threadMXBean = ManagementFactory.getThreadMXBean();
        Map<String, Object> threadInfo = new HashMap<>();
        threadInfo.put("count", threadMXBean.getThreadCount());
        threadInfo.put("peakCount", threadMXBean.getPeakThreadCount());
        threadInfo.put("daemonCount", threadMXBean.getDaemonThreadCount());
        metrics.put("threads", threadInfo);

        // 4. 类加载
        ClassLoadingMXBean classLoadingMXBean = ManagementFactory.getClassLoadingMXBean();
        Map<String, Object> classInfo = new HashMap<>();
        classInfo.put("loadedCount", classLoadingMXBean.getLoadedClassCount());
        classInfo.put("totalLoadedCount", classLoadingMXBean.getTotalLoadedClassCount());
        classInfo.put("unloadedCount", classLoadingMXBean.getUnloadedClassCount());
        metrics.put("classes", classInfo);

        // 5. 运行时
        RuntimeMXBean runtimeMXBean = ManagementFactory.getRuntimeMXBean();
        metrics.put("uptime", runtimeMXBean.getUptime());
        metrics.put("startTime", runtimeMXBean.getStartTime());

        return metrics;
    }

    public static List<Map<String, Object>> getThreadDump() {
        ThreadMXBean threadMXBean = ManagementFactory.getThreadMXBean();
        ThreadInfo[] threadInfos = threadMXBean.dumpAllThreads(true, true);
        List<Map<String, Object>> result = new ArrayList<>();

        for (ThreadInfo info : threadInfos) {
            Map<String, Object> item = new HashMap<>();
            item.put("id", info.getThreadId());
            item.put("name", info.getThreadName());
            item.put("state", info.getThreadState().name());
            
            // 堆栈
            StringBuilder stack = new StringBuilder();
            for (StackTraceElement ste : info.getStackTrace()) {
                stack.append(ste.toString()).append("\n");
            }
            item.put("stackTrace", stack.toString());
            
            // 锁信息
            item.put("lockName", info.getLockName());
            item.put("lockOwnerName", info.getLockOwnerName());
            
            result.add(item);
        }
        return result;
    }

    public static long[] findDeadlockedThreads() {
        ThreadMXBean threadMXBean = ManagementFactory.getThreadMXBean();
        return threadMXBean.findDeadlockedThreads();
    }
}
