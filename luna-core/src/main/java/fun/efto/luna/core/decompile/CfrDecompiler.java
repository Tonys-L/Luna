package fun.efto.luna.core.decompile;

import fun.efto.luna.core.instrument.InstrumentationHolder;
import org.benf.cfr.reader.api.CfrDriver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.net.URL;
import java.security.CodeSource;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.NavigableMap;
import java.util.Objects;
import java.util.Scanner;

/**
 * @author : Tony.L(<286269159@qq.com>)
 * @since  : 2025/10/19 14:12
 */
public class CfrDecompiler implements Decompiler {
    private static final Logger logger = LoggerFactory.getLogger(CfrDecompiler.class);

    @Override
    public String decompile(String className) {
        try {
            logger.debug("Starting to decompile class: {}", className);
            String initialCp = System.getProperty("java.class.path");

            String decompiledCode = doDecompile(className, initialCp);

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
        options.put("trackbytecodeloc", "true");

        SimpleOutputSinkFactory simpleOutputSinkFactory = new SimpleOutputSinkFactory();
        CfrDriver driver = new CfrDriver.Builder()
                .withOptions(options)
                .withOutputSink(simpleOutputSinkFactory)
                .build();

        driver.analyse(Collections.singletonList(target));

        if (Objects.nonNull(simpleOutputSinkFactory.getException())) {
            throw simpleOutputSinkFactory.getException();
        }

        String result = simpleOutputSinkFactory.getDecompile();
        if (result == null || result.isEmpty()) {
            return "";
        }

        NavigableMap<Integer, Integer> lineMapping = simpleOutputSinkFactory.getLineMapping();
        if (lineMapping != null && !lineMapping.isEmpty()) {
            result = applyLineNumberComments(result, lineMapping);
        }

        return result;
    }

    private String applyLineNumberComments(String src, NavigableMap<Integer, Integer> lineMapping) {
        int maxSrcLine = 0;
        for (Integer value : lineMapping.values()) {
            if (value != null && value > maxSrcLine) {
                maxSrcLine = value;
            }
        }

        String formatStr;
        if (maxSrcLine >= 1000) {
            formatStr = "/* %4d */ ";
        } else if (maxSrcLine >= 100) {
            formatStr = "/* %3d */ ";
        } else {
            formatStr = "/* %2d */ ";
        }

        StringBuilder sb = new StringBuilder();
        int index = 0;
        try (Scanner sc = new Scanner(src)) {
            while (sc.hasNextLine()) {
                String line = sc.nextLine();
                Integer srcLineNumber = lineMapping.get(index + 1);
                if (srcLineNumber != null) {
                    sb.append(String.format(formatStr, srcLineNumber));
                }
                sb.append(line).append("\n");
                index++;
            }
        }
        return sb.toString();
    }

    private Class<?> findClass(String className) {
        try {
            return Class.forName(className, false, Thread.currentThread().getContextClassLoader());
        } catch (ClassNotFoundException e) {
            try {
                return Class.forName(className, false, CfrDecompiler.class.getClassLoader());
            } catch (ClassNotFoundException e1) {
                try {
                    for (Class<?> c : InstrumentationHolder.getAllLoadedClasses()) {
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
                    File classFile = new File(root, className.replace('.', File.separatorChar) + ".class");
                    if (classFile.exists()) {
                        return new DecompileTarget(classFile.getAbsolutePath(), root.getAbsolutePath());
                    }
                }
                return new DecompileTarget(className, root.getAbsolutePath());
            }
        } catch (Throwable e) {
            logger.debug("Failed to find target via CodeSource for {}", className, e);
        }

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
                    String jarPath = path.substring(0, path.indexOf("!/"));
                    return new DecompileTarget(className, new File(jarPath).getAbsolutePath());
                } else {
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
        final String target;
        final String rootPath;

        DecompileTarget(String target, String rootPath) {
            this.target = target;
            this.rootPath = rootPath;
        }
    }
}
