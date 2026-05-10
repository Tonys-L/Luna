package fun.efto.luna.agent.web.mvc;

/**
 * @author : Tony.L(<286269159@qq.com>)
 * @since  : 2026/05/09 10:00
 */
public class ApiResult {
    private boolean success;
    private Object data;
    private String error;
    private int status = 200;

    public ApiResult() {}

    public ApiResult(boolean success, Object data, String error, int status) {
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

    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    public Object getData() {
        return data;
    }

    public void setData(Object data) {
        this.data = data;
    }

    public String getError() {
        return error;
    }

    public void setError(String error) {
        this.error = error;
    }

    public int getStatus() {
        return status;
    }

    public void setStatus(int status) {
        this.status = status;
    }
}
