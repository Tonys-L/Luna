package fun.efto.luna.core.infra;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * @author ：Tony.L(286269159@qq.com)
 * @since  : 2026/05/11 22:00
 */
class BytecodeCacheTest {

    private BytecodeCache cache;

    @BeforeEach
    void setUp() {
        cache = BytecodeCache.getInstance();
        cache.clear();
    }

    @Test
    void testPutAndGet() {
        byte[] bytecode = {1, 2, 3, 4};
        cache.cacheBytecode("com.example.TestClass", bytecode);

        byte[] result = cache.getCachedBytecode("com.example.TestClass");
        assertArrayEquals(bytecode, result);
    }

    @Test
    void testRemove() {
        byte[] bytecode = {5, 6, 7};
        cache.cacheBytecode("com.example.RemoveMe", bytecode);

        assertNotNull(cache.getCachedBytecode("com.example.RemoveMe"));
        cache.clear("com.example.RemoveMe");
        assertNull(cache.getCachedBytecode("com.example.RemoveMe"));
    }

    @Test
    void testContains() {
        assertNull(cache.getCachedBytecode("com.example.NotExist"));

        cache.cacheBytecode("com.example.Exist", new byte[]{1});
        assertNotNull(cache.getCachedBytecode("com.example.Exist"));
    }
}
