package fun.efto.luna.core.buffer;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

/**
 * MPSC RingBuffer 单元测试与并发测试
 *
 * @author : Tony.L(<286269159@qq.com>)
 * @since  : 2026/05/10 00:00
 */
public class RingBufferTest {

    @Test
    public void testBasicOfferAndPoll() {
        RingBuffer<String> buffer = new RingBuffer<>(4);
        assertTrue(buffer.offer("A"));
        assertTrue(buffer.offer("B"));
        assertEquals(2, buffer.size());
        
        assertEquals("A", buffer.poll());
        assertEquals("B", buffer.poll());
        assertNull(buffer.poll());
        assertEquals(0, buffer.size());
    }

    @Test
    public void testCapacityRounding() {
        // 容量会被对齐到最接近的 2 的幂
        RingBuffer<Integer> buffer = new RingBuffer<>(3); // -> 4
        assertTrue(buffer.offer(1));
        assertTrue(buffer.offer(2));
        assertTrue(buffer.offer(3));
        assertTrue(buffer.offer(4));
        assertFalse(buffer.offer(5), "容量应为 4");
    }

    @Test
    public void testFullDiscard() {
        RingBuffer<Integer> buffer = new RingBuffer<>(2);
        assertTrue(buffer.offer(1));
        assertTrue(buffer.offer(2));
        assertFalse(buffer.offer(3)); // 满队列丢弃
        
        assertEquals(1, buffer.poll());
        assertTrue(buffer.offer(4)); // 消费后又可以投递
        assertEquals(2, buffer.poll());
        assertEquals(4, buffer.poll());
    }

    @Test
    public void testConcurrentMpsc() throws InterruptedException {
        int capacity = 1024;
        RingBuffer<Integer> buffer = new RingBuffer<>(capacity);
        int producerCount = 4;
        int itemsPerProducer = 1000;
        
        ExecutorService executor = Executors.newFixedThreadPool(producerCount + 1);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch producerLatch = new CountDownLatch(producerCount);
        
        // 消费者线程
        List<Integer> consumed = Collections.synchronizedList(new ArrayList<>());
        AtomicInteger totalProduced = new AtomicInteger(0);
        
        executor.submit(() -> {
            try {
                startLatch.await();
                int expectedTotal = producerCount * itemsPerProducer;
                while (consumed.size() < expectedTotal) {
                    Integer item = buffer.poll();
                    if (item != null) {
                        consumed.add(item);
                    } else {
                        // 空转，实际应用中会有 sleep 或 wait 策略
                        Thread.yield();
                    }
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        });

        // 生产者线程
        for (int p = 0; p < producerCount; p++) {
            final int producerId = p;
            executor.submit(() -> {
                try {
                    startLatch.await();
                    for (int i = 0; i < itemsPerProducer; i++) {
                        int val = producerId * itemsPerProducer + i;
                        // 自旋直到投递成功
                        while (!buffer.offer(val)) {
                            Thread.yield();
                        }
                        totalProduced.incrementAndGet();
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } finally {
                    producerLatch.countDown();
                }
            });
        }

        // 开始并发
        startLatch.countDown();
        producerLatch.await();
        
        // 等待消费者完成
        int maxWaitMs = 5000;
        int expectedTotal = producerCount * itemsPerProducer;
        while (consumed.size() < expectedTotal && maxWaitMs > 0) {
            Thread.sleep(10);
            maxWaitMs -= 10;
        }

        executor.shutdownNow();

        assertEquals(expectedTotal, totalProduced.get(), "生产者应成功投递所有消息");
        assertEquals(expectedTotal, consumed.size(), "消费者应接收所有消息，没有丢失或覆盖");
    }
}
