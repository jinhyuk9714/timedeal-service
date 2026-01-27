package com.timedeal.api.infrastructure.config;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.BeanDefinitionRegistryPostProcessor;
import org.springframework.context.annotation.Configuration;

/**
 * Spring Boot 4 + springdoc + Querydsl 조합에서
 * {@code queryDslQuerydslPredicateOperationCustomizer} 빈 생성 시
 * {@code org.springframework.data.util.TypeInformation} 등을 참조해
 * ClassNotFoundException이 발생하는 문제를 피하기 위해, 해당 빈 정의를 제거합니다.
 *
 * <p>Querydsl Predicate 기반 API 문서화 기능은 사용하지 않으므로 제거해도 무방합니다.
 *
 * <p>{@code proxyBeanMethods = false}: BeanDefinitionRegistryPostProcessor는
 * 프록시 없이 직접 인스턴스로 실행되어야 하므로 프록시 생성을 비활성화합니다.
 */
@Configuration(proxyBeanMethods = false)
public class SpringDocQuerydslFixConfig implements BeanDefinitionRegistryPostProcessor {

    private static final String QUERYDSL_CUSTOMIZER_BEAN = "queryDslQuerydslPredicateOperationCustomizer";

    @Override
    public void postProcessBeanDefinitionRegistry(BeanDefinitionRegistry registry) throws BeansException {
        if (registry.containsBeanDefinition(QUERYDSL_CUSTOMIZER_BEAN)) {
            registry.removeBeanDefinition(QUERYDSL_CUSTOMIZER_BEAN);
        }
    }
}
