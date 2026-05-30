package fun.efto.luna.core.market;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * @author ：Tony.L(286269159@qq.com)
 * @since  : 2026/05/11 22:35
 */
public class MarketClient {

    private static final Logger log = Logger.getLogger(MarketClient.class.getName());
    private static final int CONNECT_TIMEOUT = 10000;
    private static final int READ_TIMEOUT = 30000;

    private final String baseUrl;
    private final String authToken;

    public MarketClient(String baseUrl) {
        this(baseUrl, null);
    }

    public MarketClient(String baseUrl, String authToken) {
        this.baseUrl = baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
        this.authToken = authToken;
    }

    public List<PluginMetadata> search(String keyword) {
        try {
            String json = httpGet("/market/search?keyword=" + URLEncoder.encode(keyword, StandardCharsets.UTF_8.name()));
            JSONArray arr = JSON.parseArray(json);
            List<PluginMetadata> result = new ArrayList<>();
            for (int i = 0; i < arr.size(); i++) {
                PluginMetadata meta = PluginMetadata.fromMap(arr.getJSONObject(i).getInnerMap());
                if (meta != null) {
                    result.add(meta);
                }
            }
            return result;
        } catch (Exception e) {
            log.log(Level.WARNING, "Market search failed", e);
            return Collections.emptyList();
        }
    }

    public PluginMetadata getMetadata(String pluginId, String version) {
        try {
            String path = version != null
                ? "/market/plugins/" + pluginId + "/versions/" + version
                : "/market/plugins/" + pluginId;
            String json = httpGet(path);
            return PluginMetadata.fromMap(JSON.parseObject(json).getInnerMap());
        } catch (Exception e) {
            log.log(Level.WARNING, "Failed to get plugin metadata: " + pluginId, e);
            return null;
        }
    }

    public Path download(String downloadUrl, Path targetDir) throws IOException {
        Files.createDirectories(targetDir);
        Path tmpFile = Files.createTempFile(targetDir, "luna-plugin-", ".jar.tmp");

        HttpURLConnection conn = null;
        try {
            URL url = new URL(downloadUrl);
            conn = (HttpURLConnection) url.openConnection();
            conn.setConnectTimeout(CONNECT_TIMEOUT);
            conn.setReadTimeout(READ_TIMEOUT);
            if (authToken != null) {
                conn.setRequestProperty("Authorization", "Bearer " + authToken);
            }

            try (java.io.InputStream in = conn.getInputStream()) {
                Files.copy(in, tmpFile, StandardCopyOption.REPLACE_EXISTING);
            }
            return tmpFile;
        } finally {
            if (conn != null) conn.disconnect();
        }
    }

    public static String sha256(Path file) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] bytes = Files.readAllBytes(file);
            byte[] hash = digest.digest(bytes);
            StringBuilder sb = new StringBuilder();
            for (byte b : hash) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (Exception e) {
            throw new RuntimeException("SHA-256 calculation failed", e);
        }
    }

    private String httpGet(String path) throws IOException {
        HttpURLConnection conn = null;
        try {
            URL url = new URL(baseUrl + path);
            conn = (HttpURLConnection) url.openConnection();
            conn.setConnectTimeout(CONNECT_TIMEOUT);
            conn.setReadTimeout(READ_TIMEOUT);
            conn.setRequestProperty("Accept", "application/json");
            if (authToken != null) {
                conn.setRequestProperty("Authorization", "Bearer " + authToken);
            }

            int code = conn.getResponseCode();
            if (code != 200) {
                throw new IOException("HTTP " + code + " for " + path);
            }

            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8))) {
                StringBuilder sb = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    sb.append(line);
                }
                return sb.toString();
            }
        } finally {
            if (conn != null) conn.disconnect();
        }
    }
}
