package com.timedeal.api.infrastructure.config;

import com.querydsl.jpa.impl.JPAQueryFactory;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Querydsl 설정 클래스
 * 
 * @Configuration:
 * - Spring 설정 클래스임을 나타냄
 * - @Component의 특수화된 형태
 * - @Bean 메서드를 통해 Spring 빈을 정의
 * - 애플리케이션 컨텍스트가 로드될 때 자동으로 실행됨
 * 
 * Querydsl:
 * - 타입 안전한 쿼리 작성 라이브러리
 * - 컴파일 타임에 쿼리 오류를 발견 가능
 * - 예: QItem.item.name.eq("상품명") 같은 방식으로 쿼리 작성
 */
@Configuration
public class QuerydslConfig {

    /**
     * EntityManager 주입
     * 
     * @PersistenceContext:
     * - JPA EntityManager를 주입받는 어노테이션
     * - @Autowired와 유사하지만 JPA 전용
     * - 트랜잭션별로 다른 EntityManager 인스턴스를 제공
     * 
     * EntityManager:
     * - JPA의 핵심 인터페이스
     * - 엔티티의 생명주기 관리 (생성, 조회, 수정, 삭제)
     * - 영속성 컨텍스트(Persistence Context) 관리
     */
    @PersistenceContext
    private EntityManager entityManager;

    /**
     * JPAQueryFactory 빈 등록
     * 
     * @Bean:
     * - Spring 컨테이너에 빈으로 등록
     * - 다른 클래스에서 @Autowired로 주입받아 사용 가능
     * - 메서드 이름이 빈 이름이 됨 (jpaQueryFactory)
     * 
     * JPAQueryFactory:
     * - Querydsl을 사용하여 쿼리를 작성하기 위한 팩토리 클래스
     * - EntityManager를 받아서 생성
     * - Service나 Repository에서 주입받아 사용
     * 
     * 사용 예시:
     * @Autowired
     * private JPAQueryFactory queryFactory;
     * 
     * QItem item = QItem.item;
     * List<Item> items = queryFactory
     *     .selectFrom(item)
     *     .where(item.name.eq("상품명"))
     *     .fetch();
     */
    @Bean
    public JPAQueryFactory jpaQueryFactory() {
        return new JPAQueryFactory(entityManager);
    }
}
