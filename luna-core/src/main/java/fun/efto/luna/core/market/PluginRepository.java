package fun.efto.luna.core.market;

/**
 * @author : Tony.L(286269159@qq.com)
 * @since  : 2026/05/11
 */

public final class PluginRepository {

    private final String id;
    private final String name;
    private final String url;
    private final boolean trusted;
    private final AuthConfig auth;

    public PluginRepository(String id, String name, String url, boolean trusted, AuthConfig auth) {
        this.id = id;
        this.name = name;
        this.url = url;
        this.trusted = trusted;
        this.auth = auth;
    }

    public String getId() { return id; }
    public String getName() { return name; }
    public String getUrl() { return url; }
    public boolean isTrusted() { return trusted; }
    public AuthConfig getAuth() { return auth; }

    public static final class AuthConfig {
        private final String type;
        private final String token;
        private final String username;
        private final String password;

        public AuthConfig(String type, String token, String username, String password) {
            this.type = type;
            this.token = token;
            this.username = username;
            this.password = password;
        }

        public String getType() { return type; }
        public String getToken() { return token; }
        public String getUsername() { return username; }
        public String getPassword() { return password; }
    }
}
