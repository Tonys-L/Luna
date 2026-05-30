package fun.efto.luna.core.common;

import java.util.concurrent.ConcurrentHashMap;

/**
 * 字节码缓存
 * @author : Tony.L(286269159@qq.com>)
 * @since ：2026/03/29 02:30
 */
public class BytecodeCache {
    private static final BytecodeCache INSTANCE = new BytecodeCache();
    private final ConcurrentHashMap<String, byte[]> bytecodeCache = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, Object> analysisCache = new ConcurrentHashMap<>();

    private BytecodeCache() {
    }

    public static BytecodeCache getInstance() {
        return INSTANCE;
    }

    /**
     * 缓存字节码
     */
    public void cacheBytecode(String className, byte[] bytecode) {
        bytecodeCache.put(className, bytecode);
    }

    /**
     * 获取缓存的字节码
     */
    public byte[] getCachedBytecode(String className) {
        return bytecodeCache.get(className);
    }

    /**
     * 缓存分析结果
     */
    public void cacheAnalysis(String className, Object analysisResult) {
        analysisCache.put(className, analysisResult);
    }

    /**
     * 获取缓存的分析结果
     */
    @SuppressWarnings("unchecked")
    public <T> T getCachedAnalysis(String className) {
        return (T) analysisCache.get(className);
    }

    /**
     * 清除缓存
     */
    public void clear() {
        bytecodeCache.clear();
        analysisCache.clear();
    }

    /**
     * 清除指定类的缓存
     */
    public void clear(String className) {
        bytecodeCache.remove(className);
        analysisCache.remove(className);
    }

    /**
     * 获取缓存大小
     */
    public int getSize() {
        return bytecodeCache.size();
    }
}
