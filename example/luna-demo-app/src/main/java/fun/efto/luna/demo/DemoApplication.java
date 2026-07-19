package fun.efto.luna.demo;

import fun.efto.luna.demo.model.Order;
import fun.efto.luna.demo.model.User;
import fun.efto.luna.demo.repository.OrderRepository;
import fun.efto.luna.demo.repository.UserRepository;
import fun.efto.luna.demo.service.OrderService;
import fun.efto.luna.demo.service.PricingService;
import fun.efto.luna.demo.service.UserService;

/**
 * Luna Agent 测试目标应用
 * 每3秒循环执行业务方法，方便观察注入效果
 *
 * @author : Tony.L(286269159@qq.com)
 * @since : 2026/05/01 15:30
 */
public class DemoApplication {
    private static final UserRepository userRepo = new UserRepository();
    private static final OrderRepository orderRepo = new OrderRepository();
    private static final PricingService pricingService = new PricingService();
    private static final UserService userService = new UserService(userRepo);
    private static final OrderService orderService = new OrderService(orderRepo, pricingService);
    private static int counter = 0;

    public static void main(String[] args) {
        System.out.println("========================================");
        System.out.println("  Luna Demo Application Started");
        System.out.println("========================================");

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            System.out.println("\n[DemoApp] Shutting down...");
            System.out.println("[DemoApp] Total users: " + userService.getUserCount());
            System.out.println("[DemoApp] Total orders: " + orderService.getOrderCount());
        }));

        while (true) {
            try {
                runBusinessCycle();
                Thread.sleep(3000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            } catch (Exception e) {
                System.err.println("[DemoApp] Error: " + e.getMessage());
                e.printStackTrace();
            }
        }
    }

    private static void runBusinessCycle() {
        counter++;
        System.out.println("\n--- Cycle #" + counter + " ---");

        User user = userService.createUser("User" + counter, 20 + counter % 50);
        User found = userService.getUser(user.getId());
        userService.updateUser(user.getId(), "Updated" + counter, 25 + counter % 30);

        Order order = orderService.createOrder(found, "Product-" + counter, 99.9 + counter);
        orderService.calculateTotal(order.getId(), 0.1);
        orderService.listOrdersByUserId(user.getId());

        System.out.println("--- Cycle #" + counter + " done ---\n");
    }
}
