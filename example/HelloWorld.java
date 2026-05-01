/**
 * 示例应用
 * @author ：Tony.L(<286269159@qq.com>)
 * @since ：2026/03/29 02:30
 */
public class HelloWorld {
    public static void main(String[] args) {
        HelloWorld helloWorld = new HelloWorld();
        helloWorld.sayHello("Luna");
        helloWorld.calculate(10, 5);
    }

    public void sayHello(String name) {
        System.out.println("Hello, " + name + "!");
        // 这是第15行，用于演示行号级注入
        System.out.println("Welcome to Luna framework!");
    }

    public int calculate(int a, int b) {
        int sum = a + b;
        int difference = a - b;
        int product = a * b;
        int quotient = a / b;
        
        System.out.println("Sum: " + sum);
        System.out.println("Difference: " + difference);
        System.out.println("Product: " + product);
        System.out.println("Quotient: " + quotient);
        
        return sum;
    }
}
