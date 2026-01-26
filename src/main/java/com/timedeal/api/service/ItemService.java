package com.timedeal.api.service;

import com.timedeal.api.domain.item.Item;
import com.timedeal.api.domain.stock.Stock;
import com.timedeal.api.dto.item.ItemRequest;
import com.timedeal.api.dto.item.ItemResponse;
import com.timedeal.api.exception.BusinessException;
import com.timedeal.api.exception.ErrorCode;
import com.timedeal.api.infrastructure.persistence.item.ItemRepository;
import com.timedeal.api.infrastructure.persistence.stock.StockRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ItemService {
    
    private final ItemRepository itemRepository;
    private final StockRepository stockRepository;
    
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
     * 전체 상품 목록 조회
     * 
     * @return List<ItemResponse> (상품 목록 + 각 상품의 재고 수량)
     */
    public List<ItemResponse> getAllItems() {
        List<Item> items = itemRepository.findAll();
        
        return items.stream()
                .map(item -> {
                    // 각 상품의 재고 정보 조회
                    Stock stock = stockRepository.findByItemId(item.getId())
                            .orElse(null);
                    return new ItemResponse(item, stock);
                })
                .toList();
    }
    
    public Item findById(Long id) {
        return itemRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.ITEM_NOT_FOUND));
    }
}
