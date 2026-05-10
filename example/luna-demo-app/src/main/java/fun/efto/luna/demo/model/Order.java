package fun.efto.luna.demo.model;

/**
 * @author : Tony.L(<286269159@qq.com>)
 * @since : 2026/05/01 15:30
 */
public class Order {
    private Long id;
    private Long userId;
    private String product;
    private double price;
    private int quantity;
    private double total;

    public Order() {}

    public Order(Long id, Long userId, String product, double price, int quantity) {
        this.id = id;
        this.userId = userId;
        this.product = product;
        this.price = price;
        this.quantity = quantity;
        this.total = price * quantity;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
    public String getProduct() { return product; }
    public void setProduct(String product) { this.product = product; }
    public double getPrice() { return price; }
    public void setPrice(double price) { this.price = price; }
    public int getQuantity() { return quantity; }
    public void setQuantity(int quantity) { this.quantity = quantity; }
    public double getTotal() { return total; }
    public void setTotal(double total) { this.total = total; }

    @Override
    public String toString() {
        return "Order{id=" + id + ", userId=" + userId + ", product='" + product
                + "', price=" + price + ", quantity=" + quantity + ", total=" + total + "}";
    }
}
