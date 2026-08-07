package fun.efto.luna.core.analysis.decompile;

import fun.efto.luna.core.injection.port.BytecodeLoader;
import org.benf.cfr.reader.api.CfrDriver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileOutputStream;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.NavigableMap;
import java.util.Objects;
import java.util.Scanner;

/**
 * 基于 CFR 的反编译器。
 *
 * <p>反编译策略（参考 arthas 的 JadDecompiler）：
 * 通过 {@link BytecodeLoader} 从 JVM 获取字节码，写临时 .class 文件后传文件路径给 CFR。
 * 此方式不依赖 java.class.path，能处理 Spring Boot fat jar、自定义 ClassLoader、动态代理等场景。
 *
 * <p>{@link BytecodeLoader} 内部实现两层策略：
 * <ol>
 *   <li>{@code ClassLoader.getResourceAsStream}（轻量，无需 retransform）</li>
 *   <li>{@code retransformClasses}（JVM 内存直取，最可靠，参考 arthas）</li>
 * </ol>
 *
 * @author : Tony.L(286269159@qq.com)
 * @since  : 2025/10/19 14:12
 */
public class CfrDecompiler implements Decompiler {
    private static final Logger logger = LoggerFactory.getLogger(CfrDecompiler.class);

    private final BytecodeLoader bytecodeLoader;

    public CfrDecompiler(BytecodeLoader bytecodeLoader) {
        this.bytecodeLoader = bytecodeLoader;
    }

    @Override
    public String decompile(String className) {
        try {
            logger.debug("Starting to decompile class: {}", className);

            if (bytecodeLoader == null) {
                return "// Decompiler not available: BytecodeLoader is null\n" +
                        "// Class: " + className;
            }

            String decompiledCode = decompileViaBytecodeDump(className);

            if (decompiledCode.isEmpty()) {
                logger.warn("Decompilation returned empty result for class: {}", className);
                return "// Decompilation returned empty result for class: " + className + "\n" +
                        "// This may happen for several reasons:\n" +
                        "// 1. The class file was not found in the classpath\n" +
                        "// 2. The class has complex dependencies that CFR cannot resolve\n" +
                        "// 3. The class is an interface or abstract class with no implementation\n" +
                        "// 4. The class is a synthetic or generated class\n";
            }

            logger.debug("Decompilation completed for class: {}, result length: {}",
                    className, decompiledCode.length());
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

    /**
     * 参考 arthas 的 JadDecompiler：通过 BytecodeLoader 获取 JVM 内存中的字节码，
     * 写到临时 .class 文件，然后传文件路径给 CFR 反编译。
     *
     * @param className 全限定类名
     * @return 反编译结果，失败时返回空字符串
     */
    private String decompileViaBytecodeDump(String className) {
        File tempFile = null;
        try {
            byte[] bytes = bytecodeLoader.loadBytecode(className);
            if (bytes == null || bytes.length == 0) {
                logger.debug("BytecodeLoader returned empty for {}", className);
                return "";
            }

            // 写到临时文件，保持包结构目录（CFR 可从同目录查找内部类）
            String relativePath = className.replace('.', File.separatorChar) + ".class";
            tempFile = new File(System.getProperty("java.io.tmpdir"), "luna-decompile-" + System.nanoTime()
                    + File.separator + relativePath);
            tempFile.getParentFile().mkdirs();

            try (FileOutputStream fos = new FileOutputStream(tempFile)) {
                fos.write(bytes);
            }

            // 把临时目录根作为 classpath，让 CFR 能找到同目录的内部类等
            String tempRoot = tempFile.getParentFile().getParentFile().getAbsolutePath();
            logger.debug("Decompiling via bytecode dump: {} (temp: {}, classpath: {})",
                    className, tempFile.getAbsolutePath(), tempRoot);
            return doDecompile(tempFile.getAbsolutePath(), tempRoot);
        } catch (Throwable e) {
            logger.debug("Bytecode dump decompile failed for {}: {}", className, e.getMessage());
            return "";
        } finally {
            if (tempFile != null) {
                deleteTempFile(tempFile);
            }
        }
    }

    /**
     * 删除临时文件及其空父目录。
     */
    private void deleteTempFile(File file) {
        try {
            if (file.exists()) {
                file.delete();
            }
            File parent = file.getParentFile();
            // 向上清理空的临时目录（直到 java.io.tmpdir 下的 luna-decompile-* 目录）
            while (parent != null && parent.isDirectory() && parent.getName().startsWith("luna-decompile-")) {
                if (parent.list() != null && parent.list().length == 0) {
                    parent.delete();
                    parent = parent.getParentFile();
                } else {
                    break;
                }
            }
        } catch (Throwable ignored) {
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
}
