package fun.efto.luna.core.buffer;

import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReferenceArray;

/**
 * 轻量级无锁多生产者单消费者（MPSC）RingBuffer。
 * 替代 Disruptor，用于高性能日志投递，确保业务线程（生产者）在任何情况下都不被阻塞。
 *
 * @author : Tony.L(<286269159@qq.com>)
 * @since  : 2026/05/10 00:00
 */
public class RingBuffer<T> {

    private final AtomicReferenceArray<T> buffer;
    private final int mask;
    
    // 使用 AtomicLong 避免并发更新冲突。实际生产中可通过 CacheLine 填充避免伪共享。
    private final AtomicLong writeIndex = new AtomicLong(0);
    private final AtomicLong readIndex = new AtomicLong(0);

    /**
     * @param capacity 容量，会自动向上取整为 2 的幂次方
     */
    public RingBuffer(int capacity) {
        int cap = 1;
        while (cap < capacity) {
            cap <<= 1;
        }
        this.buffer = new AtomicReferenceArray<>(cap);
        this.mask = cap - 1;
    }

    /**
     * 非阻塞投递消息（生产者使用）
     *
     * @param item 消息
     * @return 成功返回 true，队列满则直接丢弃并返回 false（保护业务不被 OOM 或挂起）
     */
    public boolean offer(T item) {
        if (item == null) {
            throw new NullPointerException("Item cannot be null");
        }
        
        long currentWrite;
        long currentRead;
        do {
            currentWrite = writeIndex.get();
            currentRead = readIndex.get();
            // 检查是否已满
            if (currentWrite - currentRead >= buffer.length()) {
                return false;
            }
        } while (!writeIndex.compareAndSet(currentWrite, currentWrite + 1));

        // CAS 成功，占据了插槽，此时再写入真实数据
        // 使用 lazySet 因为消费端会轮询，最终可见即可，减少内存屏障开销
        buffer.lazySet((int) (currentWrite & mask), item);
        return true;
    }

    /**
     * 消费消息（单消费者使用）
     *
     * @return 如果为空则返回 null
     */
    public T poll() {
        long currentRead = readIndex.get();
        if (currentRead >= writeIndex.get()) {
            return null; // 当前为空
        }
        
        int index = (int) (currentRead & mask);
        T item = buffer.get(index);
        if (item == null) {
            // 生产者占据了插槽但尚未写入数据，此时直接返回 null 让消费者下次再试
            return null;
        }
        
        // 读取成功，清空插槽防内存泄漏
        buffer.lazySet(index, null);
        // 单消费者安全地推进游标
        readIndex.lazySet(currentRead + 1);
        
        return item;
    }

    /**
     * 获取队列当前积压数量
     */
    public int size() {
        long size = writeIndex.get() - readIndex.get();
        if (size < 0) return 0;
        if (size > buffer.length()) return buffer.length();
        return (int) size;
    }
}
