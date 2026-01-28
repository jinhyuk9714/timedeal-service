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
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static com.timedeal.api.infrastructure.config.CacheConfig.CACHE_ITEMS;
import static com.timedeal.api.infrastructure.config.CacheConfig.CACHE_ITEM_LIST;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ItemService {
    
    private final ItemRepository itemRepository;
    private final StockRepository stockRepository;
    private final OrderRepository orderRepository;
    
    @Transactional
    @CacheEvict(value = CACHE_ITEM_LIST, allEntries = true)
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
     * 상품 조회 (단건). B-2 Caffeine 캐시 적용.
     */
    @Cacheable(value = CACHE_ITEMS, key = "#id")
    public ItemResponse getItem(Long id) {
        Item item = itemRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.ITEM_NOT_FOUND));
        
        // 재고 정보 조회
        Stock stock = stockRepository.findByItemId(id)
                .orElse(null); // 재고가 없을 수도 있음 (이론적으로는 항상 있어야 함)
        
        return new ItemResponse(item, stock);
    }
    
    /**
     * 전체 상품 목록 조회 (페이징). B-2 Caffeine 캐시 적용.
     */
    @Cacheable(value = CACHE_ITEM_LIST, keyGenerator = "itemListCacheKeyGenerator")
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
    
    @Transactional
    @Caching(evict = {
            @CacheEvict(value = CACHE_ITEMS, key = "#id"),
            @CacheEvict(value = CACHE_ITEM_LIST, allEntries = true)
    })
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
    
    @Transactional
    @CacheEvict(value = { CACHE_ITEMS, CACHE_ITEM_LIST }, allEntries = true)
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
