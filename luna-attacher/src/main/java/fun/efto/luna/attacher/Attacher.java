package fun.efto.luna.attacher;


import com.sun.tools.attach.AttachNotSupportedException;
import com.sun.tools.attach.VirtualMachine;
import com.sun.tools.attach.VirtualMachineDescriptor;
import org.slf4j.Logger;

import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.net.HttpURLConnection;
import java.net.URL;
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
        String agentPath = args[0];
        System.out.println("Agent jar: " + agentPath);
        VirtualMachineDescriptor current = waitUserInput();


        VirtualMachine vm = VirtualMachine.attach(current);
        try {
            Properties systemProperties = vm.getSystemProperties();
            System.out.println("JVM系统属性:" + systemProperties);
            System.out.println("加载 Agent");
            vm.loadAgent(agentPath);
            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                shutdownAgent();
                try {
                    vm.detach();
                } catch (Exception e) {
                }
            }));
            System.out.println("加载成功，按回车键退出...");
            new Scanner(System.in).nextLine();
            vm.detach();
        } catch (IOException e) {
            String errorMessage = e.getMessage();
            if (errorMessage.contains("Non-numeric")) {
                System.err.println("当前jdk版本低于目标jdk版本");
                System.out.println("加载成功，按回车键退出...");
                new Scanner(System.in).nextLine();
            }
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
        // 当前 attacher 进程 PID（兼容 JDK 8+）
        String currentPid = ManagementFactory.getRuntimeMXBean().getName().split("@")[0];
        List<VirtualMachineDescriptor> list = VirtualMachine.list();
        List<VirtualMachineDescriptor> validList = new ArrayList<>();
        int i = 0;
        for (VirtualMachineDescriptor virtualMachineDescriptor : list) {
            // 排除 attacher 自身进程
            if (currentPid.equals(virtualMachineDescriptor.id())) {
                continue;
            }
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

    /**
     * 远程调用 agent 的 /shutdown 接口，停止 agent 的 HTTP 服务以释放 8421 端口。
     * 在 attacher 退出时（含 Ctrl+C）触发。agent 未启动或已退出时忽略异常。
     */
    private static void shutdownAgent() {
        HttpURLConnection conn = null;
        try {
            URL url = new URL("http://localhost:8421/api/shutdown");
            conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("POST");
            conn.setConnectTimeout(2000);
            conn.setReadTimeout(2000);
            conn.getResponseCode();
        } catch (Exception e) {
            // agent 未启动或已退出，忽略
        } finally {
            if (conn != null) {
                conn.disconnect();
            }
        }
    }
}
