package com.wwweinuo.cloudmall.common.response;

/**
 * 公共错误码占位，业务错误码应由各业务模块自行维护。
 */
public enum ErrorCode {
    SUCCESS(0, "success"),
    FAILURE(1, "failure");

    private final int code;
    private final String message;

    ErrorCode(int code, String message) {
        this.code = code;
        this.message = message;
    }

    public int code() {
        return code;
    }

    public String message() {
        return message;
    }
}
