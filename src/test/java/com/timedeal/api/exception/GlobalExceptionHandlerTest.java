package com.timedeal.api.exception;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.BindException;
import org.springframework.validation.FieldError;
import org.springframework.web.context.request.WebRequest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GlobalExceptionHandlerTest {

    private GlobalExceptionHandler handler;

    @Mock
    private WebRequest webRequest;

    @BeforeEach
    void setUp() {
        handler = new GlobalExceptionHandler();
        when(webRequest.getDescription(false)).thenReturn("uri=/api/items");
    }

    @Test
    @DisplayName("BusinessException 처리 시 해당 에러 코드/메시지 반환")
    void handleBusinessException() {
        BusinessException ex = new BusinessException(ErrorCode.ITEM_NOT_FOUND);

        ResponseEntity<ErrorResponse> res = handler.handleBusinessException(ex, webRequest);

        assertThat(res.getStatusCode()).isEqualTo(ErrorCode.ITEM_NOT_FOUND.getStatus());
        assertThat(res.getBody()).isNotNull();
        assertThat(res.getBody().getError()).isEqualTo(ErrorCode.ITEM_NOT_FOUND.getStatus().name());
        assertThat(res.getBody().getMessage()).isEqualTo(ErrorCode.ITEM_NOT_FOUND.getMessage());
    }

    @Test
    @DisplayName("검증 예외(BindException) 처리 시 400 및 INVALID_INPUT_VALUE 반환")
    void handleValidationException() {
        BeanPropertyBindingResult bindingResult = new BeanPropertyBindingResult(new Object(), "target");
        bindingResult.addError(new FieldError("target", "name", "필수값입니다"));
        BindException ex = new BindException(bindingResult);

        ResponseEntity<ErrorResponse> res = handler.handleValidationException(ex, webRequest);

        assertThat(res.getStatusCode()).isEqualTo(ErrorCode.INVALID_INPUT_VALUE.getStatus());
        assertThat(res.getBody()).isNotNull();
        assertThat(res.getBody().getError()).isEqualTo(ErrorCode.INVALID_INPUT_VALUE.getStatus().name());
    }

    @Test
    @DisplayName("일반 Exception 처리 시 500 및 INTERNAL_SERVER_ERROR 반환")
    void handleException() {
        when(webRequest.getDescription(false)).thenReturn("uri=/api/items");
        Exception ex = new RuntimeException("unexpected");

        ResponseEntity<ErrorResponse> res = handler.handleException(ex, webRequest);

        assertThat(res.getStatusCode()).isEqualTo(ErrorCode.INTERNAL_SERVER_ERROR.getStatus());
        assertThat(res.getBody()).isNotNull();
        assertThat(res.getBody().getError()).isEqualTo(ErrorCode.INTERNAL_SERVER_ERROR.getStatus().name());
    }
}
