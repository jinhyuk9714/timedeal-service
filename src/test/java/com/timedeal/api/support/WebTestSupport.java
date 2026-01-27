package com.timedeal.api.support;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

/**
 * Web/Controller 테스트 시 공통 설정.
 */
public final class WebTestSupport {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper()
            .registerModule(new JavaTimeModule());

    private WebTestSupport() {}

    public static ObjectMapper objectMapper() {
        return OBJECT_MAPPER;
    }
}
