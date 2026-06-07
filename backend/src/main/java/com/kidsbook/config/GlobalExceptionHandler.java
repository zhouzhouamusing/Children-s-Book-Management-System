package com.kidsbook.config;

import com.kidsbook.common.Result;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataAccessException;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.sql.SQLException;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(AccessDeniedException.class)
    @ResponseStatus(HttpStatus.FORBIDDEN)
    public Result<Void> handleAccessDenied(AccessDeniedException e) {
        log.warn("权限不足: {}", e.getMessage());
        return Result.error(403, e.getMessage() != null ? e.getMessage() : "没有权限访问该资源");
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(HttpStatus.OK)
    public Result<Void> handleValidation(MethodArgumentNotValidException e) {
        String message = e.getBindingResult().getFieldErrors().stream()
                .map(err -> err.getDefaultMessage())
                .findFirst()
                .orElse("参数校验失败");
        log.warn("参数校验失败: {}", message);
        return Result.error(400, message);
    }

    @ExceptionHandler({DataAccessException.class, SQLException.class})
    @ResponseStatus(HttpStatus.OK)
    public Result<Void> handleDatabaseException(Exception e) {
        log.error("数据库操作异常: {}", e.getMessage(), e);
        return Result.error(500, "数据操作异常，请稍后重试");
    }

    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.OK)
    public Result<Void> handleException(Exception e) {
        log.error("未处理异常: {}", e.getMessage(), e);
        String msg = e.getMessage() != null ? e.getMessage() : "服务器内部错误";
        if (msg.toLowerCase().contains("sql") || msg.toLowerCase().contains("jdbc")
                || msg.toLowerCase().contains("table") || msg.toLowerCase().contains("column")
                || msg.toLowerCase().contains("syntax") || msg.contains("MySql")
                || msg.contains("hibernate") || msg.contains("mybatis")) {
            msg = "数据操作异常，请稍后重试";
        }
        return Result.error(500, msg);
    }
}
