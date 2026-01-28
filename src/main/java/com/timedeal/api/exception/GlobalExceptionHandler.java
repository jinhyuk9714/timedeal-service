package com.timedeal.api.exception;

import jakarta.persistence.OptimisticLockException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import io.github.resilience4j.ratelimiter.RequestNotPermitted;
import io.github.resilience4j.circuitbreaker.CallNotPermittedException;

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

    /** Rate limiting 초과 시 429 Too Many Requests */
    @ExceptionHandler(RequestNotPermitted.class)
    public ResponseEntity<ErrorResponse> handleRateLimiterException(
            RequestNotPermitted e, WebRequest request) {
        log.warn("Rate limit exceeded: {}", e.getMessage());
        ErrorResponse errorResponse = new ErrorResponse(
                ErrorCode.SERVICE_UNAVAILABLE,
                request.getDescription(false).replace("uri=", "")
        );
        return ResponseEntity
                .status(HttpStatus.TOO_MANY_REQUESTS)
                .body(errorResponse);
    }

    /** Circuit breaker 차단 시 503 Service Unavailable */
    @ExceptionHandler(CallNotPermittedException.class)
    public ResponseEntity<ErrorResponse> handleCircuitBreakerException(
            CallNotPermittedException e, WebRequest request) {
        log.warn("Circuit breaker open: {}", e.getMessage());
        ErrorResponse errorResponse = new ErrorResponse(
                ErrorCode.SERVICE_UNAVAILABLE,
                request.getDescription(false).replace("uri=", "")
        );
        return ResponseEntity
                .status(ErrorCode.SERVICE_UNAVAILABLE.getStatus())
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
