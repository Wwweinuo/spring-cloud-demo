package com.wwweinuo.cloudmall.order.common;

import com.wwweinuo.cloudmall.common.response.Result;
import feign.FeignException;
import feign.RetryableException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(IllegalArgumentException.class)
    public Result<Void> handleIllegalArgument(IllegalArgumentException exception) {
        return Result.failure(exception.getMessage());
    }

    @ExceptionHandler({FeignException.class, RetryableException.class})
    public Result<Void> handleRemoteCallException(Exception exception) {
        return Result.failure("下游服务暂时不可用，请稍后重试");
    }
}
