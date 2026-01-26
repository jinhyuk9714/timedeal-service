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
        stockRepository.save(stock);
        
        return new ItemResponse(savedItem);
    }
    
    public ItemResponse getItem(Long id) {
        Item item = itemRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.ITEM_NOT_FOUND));
        return new ItemResponse(item);
    }
    
    public List<ItemResponse> getAllItems() {
        return itemRepository.findAll().stream()
                .map(ItemResponse::new)
                .toList();
    }
    
    public Item findById(Long id) {
        return itemRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.ITEM_NOT_FOUND));
    }
}
