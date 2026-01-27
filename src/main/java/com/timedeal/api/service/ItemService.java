package com.timedeal.api.service;

import com.timedeal.api.domain.item.Item;
import com.timedeal.api.domain.stock.Stock;
import com.timedeal.api.dto.item.ItemRequest;
import com.timedeal.api.dto.item.ItemResponse;
import com.timedeal.api.dto.item.ItemSearchCondition;
import com.timedeal.api.exception.BusinessException;
import com.timedeal.api.exception.ErrorCode;
import com.timedeal.api.infrastructure.persistence.item.ItemRepository;
import com.timedeal.api.infrastructure.persistence.order.OrderRepository;
import com.timedeal.api.infrastructure.persistence.stock.StockRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ItemService {
    
    private final ItemRepository itemRepository;
    private final StockRepository stockRepository;
    private final OrderRepository orderRepository;
    
    @Transactional
    public ItemResponse createItem(ItemRequest request) {
        Item item = Item.builder()
                .name(request.getName())
                .price(request.getPrice())
                .openTime(request.getOpenTime())
                .build();
        
        Item savedItem = itemRepository.save(item);
        
        // 재고 생성
        Stock stock = Stock.builder()
                .item(savedItem)
                .quantity(request.getStockQuantity())
                .build();
        Stock savedStock = stockRepository.save(stock);
        
        // 재고 정보를 포함한 응답 반환
        return new ItemResponse(savedItem, savedStock);
    }
    
    /**
     * 상품 조회 (단건)
     * 
     * @param id: 상품 ID
     * @return ItemResponse (상품 정보 + 재고 수량)
     */
    public ItemResponse getItem(Long id) {
        Item item = itemRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.ITEM_NOT_FOUND));
        
        // 재고 정보 조회
        Stock stock = stockRepository.findByItemId(id)
                .orElse(null); // 재고가 없을 수도 있음 (이론적으로는 항상 있어야 함)
        
        return new ItemResponse(item, stock);
    }
    
    /**
     * 전체 상품 목록 조회 (페이징)
     * 검색 조건이 없으면 전체 조회, 있으면 Querydsl로 필터링
     *
     * @param condition 검색 조건 (상품명, 가격 범위, 오픈 시간 범위). null이거나 조건 없음 시 전체 조회
     * @param pageable  페이징 정보 (page, size, sort)
     * @return Page<ItemResponse> (상품 목록 + 각 상품의 재고 수량 + 페이징 정보)
     */
    public Page<ItemResponse> getItems(ItemSearchCondition condition, Pageable pageable) {
        Page<Item> itemPage = (condition != null && condition.hasAnyCondition())
                ? itemRepository.findByCondition(condition, pageable)
                : itemRepository.findAll(pageable);

        return itemPage.map(item -> {
            Stock stock = stockRepository.findByItemId(item.getId())
                    .orElse(null);
            return new ItemResponse(item, stock);
        });
    }

    /**
     * 전체 상품 목록 조회 (페이징, 검색 없음)
     *
     * @param pageable 페이징 정보 (page, size, sort)
     * @return Page<ItemResponse>
     */
    public Page<ItemResponse> getAllItems(Pageable pageable) {
        return getItems(null, pageable);
    }
    
    public Item findById(Long id) {
        return itemRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.ITEM_NOT_FOUND));
    }
    
    /**
     * 상품 수정 (관리자 전용)
     * 
     * @param id: 수정할 상품 ID
     * @param request: 수정할 상품 정보
     * @return ItemResponse (수정된 상품 정보)
     */
    @Transactional
    public ItemResponse updateItem(Long id, ItemRequest request) {
        Item item = itemRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.ITEM_NOT_FOUND));
        
        // 상품 정보 수정
        item.update(request.getName(), request.getPrice(), request.getOpenTime());
        
        Item savedItem = itemRepository.save(item);
        
        // 재고 정보 수정
        Stock stock = stockRepository.findByItemId(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.STOCK_NOT_FOUND));
        stock.setQuantity(request.getStockQuantity());
        Stock savedStock = stockRepository.save(stock);
        
        return new ItemResponse(savedItem, savedStock);
    }
    
    /**
     * 상품 삭제 (관리자 전용)
     * 
     * @param id: 삭제할 상품 ID
     */
    @Transactional
    public void deleteItem(Long id) {
        Item item = itemRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.ITEM_NOT_FOUND));
        
        // 해당 상품을 참조하는 주문이 있는지 확인
        List<com.timedeal.api.domain.order.Order> orders = orderRepository.findByItemId(id);
        if (!orders.isEmpty()) {
            throw new BusinessException(ErrorCode.ITEM_CANNOT_BE_DELETED);
        }
        
        // 재고 삭제
        stockRepository.findByItemId(id).ifPresent(stockRepository::delete);
        
        // 상품 삭제
        itemRepository.delete(item);
    }
}
