package com.yunweibang.auth.common;

/**
 * json response返回对象
 *
 * @author lpp
 * @since 2017-08-17 15:22:18
 */
public class JsonResponse<T> {

    private Integer code = 200;
    private String message = "操作成功";
    private T result;

    public JsonResponse() {
    }

    public JsonResponse(T result) {
        this.result = result;
    }

    public JsonResponse(Integer code, String message, T result) {
        this.code = code;
        this.message = message;
        this.result = result;
    }

    public Integer getCode() {
        return code;
    }

    public void setCode(Integer code) {
        this.code = code;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public T getResult() {
        return result;
    }

    public void setResult(T result) {
        this.result = result;
    }

}
