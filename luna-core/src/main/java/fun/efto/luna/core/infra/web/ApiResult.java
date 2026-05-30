package fun.efto.luna.core.infra.web;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 统一 API 响应结果。
 * 字段名与前端约定一致：error（而非 message）。
 *
 * @author : Tony.L(286269159@qq.com)
 * @since  : 2026/05/11 22:00
 */
public class ApiResult {

    private final boolean success;
    private final Object data;
    private final String error;
    private final int status;

    private ApiResult(boolean success, Object data, String error, int status) {
        this.success = success;
        this.data = data;
        this.error = error;
        this.status = status;
    }

    public static ApiResult ok() {
        return new ApiResult(true, null, null, 200);
    }

    public static ApiResult ok(Object data) {
        return new ApiResult(true, data, null, 200);
    }

    public static ApiResult fail(String error) {
        return new ApiResult(false, null, error, 400);
    }

    public static ApiResult fail(String error, int status) {
        return new ApiResult(false, null, error, status);
    }

    public static ApiResult fail(String error, Object data, int status) {
        return new ApiResult(false, data, error, status);
    }

    public boolean isSuccess() {
        return success;
    }

    public Object getData() {
        return data;
    }

    public String getError() {
        return error;
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
        if (error != null) {
            map.put("error", error);
        }
        return map;
    }
}
