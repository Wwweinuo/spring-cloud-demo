package com.wwweinuo.cloudmall.common.response;

/**
 * 统一接口响应结构。
 *
 * @param <T> 响应数据类型
 */
public record Result<T>(int code, String message, T data) {

    public static <T> Result<T> success(T data) {
        return new Result<>(0, "success", data);
    }

    public static <T> Result<T> failure(String message) {
        return new Result<>(1, message, null);
    }
}
