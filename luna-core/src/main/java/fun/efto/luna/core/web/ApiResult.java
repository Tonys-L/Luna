package fun.efto.luna.core.web;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * @author : Tony.L(<286269159@qq.com>)
 * @since  : 2026/05/11 22:00
 */
public class ApiResult {

    private final boolean success;
    private final Object data;
    private final String message;
    private final int status;

    private ApiResult(boolean success, Object data, String message, int status) {
        this.success = success;
        this.data = data;
        this.message = message;
        this.status = status;
    }

    public static ApiResult ok(Object data) {
        return new ApiResult(true, data, null, 200);
    }

    public static ApiResult fail(String message, int status) {
        return new ApiResult(false, null, message, status);
    }

    public boolean isSuccess() {
        return success;
    }

    public Object getData() {
        return data;
    }

    public String getMessage() {
        return message;
    }

    public int getStatus() {
        return status;
    }

    public Map<String, Object> toMap() {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("success", success);
        map.put("status", status);
        if (data != null) {
            map.put("data", data);
        }
        if (message != null) {
            map.put("message", message);
        }
        return map;
    }
}
