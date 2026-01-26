package com.timedeal.api.service;

import com.timedeal.api.domain.item.Item;
import com.timedeal.api.domain.stock.Stock;
import com.timedeal.api.dto.item.ItemRequest;
import com.timedeal.api.dto.item.ItemResponse;
import com.timedeal.api.exception.BusinessException;
import com.timedeal.api.exception.ErrorCode;
import com.timedeal.api.infrastructure.persistence.item.ItemRepository;
import com.timedeal.api.infrastructure.persistence.order.OrderRepository;
import com.timedeal.api.infrastructure.persistence.stock.StockRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * ItemService 단위 테스트
 * 
 * @ExtendWith(MockitoExtension.class): Mockito를 사용한 단위 테스트
 * @Mock: Mock 객체 생성 (의존성을 대체)
 * @InjectMocks: Mock 객체를 주입받을 대상 객체
 * 
 * 이 테스트는 실제 데이터베이스 없이 Service 로직만 검증합니다.
 */
@ExtendWith(MockitoExtension.class)
class ItemServiceTest {

    @Mock
    private ItemRepository itemRepository;

    @Mock
    private StockRepository stockRepository;

    @Mock
    private OrderRepository orderRepository;

    @InjectMocks
    private ItemService itemService;

    private Item item;
    private Stock stock;
    private ItemRequest itemRequest;

    @BeforeEach
    void setUp() {
        // 테스트 데이터 준비
        item = Item.builder()
                .name("타임딜 상품")
                .price(new BigDecimal("10000"))
                .openTime(LocalDateTime.now().plusHours(1))
                .build();
        item.setId(1L);

        stock = Stock.builder()
                .item(item)
                .quantity(100)
                .build();
        stock.setId(1L);

        itemRequest = new ItemRequest();
        itemRequest.setName("타임딜 상품");
        itemRequest.setPrice(new BigDecimal("10000"));
        itemRequest.setOpenTime(LocalDateTime.now().plusHours(1));
        itemRequest.setStockQuantity(100);
    }

    @Test
    @DisplayName("상품 수정 성공")
    void updateItem_Success() {
        // given: Mock 객체의 동작 정의
        ItemRequest updateRequest = new ItemRequest();
        updateRequest.setName("수정된 상품명");
        updateRequest.setPrice(new BigDecimal("20000"));
        updateRequest.setOpenTime(LocalDateTime.now().plusHours(2));
        updateRequest.setStockQuantity(200);

        when(itemRepository.findById(1L)).thenReturn(Optional.of(item));
        when(stockRepository.findByItemId(1L)).thenReturn(Optional.of(stock));
        when(itemRepository.save(any(Item.class))).thenReturn(item);
        when(stockRepository.save(any(Stock.class))).thenReturn(stock);

        // when: 상품 수정
        ItemResponse response = itemService.updateItem(1L, updateRequest);

        // then: 검증
        assertThat(response).isNotNull();
        verify(itemRepository, times(1)).findById(1L);
        verify(stockRepository, times(1)).findByItemId(1L);
        verify(itemRepository, times(1)).save(any(Item.class));
        verify(stockRepository, times(1)).save(any(Stock.class));
    }

    @Test
    @DisplayName("상품 수정 실패 - 상품을 찾을 수 없음")
    void updateItem_ItemNotFound_Fail() {
        // given: 존재하지 않는 상품 ID
        when(itemRepository.findById(999L)).thenReturn(Optional.empty());

        // when & then: 상품을 찾을 수 없으므로 예외 발생
        assertThatThrownBy(() -> itemService.updateItem(999L, itemRequest))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining(ErrorCode.ITEM_NOT_FOUND.getMessage());
    }

    @Test
    @DisplayName("상품 수정 실패 - 재고를 찾을 수 없음")
    void updateItem_StockNotFound_Fail() {
        // given: 재고가 없는 경우
        when(itemRepository.findById(1L)).thenReturn(Optional.of(item));
        when(stockRepository.findByItemId(1L)).thenReturn(Optional.empty());

        // when & then: 재고를 찾을 수 없으므로 예외 발생
        assertThatThrownBy(() -> itemService.updateItem(1L, itemRequest))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining(ErrorCode.STOCK_NOT_FOUND.getMessage());
    }

    @Test
    @DisplayName("상품 삭제 성공")
    void deleteItem_Success() {
        // given: Mock 객체의 동작 정의 (주문이 없는 경우)
        when(itemRepository.findById(1L)).thenReturn(Optional.of(item));
        when(orderRepository.findByItemId(1L)).thenReturn(List.of()); // 주문 없음
        when(stockRepository.findByItemId(1L)).thenReturn(Optional.of(stock));
        doNothing().when(stockRepository).delete(any(Stock.class));
        doNothing().when(itemRepository).delete(any(Item.class));

        // when: 상품 삭제
        itemService.deleteItem(1L);

        // then: 검증
        verify(itemRepository, times(1)).findById(1L);
        verify(orderRepository, times(1)).findByItemId(1L);
        verify(stockRepository, times(1)).findByItemId(1L);
        verify(stockRepository, times(1)).delete(any(Stock.class));
        verify(itemRepository, times(1)).delete(any(Item.class));
    }

    @Test
    @DisplayName("상품 삭제 실패 - 상품을 찾을 수 없음")
    void deleteItem_ItemNotFound_Fail() {
        // given: 존재하지 않는 상품 ID
        when(itemRepository.findById(999L)).thenReturn(Optional.empty());

        // when & then: 상품을 찾을 수 없으므로 예외 발생
        assertThatThrownBy(() -> itemService.deleteItem(999L))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining(ErrorCode.ITEM_NOT_FOUND.getMessage());
    }

    @Test
    @DisplayName("상품 삭제 실패 - 주문이 존재하는 경우")
    void deleteItem_OrderExists_Fail() {
        // given: 주문이 있는 경우
        when(itemRepository.findById(1L)).thenReturn(Optional.of(item));
        when(orderRepository.findByItemId(1L)).thenReturn(List.of(mock(com.timedeal.api.domain.order.Order.class))); // 주문 존재

        // when & then: 주문이 있으므로 삭제 불가
        assertThatThrownBy(() -> itemService.deleteItem(1L))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining(ErrorCode.ITEM_CANNOT_BE_DELETED.getMessage());
        
        verify(itemRepository, times(1)).findById(1L);
        verify(orderRepository, times(1)).findByItemId(1L);
        verify(itemRepository, never()).delete(any(Item.class));
    }

    @Test
    @DisplayName("상품 삭제 성공 - 재고가 없는 경우")
    void deleteItem_StockNotExists_Success() {
        // given: 재고가 없는 경우 (이론적으로는 없어야 하지만 안전하게 처리)
        when(itemRepository.findById(1L)).thenReturn(Optional.of(item));
        when(orderRepository.findByItemId(1L)).thenReturn(List.of()); // 주문 없음
        when(stockRepository.findByItemId(1L)).thenReturn(Optional.empty());
        doNothing().when(itemRepository).delete(any(Item.class));

        // when: 상품 삭제
        itemService.deleteItem(1L);

        // then: 검증 (재고 삭제는 호출되지 않음)
        verify(itemRepository, times(1)).findById(1L);
        verify(orderRepository, times(1)).findByItemId(1L);
        verify(stockRepository, times(1)).findByItemId(1L);
        verify(stockRepository, never()).delete(any(Stock.class));
        verify(itemRepository, times(1)).delete(any(Item.class));
    }
}
