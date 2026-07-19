package fun.efto.luna.demo.service;

import fun.efto.luna.demo.model.Order;
import fun.efto.luna.demo.model.User;
import fun.efto.luna.demo.repository.OrderRepository;

import java.util.List;

/**
 * @author : Tony.L(286269159@qq.com)
 * @since : 2026/05/01 15:30
 */
public class OrderService {
    private final OrderRepository orderRepo;
    private final PricingService pricingService;

    public OrderService(OrderRepository orderRepo, PricingService pricingService) {
        this.orderRepo = orderRepo;
        this.pricingService = pricingService;
    }

    public Order createOrder(User user, String product, double price) {
        Order order = new Order(null, user.getId(), product, price, 1);
        double discount = pricingService.calculateDiscount(user.getAge(), orderRepo.count());
        order.setTotal(pricingService.applyDiscount(price, 1, discount));
        orderRepo.save(order);
        System.out.println("[OrderService] Created order: " + order);
        return order;
    }

    public Order getOrder(Long id) {
        Order order = orderRepo.findById(id);
        if (order == null) {
            System.out.println("[OrderService] Order not found: id=" + id);
        } else {
            System.out.println("[OrderService] Found order: " + order);
        }
        return order;
    }

    public double calculateTotal(Long orderId, double discount) {
        Order order = orderRepo.findById(orderId);
        if (order == null) {
            System.out.println("[OrderService] Order not found for calculation: id=" + orderId);
            return 0.0;
        }
        double total = pricingService.applyDiscount(order.getPrice(), order.getQuantity(), discount);
        order.setTotal(total);
        System.out.println("[OrderService] Calculated total: " + total + " (discount=" + discount + ")");
        return total;
    }

    public List<Order> listOrdersByUserId(Long userId) {
        List<Order> result = orderRepo.findByUserId(userId);
        System.out.println("[OrderService] Listed " + result.size() + " orders for userId=" + userId);
        return result;
    }

    public int getOrderCount() {
        return orderRepo.count();
    }
}
