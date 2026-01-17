package fun.efto.luna.core.decompile;

import fun.efto.luna.core.InstrumentationManager;
import org.benf.cfr.reader.api.CfrDriver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.net.URL;
import java.security.CodeSource;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * @author ：Tony.L(286269159@qq.com)
 * @since ：2025/10/19 14:12
 */
public class CfrDecompiler implements Decompiler {
    private static final Logger logger = LoggerFactory.getLogger(CfrDecompiler.class);

    @Override
    public String decompile(String className) {
        try {
            logger.debug("Starting to decompile class: {}", className);
            String initialCp = System.getProperty("java.class.path");

            // 执行初始反编译（尝试使用系统类路径和类名）
            String decompiledCode = doDecompile(className, initialCp);

            // 如果结果为空，尝试精准定位
            if (decompiledCode.isEmpty()) {
                logger.info("Decompilation returned empty for {}, searching for better target...", className);
                Class<?> clazz = findClass(className);
                if (clazz != null) {
                    DecompileTarget target = findDecompileTarget(clazz, className);
                    if (target != null) {
                        String combinedCp = initialCp + File.pathSeparator + target.rootPath;
                        logger.info("Retrying decompile with target: {} and extra path: {}", target.target,
                                target.rootPath);
                        decompiledCode = doDecompile(target.target, combinedCp);
                    }
                }
            }

            if (decompiledCode.isEmpty()) {
                logger.warn("Decompilation returned empty result for class: {}", className);
                return "// Decompilation returned empty result for class: " + className + "\n" +
                        "// This may happen for several reasons:\n" +
                        "// 1. The class file was not found in the classpath\n" +
                        "// 2. The class has complex dependencies that CFR cannot resolve\n" +
                        "// 3. The class is an interface or abstract class with no implementation\n" +
                        "// 4. The class is a synthetic or generated class\n" +
                        "//\n" +
                        "// Current classpath: " + initialCp.substring(0, Math.min(200, initialCp.length()))
                        + (initialCp.length() > 200 ? "..." : "");
            }

            logger.debug("Decompilation completed for class: {}, result length: {}", className,
                    decompiledCode.length());
            return decompiledCode;
        } catch (Exception e) {
            logger.error("Failed to decompile class: " + className, e);
            return "// Decompilation failed for class: " + className + "\n" +
                    "// Error: " + e.getMessage();
        } catch (Throwable t) {
            logger.error("Unexpected error while decompiling class: " + className, t);
            return "// Decompilation failed for class: " + className + "\n" +
                    "// Unexpected error: " + t.getMessage();
        }
    }

    private String doDecompile(String target, String classPath) throws Throwable {
        Map<String, String> options = new HashMap<>();
        options.put("c", classPath);

        SimpleOutputSinkFactory simpleOutputSinkFactory = new SimpleOutputSinkFactory();
        CfrDriver driver = new CfrDriver.Builder()
                .withOptions(options)
                .withOutputSink(simpleOutputSinkFactory)
                .build();

        driver.analyse(Collections.singletonList(target));

        if (Objects.nonNull(simpleOutputSinkFactory.getException())) {
            throw simpleOutputSinkFactory.getException();
        }

        return simpleOutputSinkFactory.getDecompile() == null ? "" : simpleOutputSinkFactory.getDecompile();
    }

    private Class<?> findClass(String className) {
        try {
            return Class.forName(className, false, Thread.currentThread().getContextClassLoader());
        } catch (ClassNotFoundException e) {
            try {
                return Class.forName(className, false, CfrDecompiler.class.getClassLoader());
            } catch (ClassNotFoundException e1) {
                try {
                    InstrumentationManager manager = InstrumentationManager.getInstance();
                    for (Class<?> c : manager.getAllLoadedClasses()) {
                        if (c.getName().equals(className)) {
                            return c;
                        }
                    }
                } catch (Throwable ignored) {
                }
            }
        }
        return null;
    }

    private DecompileTarget findDecompileTarget(Class<?> clazz, String className) {
        try {
            CodeSource codeSource = clazz.getProtectionDomain().getCodeSource();
            if (codeSource != null && codeSource.getLocation() != null) {
                File root = new File(codeSource.getLocation().toURI());
                if (root.isDirectory()) {
                    // 如果类在目录中，通过绝对路径指定 .class 文件，这能最有效地解决目录查找问题
                    File classFile = new File(root, className.replace('.', File.separatorChar) + ".class");
                    if (classFile.exists()) {
                        return new DecompileTarget(classFile.getAbsolutePath(), root.getAbsolutePath());
                    }
                }
                // JAR 包文件
                return new DecompileTarget(className, root.getAbsolutePath());
            }
        } catch (Throwable e) {
            logger.debug("Failed to find target via CodeSource for {}", className, e);
        }

        // 备选方案：通过资源路径解析
        try {
            String resourceName = className.replace('.', '/') + ".class";
            URL url = clazz.getResource("/" + resourceName);
            if (url == null) {
                url = clazz.getClassLoader().getResource(resourceName);
            }
            if (url != null) {
                String path = url.getPath();
                if (path.startsWith("file:")) {
                    path = path.substring(5);
                }
                if (path.contains("!/")) {
                    // JAR 中的类
                    String jarPath = path.substring(0, path.indexOf("!/"));
                    return new DecompileTarget(className, new File(jarPath).getAbsolutePath());
                } else {
                    // 目录中的类
                    String rootPath = path.substring(0, path.length() - resourceName.length());
                    return new DecompileTarget(new File(path).getAbsolutePath(), new File(rootPath).getAbsolutePath());
                }
            }
        } catch (Throwable e) {
            logger.debug("Failed to find target via resource for {}", className, e);
        }
        return null;
    }

    private static class DecompileTarget {
        final String target; // 类名或文件绝对路径
        final String rootPath; // 根目录或 JAR 路径

        DecompileTarget(String target, String rootPath) {
            this.target = target;
            this.rootPath = rootPath;
        }
    }
}
