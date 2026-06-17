package com.natsukaze.smartoffice.common.handler;

import com.natsukaze.smartoffice.common.core.ErrorCode;
import com.natsukaze.smartoffice.common.core.Result;
import com.natsukaze.smartoffice.common.exception.BusinessException;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.validation.BindException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.stream.Collectors;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(BusinessException.class)
    @ResponseStatus(HttpStatus.OK)
    public Result<Void> handleBusinessException(BusinessException ex) {
        return Result.failure(ex.getCode(), ex.getMessage());
    }

    @ExceptionHandler({MethodArgumentNotValidException.class, BindException.class})
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Result<Void> handleBindException(Exception ex) {
        String message;
        if (ex instanceof MethodArgumentNotValidException methodEx) {
            message = methodEx.getBindingResult().getFieldErrors().stream()
                    .map(error -> error.getField() + " " + error.getDefaultMessage())
                    .collect(Collectors.joining("; "));
        } else {
            BindException bindEx = (BindException) ex;
            message = bindEx.getBindingResult().getFieldErrors().stream()
                    .map(error -> error.getField() + " " + error.getDefaultMessage())
                    .collect(Collectors.joining("; "));
        }
        return Result.failure(ErrorCode.BAD_REQUEST, message);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Result<Void> handleConstraintViolationException(ConstraintViolationException ex) {
        return Result.failure(ErrorCode.BAD_REQUEST, ex.getMessage());
    }

    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public Result<Void> handleException(Exception ex) {
        log.error("Unhandled exception", ex);
        return Result.failure(ErrorCode.SYSTEM_ERROR);
    }
}
