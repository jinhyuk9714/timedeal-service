package com.timedeal.api.exception;

import jakarta.persistence.OptimisticLockException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {
    
    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ErrorResponse> handleBusinessException(
            BusinessException e, WebRequest request) {
        log.error("BusinessException: {}", e.getMessage());
        ErrorResponse errorResponse = new ErrorResponse(
                e.getErrorCode(), 
                request.getDescription(false).replace("uri=", "")
        );
        return ResponseEntity
                .status(e.getErrorCode().getStatus())
                .body(errorResponse);
    }
    
    /** 낙관적 락 충돌이 재시도 루프 밖으로 나온 경우 → 4xx(INSUFFICIENT_STOCK)로 응답. 5xx 방지. */
    @ExceptionHandler({OptimisticLockException.class, OptimisticLockingFailureException.class})
    public ResponseEntity<ErrorResponse> handleOptimisticLock(
            Exception e, WebRequest request) {
        log.warn("Optimistic lock conflict (mapped to 4xx): {}", e.getMessage());
        ErrorResponse errorResponse = new ErrorResponse(
                ErrorCode.INSUFFICIENT_STOCK,
                request.getDescription(false).replace("uri=", "")
        );
        return ResponseEntity
                .status(ErrorCode.INSUFFICIENT_STOCK.getStatus())
                .body(errorResponse);
    }

    @ExceptionHandler({MethodArgumentNotValidException.class, BindException.class})
    public ResponseEntity<ErrorResponse> handleValidationException(
            Exception e, WebRequest request) {
        log.error("ValidationException: {}", e.getMessage());
        ErrorResponse errorResponse = new ErrorResponse(
                ErrorCode.INVALID_INPUT_VALUE,
                request.getDescription(false).replace("uri=", "")
        );
        return ResponseEntity
                .status(ErrorCode.INVALID_INPUT_VALUE.getStatus())
                .body(errorResponse);
    }
    
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleException(
            Exception e, WebRequest request) {
        log.error("Unexpected error: ", e);
        ErrorResponse errorResponse = new ErrorResponse(
                ErrorCode.INTERNAL_SERVER_ERROR,
                request.getDescription(false).replace("uri=", "")
        );
        return ResponseEntity
                .status(ErrorCode.INTERNAL_SERVER_ERROR.getStatus())
                .body(errorResponse);
    }
}
