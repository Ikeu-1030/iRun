package com.ikeu.server.handler;

import com.ikeu.common.exception.BusinessException;
import com.ikeu.common.exception.ForbiddenException;
import com.ikeu.common.exception.NotFoundException;
import com.ikeu.common.exception.ParamErrorException;
import com.ikeu.common.exception.UnauthorizedException;
import com.ikeu.common.result.Result;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

/**
 * 全局异常处理器，统一拦截各层抛出的异常并返回标准的 {@link Result} 响应。
 * @author ikeu
 * @since 2025/05/21
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * 处理业务异常（BusinessException），返回 HTTP 400。
     *
     * <p>业务异常通常由业务规则校验不通过时抛出，如用户名已存在、余额不足等。
     * 使用异常自身的 code 和 message 构造 Result。
     */
    @ExceptionHandler(BusinessException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Result<Void> handleBusiness(BusinessException e) {
        log.warn("业务异常: {}", e.getMessage());
        return Result.error(e.getCode(), e.getMessage());
    }

    /**
     * 处理资源未找到异常（NotFoundException），返回 HTTP 404。
     */
    @ExceptionHandler(NotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public Result<Void> handleNotFound(NotFoundException e) {
        return Result.error(e.getCode(), e.getMessage());
    }

    /**
     * 处理参数错误异常（ParamErrorException），返回 HTTP 400。
     */
    @ExceptionHandler(ParamErrorException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Result<Void> handleParamError(ParamErrorException e) {
        return Result.error(e.getCode(), e.getMessage());
    }

    /**
     * 处理未授权异常（UnauthorizedException），返回 HTTP 401。
     *
     * <p>未授权异常在 token 无效、密码错误、账号被禁用等场景下抛出。
     */
    @ExceptionHandler(UnauthorizedException.class)
    @ResponseStatus(HttpStatus.UNAUTHORIZED)
    public Result<Void> handleUnauthorized(UnauthorizedException e) {
        log.warn("未授权访问: {}", e.getMessage());
        return Result.error(e.getCode(), e.getMessage());
    }

    /**
     * 处理禁止访问异常（ForbiddenException），返回 HTTP 403。
     */
    @ExceptionHandler(ForbiddenException.class)
    @ResponseStatus(HttpStatus.FORBIDDEN)
    public Result<Void> handleForbidden(ForbiddenException e) {
        log.warn("操作被禁止: {}", e.getMessage());
        return Result.error(e.getCode(), e.getMessage());
    }

    /**
     * 处理方法参数校验失败异常，返回 HTTP 400。
     *
     * <p>当 {@code @Valid} 或 {@code @Validated} 标注的 DTO 校验不通过时由 Spring 抛出。
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Result<Void> handleValidation(MethodArgumentNotValidException e) {
        String msg = e.getBindingResult().getFieldErrors().stream()
                .map(err -> err.getDefaultMessage())
                .reduce((a, b) -> a + "; " + b).orElse("参数校验失败");
        return Result.error(msg);
    }

    /**
     * 处理方法参数类型不匹配异常，返回 HTTP 400。
     *
     * <p>当请求参数无法转换为目标类型时由 Spring 抛出，如将字符串 "abc" 传入 Integer 参数。
     */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Result<Void> handleTypeMismatch(MethodArgumentTypeMismatchException e) {
        return Result.error("参数类型错误: " + e.getName());
    }

    /**
     * 兜底异常处理，捕获所有未显式处理的异常，返回 HTTP 500。
     *
     * <p>记录完整堆栈信息到 error 日志，向客户端返回通用错误提示。
     */
    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public Result<Void> handleException(Exception e) {
        log.error("系统异常", e);
        return Result.error("系统繁忙，请稍后重试");
    }
}
