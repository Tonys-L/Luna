package fun.efto.luna.core.market;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * @author : Tony.L(286269159@qq.com)
 * @since  : 2026/05/11
 */
public final class RepositoryConfig {

    private static final Logger log = Logger.getLogger(RepositoryConfig.class.getName());
    private static final Path CONFIG_DIR = Paths.get(System.getProperty("user.home"), ".luna", "config");
    private static final Path REPOS_FILE = CONFIG_DIR.resolve("plugin-repositories.json");

    private static volatile RepositoryConfig instance;

    private final List<PluginRepository> repositories = new ArrayList<>();

    private RepositoryConfig() {
        load();
    }

    public static RepositoryConfig getInstance() {
        if (instance == null) {
            synchronized (RepositoryConfig.class) {
                if (instance == null) {
                    instance = new RepositoryConfig();
                }
            }
        }
        return instance;
    }

    public List<PluginRepository> getRepositories() {
        return Collections.unmodifiableList(repositories);
    }

    public void addRepository(PluginRepository repository) {
        for (PluginRepository existing : repositories) {
            if (existing.getId().equals(repository.getId())) {
                return;
            }
        }
        repositories.add(repository);
        save();
    }

    public void removeRepository(String id) {
        boolean removed = repositories.removeIf(r -> r.getId().equals(id));
        if (removed) {
            save();
        }
    }

    public void load() {
        repositories.clear();
        if (!Files.exists(REPOS_FILE)) {
            return;
        }
        try {
            String json = new String(Files.readAllBytes(REPOS_FILE), StandardCharsets.UTF_8);
            JSONArray arr = JSON.parseArray(json);
            for (int i = 0; i < arr.size(); i++) {
                PluginRepository repo = parseRepository(arr.getJSONObject(i));
                if (repo != null) {
                    repositories.add(repo);
                }
            }
        } catch (Exception e) {
            log.log(Level.WARNING, "Failed to load repository config", e);
        }
    }

    public void save() {
        try {
            Files.createDirectories(CONFIG_DIR);
            JSONArray arr = new JSONArray();
            for (PluginRepository repo : repositories) {
                arr.add(toJSONObject(repo));
            }
            Files.write(REPOS_FILE, arr.toJSONString().getBytes(StandardCharsets.UTF_8));
        } catch (IOException e) {
            log.log(Level.WARNING, "Failed to save repository config", e);
        }
    }

    private PluginRepository parseRepository(JSONObject obj) {
        String id = obj.getString("id");
        String name = obj.getString("name");
        String url = obj.getString("url");
        boolean trusted = obj.getBooleanValue("trusted");
        PluginRepository.AuthConfig auth = null;
        JSONObject authObj = obj.getJSONObject("auth");
        if (authObj != null) {
            auth = new PluginRepository.AuthConfig(
                authObj.getString("type"),
                authObj.getString("token"),
                authObj.getString("username"),
                authObj.getString("password")
            );
        }
        return new PluginRepository(id, name, url, trusted, auth);
    }

    private JSONObject toJSONObject(PluginRepository repo) {
        JSONObject obj = new JSONObject();
        obj.put("id", repo.getId());
        obj.put("name", repo.getName());
        obj.put("url", repo.getUrl());
        obj.put("trusted", repo.isTrusted());
        if (repo.getAuth() != null) {
            JSONObject authObj = new JSONObject();
            authObj.put("type", repo.getAuth().getType());
            authObj.put("token", repo.getAuth().getToken());
            authObj.put("username", repo.getAuth().getUsername());
            authObj.put("password", repo.getAuth().getPassword());
            obj.put("auth", authObj);
        }
        return obj;
    }
}
