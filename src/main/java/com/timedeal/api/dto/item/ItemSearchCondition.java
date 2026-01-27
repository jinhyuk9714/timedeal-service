package com.timedeal.api.dto.item;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.format.annotation.DateTimeFormat;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 상품 목록 검색/필터 조건 (모든 필드 선택)
 * GET /api/items?name=...&minPrice=...&maxPrice=...&openAfter=...&openBefore=... 에 바인딩
 */
@Schema(description = "상품 검색 조건 (모든 필드 선택)")
@ParameterObject
@Getter
@Setter
@NoArgsConstructor
public class ItemSearchCondition {

    @Schema(description = "상품명(부분 일치)", example = "타임딜")
    private String name;

    @Schema(description = "최소 가격 이상", example = "10000")
    private BigDecimal minPrice;

    @Schema(description = "최대 가격 이하", example = "100000")
    private BigDecimal maxPrice;

    @Schema(description = "오픈 시간 이상 (이 시간 이후 오픈)", example = "2026-01-01T00:00:00")
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
    private LocalDateTime openAfter;

    @Schema(description = "오픈 시간 이하 (이 시간 이전 오픈)", example = "2026-12-31T23:59:59")
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
    private LocalDateTime openBefore;

    /**
     * 조건이 하나라도 있으면 true
     */
    public boolean hasAnyCondition() {
        return (name != null && !name.isBlank())
                || minPrice != null
                || maxPrice != null
                || openAfter != null
                || openBefore != null;
    }
}
