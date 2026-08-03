package fun.efto.luna.demo.service;

/**
 * @author : Tony.L(286269159@qq.com)
 * @since : 2026/07/05 19:10
 */
public class PricingService {

    public double calculateDiscount(int userAge, int orderCount) {
        simulateDelay(3);
        if (userAge > 60) {
            return 0.2;
        }
        if (orderCount > 10) {
            return 0.15;
        }
        return 0.1;
    }

    public double applyDiscount(double price, int quantity, double discount) {
        simulateDelay(2);
        return price * quantity * (1 - discount);
    }

    private void simulateDelay(int ms) {
        try {
            Thread.sleep(ms);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
