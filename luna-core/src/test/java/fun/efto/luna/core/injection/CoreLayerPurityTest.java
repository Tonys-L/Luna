/**
 * @author : Tony.L(<286269159@qq.com>)
 * @since  : 2026/05/31 23:45
 */
package fun.efto.luna.core.injection;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

public class CoreLayerPurityTest {

    private static final String[] FORBIDDEN_IMPORTS = {
            "com.alibaba.bytekit.",
            "org.objectweb.asm."
    };

    private static final String[] SCANNED_PACKAGES = {
            "fun/efto/luna/core/injection",
            "fun/efto/luna/core/bytecode"
    };

    private List<Path> findSourceFiles() throws IOException {
        String classpath = System.getProperty("java.class.path");
        String[] entries = classpath.split(System.getProperty("path.separator"));

        List<Path> sourceDirs = new ArrayList<>();
        for (String entry : entries) {
            if (entry.contains("luna-core") && entry.contains("classes")) {
                Path parent = Paths.get(entry).getParent();
                Path javaBase = parent.resolve("sources");
                if (Files.isDirectory(javaBase)) {
                    sourceDirs.add(javaBase);
                }
            }
        }

        List<Path> javaFiles = new ArrayList<>();
        for (Path sourceDir : sourceDirs) {
            for (String pkg : SCANNED_PACKAGES) {
                Path pkgDir = sourceDir.resolve(pkg);
                if (Files.isDirectory(pkgDir)) {
                    Files.walkFileTree(pkgDir, new SimpleFileVisitor<Path>() {
                        @Override
                        public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) {
                            if (file.toString().endsWith(".java")) {
                                javaFiles.add(file);
                            }
                            return FileVisitResult.CONTINUE;
                        }
                    });
                }
            }
        }

        if (javaFiles.isEmpty()) {
            ClassLoader cl = CoreLayerPurityTest.class.getClassLoader();
            for (String pkg : SCANNED_PACKAGES) {
                Enumeration<URL> resources = cl.getResources(pkg);
                while (resources.hasMoreElements()) {
                    URL url = resources.nextElement();
                    if (url.getProtocol().equals("file")) {
                        try {
                            Path pkgPath = Paths.get(url.toURI());
                            Path srcRoot = findSourceRoot(pkgPath, pkg);
                            if (srcRoot != null && Files.isDirectory(srcRoot)) {
                                Path srcPkgDir = srcRoot.resolve(pkg);
                                if (Files.isDirectory(srcPkgDir)) {
                                    Files.walkFileTree(srcPkgDir, new SimpleFileVisitor<Path>() {
                                        @Override
                                        public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) {
                                            if (file.toString().endsWith(".java")) {
                                                javaFiles.add(file);
                                            }
                                            return FileVisitResult.CONTINUE;
                                        }
                                    });
                                }
                            }
                        } catch (java.net.URISyntaxException e) {
                            throw new IOException("Invalid URI: " + url, e);
                        }
                    }
                }
            }
        }

        return javaFiles;
    }

    private Path findSourceRoot(Path classPath, String pkg) {
        Path current = classPath;
        for (int i = 0; i < pkg.split("/").length; i++) {
            current = current.getParent();
        }
        Path srcMain = current;
        if (Files.isDirectory(srcMain)) {
            return srcMain;
        }
        return null;
    }

    private List<String> findForbiddenImports(Path javaFile) throws IOException {
        List<String> violations = new ArrayList<>();
        List<String> lines = Files.readAllLines(javaFile);
        for (String line : lines) {
            String trimmed = line.trim();
            if (trimmed.startsWith("import ")) {
                for (String forbidden : FORBIDDEN_IMPORTS) {
                    if (trimmed.contains(forbidden)) {
                        violations.add(trimmed);
                    }
                }
            }
        }
        return violations;
    }

    @Test
    @DisplayName("核心层 injection 和 bytecode 包无 ByteKit/ASM import")
    void testCoreLayerHasNoByteKitOrAsmImports() throws Exception {
        List<Path> sourceFiles = findSourceFiles();

        if (sourceFiles.isEmpty()) {
            Path projectRoot = Paths.get(System.getProperty("user.dir"));
            if (!projectRoot.getFileName().toString().equals("luna-core")) {
                projectRoot = projectRoot.resolve("luna-core");
            }
            Path srcMainJava = projectRoot.resolve("src/main/java");

            for (String pkg : SCANNED_PACKAGES) {
                Path pkgDir = srcMainJava.resolve(pkg);
                if (Files.isDirectory(pkgDir)) {
                    Files.walkFileTree(pkgDir, new SimpleFileVisitor<Path>() {
                        @Override
                        public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) {
                            if (file.toString().endsWith(".java")) {
                                sourceFiles.add(file);
                            }
                            return FileVisitResult.CONTINUE;
                        }
                    });
                }
            }
        }

        assertFalse(sourceFiles.isEmpty(), "Should find at least one source file in core layer packages");

        List<String> allViolations = new ArrayList<>();
        for (Path file : sourceFiles) {
            List<String> violations = findForbiddenImports(file);
            for (String violation : violations) {
                allViolations.add(file.getFileName() + ": " + violation);
            }
        }

        assertTrue(allViolations.isEmpty(),
                "Core layer packages must not import ByteKit or ASM. Violations:\n" +
                        String.join("\n", allViolations));
    }
}
