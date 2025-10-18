package fun.efto.luna.attacher;


import com.sun.tools.attach.AttachNotSupportedException;
import com.sun.tools.attach.VirtualMachine;
import com.sun.tools.attach.VirtualMachineDescriptor;
import org.slf4j.Logger;

import java.io.IOException;
import java.util.*;

/**
 * @author ：Tony.L(286269159@qq.com)
 * @since ：2025/10/3 19:19
 */
public class Attacher {
    private static Logger logger; //LoggerFactory.getLogger(Attacher.class.getName());

    public static void main(String[] args) throws IOException, AttachNotSupportedException, ClassNotFoundException {
        if (args.length < 1) {
            System.err.println("请提供 agent jar 路径作为参数");
            System.exit(1);
        }
        String agentPath = "D:\\project\\my\\Luna\\luna-agent\\target\\luna-agent-1.0-SNAPSHOT.jar";//args[0];
        System.out.println("Agent jar: " + agentPath);
        VirtualMachineDescriptor current = waitUserInput();


        VirtualMachine vm = VirtualMachine.attach(current);
        try {
            Properties systemProperties = vm.getSystemProperties();
            System.out.println("JVM系统属性:" + systemProperties);
            System.out.println("加载 Agent");
            vm.loadAgent(agentPath); // 调用 agentmain 方法
            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                try {
                    vm.detach();
                } catch (Exception e) {
                }
            }));
            System.out.println("加载成功，按回车键退出...");
            new Scanner(System.in).nextLine(); // 等待用户输入
            vm.detach();
        } catch (Exception e) {
            System.err.println("加载代理失败: " + e.getMessage());
            e.printStackTrace();
        } finally {
            vm.detach();
        }
    }

    private static VirtualMachineDescriptor waitUserInput() {
        List<VirtualMachineDescriptor> list = refreshJavaProcessList();
        VirtualMachineDescriptor current;
        Scanner scanner = new Scanner(System.in);
        while (true) {
            System.out.println("请输入要附加的JVM的序号 (输入 r 刷新):");
            String s = scanner.nextLine();
            if (s == null || s.length() == 0) {
                continue;
            }
            if ("r".equals(s)) {
                refreshJavaProcessList();
                continue;
            }
            try {
                int index = Integer.parseInt(s);
                current = list.get(index - 1);
                break;
            } catch (Exception e) {
                System.out.println("输入有误,请重新输入");
            }
        }

        System.out.println(current.id() + "  " + current.displayName());
        return current;
    }

    private static List<VirtualMachineDescriptor> refreshJavaProcessList() {
        List<VirtualMachineDescriptor> list = VirtualMachine.list();
        List<VirtualMachineDescriptor> validList = new ArrayList<>();
        int i = 0;
        for (VirtualMachineDescriptor virtualMachineDescriptor : list) {
            String name = virtualMachineDescriptor.displayName();
            if (Objects.isNull(name) || name.length() == 0) {
                continue;
            }
            String[] nameArr = name.split(" ");
            validList.add(virtualMachineDescriptor);
            System.out.println(++i + ":" + virtualMachineDescriptor.id() + "  " + nameArr[0]);
        }
        return validList;
    }
}
