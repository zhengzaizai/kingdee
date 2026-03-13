package com.kingdee.qa.http;

/**
 * HTTP 响应数据类
 * 封装状态码、响应体和错误信息
 */
public class HttpResponse {

    private final int statusCode;   // HTTP 状态码（如 200、404）
    private final String body;      // 响应体字符串
    private final boolean success;  // 是否成功（网络层面）
    private final String error;     // 错误信息（网络异常时非空）

    public HttpResponse(int statusCode, String body, boolean success, String error) {
        this.statusCode = statusCode;
        this.body = body;
        this.success = success;
        this.error = error;
    }

    /** 请求是否成功（网络通畅 且 状态码为 200） */
    public boolean isOk() {
        return success && statusCode == 200;
    }

    public int getStatusCode() { return statusCode; }
    public String getBody()    { return body; }
    public boolean isSuccess() { return success; }
    public String getError()   { return error; }

    @Override
    public String toString() {
        return "HttpResponse{code=" + statusCode + ", success=" + success
                + (error != null ? ", error='" + error + "'" : "") + "}";
    }
}
