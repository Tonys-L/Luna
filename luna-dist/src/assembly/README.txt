============================================
  Luna 动态字节码注入框架
============================================

一、目录结构
--------------------------------------------
luna-1.0-SNAPSHOT/
├── bin/
│   ├── demo.bat           Windows 启动 demo（启动时挂载 agent）
│   ├── demo.sh            Linux/macOS 启动 demo（启动时挂载 agent）
│   ├── attach.bat         Windows 动态 attach 模式（启动 demo + attacher 工具）
│   └── attach.sh          Linux/macOS 动态 attach 模式（启动 demo + attacher 工具）
├── lib/
│   ├── luna-agent-1.0-SNAPSHOT.jar          Java Agent（fat jar）
│   ├── luna-attacher-1.0-SNAPSHOT.jar       Attach 工具（fat jar）
│   └── luna-demo-app-1.0-SNAPSHOT.jar       示例应用
└── README.txt


二、环境要求
--------------------------------------------
- JDK 8 或更高版本
- JAVA_HOME 环境变量指向 JDK 安装目录（JDK 8 attach 模式必需）
- java 命令在 PATH 中


三、两种使用模式
--------------------------------------------

【模式 1：启动时挂载】（推荐用于体验）
  Agent 在应用启动时就挂载，能监控从启动开始的完整生命周期。

  Windows:
    bin\demo.bat

  Linux/macOS:
    sh bin/demo.sh

  启动后访问: http://localhost:8421


【模式 2：运行时动态 attach】
  应用已经在运行，通过 Attach API 把 Agent 动态挂载进去。
  适用于不想重启应用、或想 attach 到任意正在运行的 Java 进程的场景。

  使用步骤:
  1. 先启动目标应用（例如 java -jar lib/luna-demo-app-1.0-SNAPSHOT.jar）
  2. 运行 attach 脚本，attacher 会列出本机所有 Java 进程
  3. 选择目标进程编号，回车后 agent 自动挂载

  Windows:
    bin\attach.bat

  Linux/macOS:
    sh bin/attach.sh


四、挂载到自己的应用
--------------------------------------------

方式 1：启动时挂载
  java -javaagent:lib/luna-agent-1.0-SNAPSHOT.jar -jar your-app.jar

方式 2：运行时 attach（JDK 9+）
  java -jar lib/luna-attacher-1.0-SNAPSHOT.jar lib/luna-agent-1.0-SNAPSHOT.jar

方式 3：运行时 attach（JDK 8，需要 tools.jar）
  java -Xbootclasspath/a:%JAVA_HOME%\lib\tools.jar -jar lib/luna-attacher-1.0-SNAPSHOT.jar lib/luna-agent-1.0-SNAPSHOT.jar


五、JDK 版本说明
--------------------------------------------
- JDK 9+：Attach API 由 jdk.attach 模块提供，开箱即用
- JDK 8：Attach API 位于 tools.jar，attach 脚本会自动检测并加载
  （需要 JAVA_HOME 正确指向 JDK 安装目录，JRE 不含 tools.jar）


六、常见问题
--------------------------------------------

Q: attach 时报 "AttachNotSupportedException"？
A: 检查目标 JVM 是否禁用了 attach 机制（-XX:+DisableAttachMechanism）。

Q: JDK 8 下 attach 报 "ClassNotFoundException: com.sun.tools.attach.VirtualMachine"？
A: 确认 JAVA_HOME 指向 JDK 而非 JRE，且 %JAVA_HOME%\lib\tools.jar 文件存在。

Q: 启动后浏览器没自动打开？
A: 手动访问 http://localhost:8421
