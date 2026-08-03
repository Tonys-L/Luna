package fun.efto.luna.demo.repository;

import fun.efto.luna.demo.model.Order;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * @author : Tony.L(286269159@qq.com)
 * @since : 2026/07/05 19:10
 */
public class OrderRepository {
    private final Map<Long, Order> store = new ConcurrentHashMap<>();
    private final AtomicLong idGenerator = new AtomicLong(1);

    public Order save(Order order) {
        simulateDelay(2);
        if (order.getId() == null) {
            order.setId(idGenerator.getAndIncrement());
        }
        store.put(order.getId(), order);
        return order;
    }

    public Order findById(Long id) {
        simulateDelay(1);
        return store.get(id);
    }

    public List<Order> findByUserId(Long userId) {
        simulateDelay(2);
        List<Order> result = new ArrayList<>();
        for (Order order : store.values()) {
            if (order.getUserId().equals(userId)) {
                result.add(order);
            }
        }
        return result;
    }

    public int count() {
        return store.size();
    }

    private void simulateDelay(int ms) {
        try {
            Thread.sleep(ms);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
