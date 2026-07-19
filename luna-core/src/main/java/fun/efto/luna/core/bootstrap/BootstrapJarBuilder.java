package fun.efto.luna.core.bootstrap;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Enumeration;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.jar.JarOutputStream;

/**
 * Bootstrap JAR 构建器：从 Agent JAR 中提取注入字节码所需的类，
 * 创建一个独立的 Bootstrap JAR，仅包含这些类。
 * <p>
 * 根因分析：appendToBootstrapClassLoaderSearch(agentJar) 会将整个 Agent JAR
 * （包含 relocated Log4j2: fun.efto.luna.shadow.log4j2.*）加入 Bootstrap CL。
 * Log4j2 的 PluginType.getPluginClass() 是懒加载的，使用 getClass().getClassLoader()。
 * 如果 PluginType 被 Bootstrap CL 加载，则 Lookup 插件类也由 Bootstrap CL 加载，
 * 而 Interpolator 的 StrLookup.class 由 LunaAgentClassLoader 加载，
 * 导致 asSubclass() 抛出 ClassCastException。
 * <p>
 * 解决方案：只将注入字节码引用的类放入 Bootstrap JAR，排除所有 shadow 类，
 * 确保 Bootstrap CL 永远不会看到 Log4j2 类，从根本上消除 ClassLoader 分裂。
 *
 * @author : Tony.L(286269159@qq.com)
 * @since  : 2026/06/02 10:00
 */
public final class BootstrapJarBuilder {

    /**
     * 需要包含在 Bootstrap JAR 中的包前缀。
     * probe/ 包含所有探针运行时类（LogProbe、TraceProbe、SnapshotProbe、InvocationTraceProbe、
     * RingBuffer、StackFrameCapture、SnapshotSerializer 等），expression/ 包含表达式引擎类。
     * 这些类被注入字节码引用，必须由 Bootstrap CL 加载。
     */
    private static final String[] INCLUDE_PREFIXES = {
            "fun/efto/luna/core/probe/",
            "fun/efto/luna/core/expression/",
    };

    /** 需要从包含列表中排除的包前缀（有 ASM 依赖，不需要在 Bootstrap CL） */
    private static final String[] EXCLUDE_PREFIXES = {
            "fun/efto/luna/core/expression/bytecode/",
    };

    private BootstrapJarBuilder() {
    }

    /**
     * 从 Agent JAR 构建 Bootstrap JAR
     *
     * @param agentJarPath Agent JAR 的路径
     * @return Bootstrap JAR 的临时文件路径
     * @throws IOException 如果读取或写入 JAR 失败
     */
    public static Path build(String agentJarPath) throws IOException {
        Path tempJar = Files.createTempFile("luna-bootstrap-", ".jar");
        tempJar.toFile().deleteOnExit();

        try (JarFile agentJar = new JarFile(agentJarPath);
             JarOutputStream jos = new JarOutputStream(Files.newOutputStream(tempJar))) {

            Enumeration<JarEntry> entries = agentJar.entries();
            while (entries.hasMoreElements()) {
                JarEntry entry = entries.nextElement();
                String name = entry.getName();

                if (isBootstrapClass(name)) {
                    JarEntry newEntry = new JarEntry(name);
                    jos.putNextEntry(newEntry);
                    try (InputStream is = agentJar.getInputStream(entry)) {
                        byte[] buffer = new byte[8192];
                        int len;
                        while ((len = is.read(buffer)) != -1) {
                            jos.write(buffer, 0, len);
                        }
                    }
                    jos.closeEntry();
                }
            }
        }

        return tempJar;
    }

    private static boolean isBootstrapClass(String entryName) {
        if (!entryName.endsWith(".class")) {
            return false;
        }

        // 检查排除列表
        for (String exclude : EXCLUDE_PREFIXES) {
            if (entryName.startsWith(exclude)) {
                return false;
            }
        }

        // 检查包前缀包含列表（probe/ 覆盖所有探针运行时类）
        for (String prefix : INCLUDE_PREFIXES) {
            if (entryName.startsWith(prefix)) {
                return true;
            }
        }

        return false;
    }
}
