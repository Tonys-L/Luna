package fun.efto.luna.core.market;

import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * @author : Tony.L(<286269159@qq.com>)
 * @since  : 2026/05/11
 */
public final class PluginMetadata {

    private final String id;
    private final String displayName;
    private final String version;
    private final String author;
    private final String category;
    private final String description;
    private final List<String> dependencies;
    private final String minLunaVersion;
    private final String maxLunaVersion;
    private final String downloadUrl;
    private final String checksum;
    private final long size;
    private final String license;
    private final String repository;
    private final List<String> tags;
    private final double ratings;
    private final int downloads;

    public PluginMetadata(String id, String displayName, String version, String author,
                          String category, String description, List<String> dependencies,
                          String minLunaVersion, String maxLunaVersion, String downloadUrl,
                          String checksum, long size, String license, String repository,
                          List<String> tags, double ratings, int downloads) {
        this.id = id;
        this.displayName = displayName;
        this.version = version;
        this.author = author;
        this.category = category;
        this.description = description;
        this.dependencies = dependencies != null ? dependencies : Collections.emptyList();
        this.minLunaVersion = minLunaVersion;
        this.maxLunaVersion = maxLunaVersion;
        this.downloadUrl = downloadUrl;
        this.checksum = checksum;
        this.size = size;
        this.license = license;
        this.repository = repository;
        this.tags = tags != null ? tags : Collections.emptyList();
        this.ratings = ratings;
        this.downloads = downloads;
    }

    @SuppressWarnings("unchecked")
    public static PluginMetadata fromMap(Map<String, Object> map) {
        if (map == null || map.isEmpty()) {
            return null;
        }
        return new PluginMetadata(
            getString(map, "id"),
            getString(map, "displayName"),
            getString(map, "version"),
            getString(map, "author"),
            getString(map, "category"),
            getString(map, "description"),
            map.get("dependencies") instanceof List ? (List<String>) map.get("dependencies") : null,
            getString(map, "minLunaVersion"),
            getString(map, "maxLunaVersion"),
            getString(map, "downloadUrl"),
            getString(map, "checksum"),
            map.get("size") instanceof Number ? ((Number) map.get("size")).longValue() : 0L,
            getString(map, "license"),
            getString(map, "repository"),
            map.get("tags") instanceof List ? (List<String>) map.get("tags") : null,
            map.get("ratings") instanceof Number ? ((Number) map.get("ratings")).doubleValue() : 0.0,
            map.get("downloads") instanceof Number ? ((Number) map.get("downloads")).intValue() : 0
        );
    }

    private static String getString(Map<String, Object> map, String key) {
        Object val = map.get(key);
        return val != null ? val.toString() : null;
    }

    public String getId() { return id; }
    public String getDisplayName() { return displayName; }
    public String getVersion() { return version; }
    public String getAuthor() { return author; }
    public String getCategory() { return category; }
    public String getDescription() { return description; }
    public List<String> getDependencies() { return dependencies; }
    public String getMinLunaVersion() { return minLunaVersion; }
    public String getMaxLunaVersion() { return maxLunaVersion; }
    public String getDownloadUrl() { return downloadUrl; }
    public String getChecksum() { return checksum; }
    public long getSize() { return size; }
    public String getLicense() { return license; }
    public String getRepository() { return repository; }
    public List<String> getTags() { return tags; }
    public double getRatings() { return ratings; }
    public int getDownloads() { return downloads; }
}
