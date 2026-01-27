package com.timedeal.api.infrastructure.persistence.item;

import com.querydsl.core.types.Order;
import com.querydsl.core.types.OrderSpecifier;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.impl.JPAQuery;
import com.querydsl.jpa.impl.JPAQueryFactory;
import com.timedeal.api.domain.item.Item;
import com.timedeal.api.domain.item.QItem;
import com.timedeal.api.dto.item.ItemSearchCondition;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.support.PageableExecutionUtils;
import org.springframework.util.StringUtils;

import java.util.List;

/**
 * 상품 검색/필터 Querydsl 구현
 */
@RequiredArgsConstructor
public class ItemRepositoryImpl implements ItemRepositoryCustom {

    private final JPAQueryFactory queryFactory;

    @Override
    public Page<Item> findByCondition(ItemSearchCondition condition, Pageable pageable) {
        QItem item = QItem.item;

        List<Item> content = queryFactory
                .selectFrom(item)
                .where(
                        nameContains(condition.getName()),
                        priceGoe(condition.getMinPrice()),
                        priceLoe(condition.getMaxPrice()),
                        openTimeAfter(condition.getOpenAfter()),
                        openTimeBefore(condition.getOpenBefore())
                )
                .orderBy(orderSpecifiers(pageable))
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .fetch();

        JPAQuery<Long> countQuery = queryFactory
                .select(item.count())
                .from(item)
                .where(
                        nameContains(condition.getName()),
                        priceGoe(condition.getMinPrice()),
                        priceLoe(condition.getMaxPrice()),
                        openTimeAfter(condition.getOpenAfter()),
                        openTimeBefore(condition.getOpenBefore())
                );

        return PageableExecutionUtils.getPage(content, pageable, countQuery::fetchOne);
    }

    private BooleanExpression nameContains(String name) {
        return StringUtils.hasText(name) ? QItem.item.name.containsIgnoreCase(name) : null;
    }

    private BooleanExpression priceGoe(java.math.BigDecimal minPrice) {
        return minPrice != null ? QItem.item.price.goe(minPrice) : null;
    }

    private BooleanExpression priceLoe(java.math.BigDecimal maxPrice) {
        return maxPrice != null ? QItem.item.price.loe(maxPrice) : null;
    }

    private BooleanExpression openTimeAfter(java.time.LocalDateTime openAfter) {
        return openAfter != null ? QItem.item.openTime.after(openAfter) : null;
    }

    private BooleanExpression openTimeBefore(java.time.LocalDateTime openBefore) {
        return openBefore != null ? QItem.item.openTime.before(openBefore) : null;
    }

    private OrderSpecifier<?>[] orderSpecifiers(Pageable pageable) {
        if (!pageable.getSort().isSorted()) {
            return new OrderSpecifier[]{new OrderSpecifier<>(Order.DESC, QItem.item.id)};
        }
        return pageable.getSort().stream()
                .map(order -> {
                    Order direction = order.isAscending() ? Order.ASC : Order.DESC;
                    return switch (order.getProperty()) {
                        case "name" -> new OrderSpecifier<>(direction, QItem.item.name);
                        case "price" -> new OrderSpecifier<>(direction, QItem.item.price);
                        case "openTime" -> new OrderSpecifier<>(direction, QItem.item.openTime);
                        case "createdAt" -> new OrderSpecifier<>(direction, QItem.item.createdAt);
                        case "updatedAt" -> new OrderSpecifier<>(direction, QItem.item.updatedAt);
                        default -> new OrderSpecifier<>(direction, QItem.item.id);
                    };
                })
                .toArray(OrderSpecifier[]::new);
    }
}
