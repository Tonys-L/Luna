package fun.efto.luna.core.plugin.loader;

import fun.efto.luna.core.plugin.LunaPlugin;

import java.io.IOException;
import java.net.URL;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.ServiceLoader;
import java.util.jar.Attributes;
import java.util.jar.JarFile;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Stream;

/**
 * @author : Tony.L(<286269159@qq.com>)
 * @since  : 2026/05/11 22:35
 */
public class PluginLoader {

    private static final Logger log = Logger.getLogger(PluginLoader.class.getName());
    private static final Path PLUGINS_DIR =
        Paths.get(System.getProperty("user.home"), ".luna", "plugins");

    private final ClassLoader agentClassLoader;

    public PluginLoader(ClassLoader agentClassLoader) {
        this.agentClassLoader = agentClassLoader;
    }

    public List<LunaPlugin> discover() {
        List<LunaPlugin> plugins = new ArrayList<>();
        try {
            ServiceLoader.load(LunaPlugin.class, agentClassLoader)
                .forEach(plugins::add);
        } catch (Throwable t) {
            log.log(Level.WARNING, "ServiceLoader failed to discover built-in plugins, returning empty list", t);
        }

        if (Files.isDirectory(PLUGINS_DIR)) {
            try (Stream<Path> dirs = Files.list(PLUGINS_DIR)) {
                dirs.filter(Files::isDirectory)
                    .forEach(pluginDir -> {
                        findLatestJar(pluginDir).ifPresent(jarPath ->
                            loadFromJar(jarPath, plugins));
                    });
            } catch (IOException e) {
                log.log(Level.SEVERE, "Failed to scan plugin directory", e);
            }
        }
        return plugins;
    }

    private void loadFromJar(Path jarPath, List<LunaPlugin> out) {
        try {
            boolean isolated = isIsolatedPlugin(jarPath);
            ClassLoader cl;
            if (isolated) {
                String pluginId = jarPath.getParent().getFileName().toString();
                cl = new PluginClassLoader(pluginId,
                    new URL[]{ jarPath.toUri().toURL() },
                    agentClassLoader);
            } else {
                if (agentClassLoader instanceof LunaAgentClassLoader) {
                    ((LunaAgentClassLoader) agentClassLoader).appendUrl(jarPath.toUri().toURL());
                }
                cl = agentClassLoader;
            }
            ServiceLoader.load(LunaPlugin.class, cl).forEach(out::add);
        } catch (Throwable t) {
            log.log(Level.SEVERE, "Failed to load plugin JAR: " + jarPath, t);
        }
    }

    private boolean isIsolatedPlugin(Path jarPath) {
        try (JarFile jar = new JarFile(jarPath.toFile())) {
            Attributes attrs = jar.getManifest().getMainAttributes();
            return "true".equalsIgnoreCase(attrs.getValue("Luna-Plugin-Isolated"));
        } catch (Exception e) {
            return false;
        }
    }

    private Optional<Path> findLatestJar(Path pluginDir) {
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(pluginDir, "*.jar")) {
            Path latest = null;
            long lastModified = 0;
            for (Path jar : stream) {
                long modified = Files.getLastModifiedTime(jar).toMillis();
                if (modified > lastModified) {
                    lastModified = modified;
                    latest = jar;
                }
            }
            return Optional.ofNullable(latest);
        } catch (IOException e) {
            return Optional.empty();
        }
    }

    public static Path getPluginsDir() {
        return PLUGINS_DIR;
    }
}
