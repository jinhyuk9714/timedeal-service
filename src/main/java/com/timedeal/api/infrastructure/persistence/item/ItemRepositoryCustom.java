package com.timedeal.api.infrastructure.persistence.item;

import com.timedeal.api.domain.item.Item;
import com.timedeal.api.dto.item.ItemSearchCondition;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

/**
 * 상품 검색/필터용 커스텀 Repository (Querydsl)
 */
public interface ItemRepositoryCustom {

    /**
     * 검색 조건과 페이징으로 상품 목록 조회
     *
     * @param condition 검색 조건 (상품명, 가격 범위, 오픈 시간 범위)
     * @param pageable  페이징·정렬
     * @return 조건에 맞는 상품 페이지
     */
    Page<Item> findByCondition(ItemSearchCondition condition, Pageable pageable);
}
