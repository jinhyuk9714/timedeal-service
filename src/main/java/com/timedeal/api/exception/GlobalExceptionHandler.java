package com.timedeal.api.exception;

import lombok.extern.slf4j.Slf4j;
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
