package fun.efto.luna.demo.service;

import fun.efto.luna.demo.model.Order;
import fun.efto.luna.demo.model.User;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * @author : Tony.L(<286269159@qq.com>)
 * @since : 2026/05/01 15:30
 */
public class OrderService {
    private final Map<Long, Order> orderStore = new ConcurrentHashMap<>();
    private final AtomicLong idGenerator = new AtomicLong(1);

    public Order createOrder(User user, String product, double price) {
        long id = idGenerator.getAndIncrement();
        Order order = new Order(id, user.getId(), product, price, 1);
        orderStore.put(id, order);
        System.out.println("[OrderService] Created order: " + order);
        return order;
    }

    public Order getOrder(Long id) {
        Order order = orderStore.get(id);
        if (order == null) {
            System.out.println("[OrderService] Order not found: id=" + id);
            return null;
        }
        System.out.println("[OrderService] Found order: " + order);
        return order;
    }

    public double calculateTotal(Long orderId, double discount) {
        Order order = orderStore.get(orderId);
        if (order == null) {
            System.out.println("[OrderService] Order not found for calculation: id=" + orderId);
            return 0.0;
        }
        double total = order.getPrice() * order.getQuantity() * (1 - discount);
        order.setTotal(total);
        System.out.println("[OrderService] Calculated total: " + total + " (discount=" + discount + ")");
        return total;
    }

    public List<Order> listOrdersByUserId(Long userId) {
        List<Order> result = new ArrayList<>();
        for (Order order : orderStore.values()) {
            if (order.getUserId().equals(userId)) {
                result.add(order);
            }
        }
        System.out.println("[OrderService] Listed " + result.size() + " orders for userId=" + userId);
        return result;
    }

    public int getOrderCount() {
        return orderStore.size();
    }
}
