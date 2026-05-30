package fun.efto.luna.core.plugin;

import fun.efto.luna.core.buffer.RingBuffer;
import fun.efto.luna.core.injection.port.Retransformer;
import fun.efto.luna.core.plugin.lifecycle.PluginManagerImpl;
import fun.efto.luna.core.plugin.lifecycle.ReadyGate;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.StampedLock;

import static org.junit.jupiter.api.Assertions.*;

/**
 * @author : Tony.L(<286269159@qq.com>)
 * @since  : 2026/05/11 22:00
 */
@DisplayName("StampedLock 并发控制测试")
public class ConcurrencyTest {

    private PluginManagerImpl pluginManager;
    private ReadyGate readyGate;

    @BeforeEach
    void setUp() {
        readyGate = new ReadyGate();
        pluginManager = new PluginManagerImpl(
            readyGate,
            new DefaultLogEmitter(),
            new RingBuffer<>(1024),
            (Retransformer) className -> {},
            null,
            null
        );
    }

    @Test
    @DisplayName("getTransformLock 返回 StampedLock 实例")
    void testGetTransformLockReturnsStampedLock() {
        StampedLock lock = pluginManager.getTransformLock();
        assertNotNull(lock);
    }

    @Test
    @DisplayName("读锁不阻塞其他读锁")
    void testReadLockDoesNotBlockReaders() throws InterruptedException {
        StampedLock lock = pluginManager.getTransformLock();
        int readerCount = 10;
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(readerCount);
        AtomicInteger concurrentReaders = new AtomicInteger(0);
        AtomicInteger maxConcurrentReaders = new AtomicInteger(0);

        for (int i = 0; i < readerCount; i++) {
            new Thread(() -> {
                try {
                    startLatch.await();
                    long stamp = lock.readLock();
                    try {
                        int current = concurrentReaders.incrementAndGet();
                        maxConcurrentReaders.updateAndGet(max -> Math.max(max, current));
                        Thread.sleep(50);
                    } finally {
                        concurrentReaders.decrementAndGet();
                        lock.unlockRead(stamp);
                        doneLatch.countDown();
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }).start();
        }

        startLatch.countDown();
        assertTrue(doneLatch.await(5, TimeUnit.SECONDS));
        assertTrue(maxConcurrentReaders.get() > 1, "Multiple readers should be concurrent, max was: " + maxConcurrentReaders.get());
    }

    @Test
    @DisplayName("写锁阻塞读锁")
    void testWriteLockBlocksReaders() throws InterruptedException {
        StampedLock lock = pluginManager.getTransformLock();
        long writeStamp = lock.writeLock();
        AtomicInteger readSuccess = new AtomicInteger(0);

        Thread reader = new Thread(() -> {
            try {
                long stamp = lock.tryReadLock(100, TimeUnit.MILLISECONDS);
                if (stamp != 0) {
                    readSuccess.incrementAndGet();
                    lock.unlockRead(stamp);
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        });
        reader.start();
        reader.join(200);

        assertEquals(0, readSuccess.get(), "Reader should be blocked while writer holds lock");
        lock.unlockWrite(writeStamp);
    }

    @Test
    @DisplayName("多线程并发读写不抛异常")
    void testConcurrentReadWriteNoException() throws InterruptedException {
        StampedLock lock = pluginManager.getTransformLock();
        int iterations = 100;
        ExecutorService executor = Executors.newFixedThreadPool(4);
        AtomicInteger errors = new AtomicInteger(0);

        for (int i = 0; i < iterations; i++) {
            final int idx = i;
            executor.submit(() -> {
                try {
                    if (idx % 3 == 0) {
                        long stamp = lock.writeLock();
                        try {
                            Thread.sleep(1);
                        } finally {
                            lock.unlockWrite(stamp);
                        }
                    } else {
                        long stamp = lock.readLock();
                        try {
                            Thread.sleep(1);
                        } finally {
                            lock.unlockRead(stamp);
                        }
                    }
                } catch (Exception e) {
                    errors.incrementAndGet();
                }
            });
        }

        executor.shutdown();
        assertTrue(executor.awaitTermination(5, TimeUnit.SECONDS));
        assertEquals(0, errors.get(), "No exceptions should occur during concurrent read/write");
    }
}
