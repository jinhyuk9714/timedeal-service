package com.timedeal.api.infrastructure.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Swagger/OpenAPI 설정 클래스
 * 
 * @Configuration:
 * - Spring 설정 클래스임을 명시
 * - @Bean 메서드가 반환하는 객체를 Spring 빈으로 등록
 * 
 * 이 설정을 통해 Swagger UI에서 API 문서를 확인하고 테스트할 수 있습니다.
 * - Swagger UI: http://localhost:8080/swagger-ui.html
 * - OpenAPI JSON: http://localhost:8080/v3/api-docs
 */
@Configuration
public class SwaggerConfig {

    /**
     * OpenAPI 설정 빈 등록
     * 
     * JWT 인증을 위한 Security Scheme을 포함하여
     * Swagger UI에서 직접 JWT 토큰을 입력하여 인증이 필요한 API를 테스트할 수 있습니다.
     * 
     * @return OpenAPI 설정 객체
     */
    @Bean
    public OpenAPI openAPI() {
        // JWT 인증 스키마 정의
        String jwtSchemeName = "bearerAuth";
        SecurityRequirement securityRequirement = new SecurityRequirement().addList(jwtSchemeName);
        Components components = new Components()
                .addSecuritySchemes(jwtSchemeName, new SecurityScheme()
                        .name(jwtSchemeName)
                        .type(SecurityScheme.Type.HTTP)
                        .scheme("bearer")
                        .bearerFormat("JWT")
                        .description("JWT 토큰을 입력하세요. 형식: Bearer {token}"));

        return new OpenAPI()
                .info(new Info()
                        .title("타임딜 서비스 API")
                        .description("타임딜 상품 주문 서비스 REST API 문서")
                        .version("v1.0.0")
                        .contact(new Contact()
                                .name("타임딜 서비스")
                                .email("support@timedeal.com"))
                        .license(new License()
                                .name("Apache 2.0")
                                .url("https://www.apache.org/licenses/LICENSE-2.0.html")))
                .addSecurityItem(securityRequirement)
                .components(components);
    }
}
