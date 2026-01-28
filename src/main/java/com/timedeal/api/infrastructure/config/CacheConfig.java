package com.timedeal.api.infrastructure.config;

import com.github.benmanes.caffeine.cache.Caffeine;
import com.timedeal.api.dto.item.ItemSearchCondition;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.domain.Pageable;

import java.util.concurrent.TimeUnit;

/**
 * Spring Cache 설정 (B-2 상품 목록/상세 캐시).
 * Caffeine 인메모리 캐시 사용.
 */
@Configuration
@EnableCaching
public class CacheConfig {

    public static final String CACHE_ITEMS = "items";
    public static final String CACHE_ITEM_LIST = "itemList";

    @Bean
    public CacheManager cacheManager() {
        CaffeineCacheManager manager = new CaffeineCacheManager(CACHE_ITEMS, CACHE_ITEM_LIST);
        manager.setCaffeine(Caffeine.newBuilder()
                .maximumSize(10_000)
                .expireAfterWrite(10, TimeUnit.MINUTES));
        return manager;
    }

    /**
     * getItems(condition, pageable) 캐시 키 생성.
     * basic-read는 기본 페이지(condition 없음)로 반복 호출하므로 동일 키로 캐시 히트.
     */
    @Bean("itemListCacheKeyGenerator")
    public org.springframework.cache.interceptor.KeyGenerator itemListCacheKeyGenerator() {
        return (target, method, params) -> {
            ItemSearchCondition condition = (ItemSearchCondition) params[0];
            Pageable pageable = (Pageable) params[1];
            String condKey = condition == null ? "all"
                    : (condition.getName() + "|" + condition.getMinPrice() + "|" + condition.getMaxPrice()
                    + "|" + condition.getOpenAfter() + "|" + condition.getOpenBefore());
            return "list_" + condKey + "_" + pageable.getPageNumber()
                    + "_" + pageable.getPageSize() + "_" + pageable.getSort();
        };
    }
}
