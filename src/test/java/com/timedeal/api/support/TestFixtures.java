package com.timedeal.api.support;

import com.timedeal.api.domain.item.Item;
import com.timedeal.api.domain.order.Order;
import com.timedeal.api.domain.order.OrderStatus;
import com.timedeal.api.domain.stock.Stock;
import com.timedeal.api.domain.user.User;
import com.timedeal.api.domain.user.UserRole;
import com.timedeal.api.dto.item.ItemRequest;
import com.timedeal.api.dto.order.OrderRequest;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 테스트용 공통 데이터 픽스처.
 * 중복 제거 및 일관된 테스트 데이터 제공.
 */
public final class TestFixtures {

    private TestFixtures() {}

    // ─── User ─────────────────────────────────────────────────────────────
    public static User user(Long id) {
        User u = User.builder()
                .email("test@test.com")
                .password("password")
                .name("테스트 사용자")
                .build();
        u.setId(id != null ? id : 1L);
        return u;
    }

    public static User adminUser(Long id) {
        User u = user(id);
        u.changeRole(UserRole.ADMIN);
        return u;
    }

    // ─── Item ─────────────────────────────────────────────────────────────
    public static Item item(Long id, LocalDateTime openTime) {
        Item i = Item.builder()
                .name("타임딜 상품")
                .price(new BigDecimal("10000"))
                .openTime(openTime != null ? openTime : LocalDateTime.now().plusHours(1))
                .build();
        if (id != null) {
            i.setId(id);
        }
        return i;
    }

    public static Item itemOpened(Long id) {
        return item(id, LocalDateTime.now().minusHours(1));
    }

    // ─── Stock ─────────────────────────────────────────────────────────────
    public static Stock stock(Item item, int quantity, Long id) {
        Stock s = Stock.builder()
                .item(item)
                .quantity(quantity)
                .build();
        if (id != null) {
            s.setId(id);
        }
        return s;
    }

    // ─── Order ─────────────────────────────────────────────────────────────
    public static Order order(User user, Item item, int quantity, OrderStatus status, Long id) {
        Order o = Order.builder()
                .user(user)
                .item(item)
                .status(status != null ? status : OrderStatus.ORDERED)
                .quantity(quantity)
                .build();
        if (id != null) {
            o.setId(id);
        }
        return o;
    }

    // ─── DTO ───────────────────────────────────────────────────────────────
    public static ItemRequest itemRequest(String name, BigDecimal price, LocalDateTime openTime, int stockQty) {
        ItemRequest r = new ItemRequest();
        r.setName(name != null ? name : "타임딜 상품");
        r.setPrice(price != null ? price : new BigDecimal("10000"));
        r.setOpenTime(openTime != null ? openTime : LocalDateTime.now().plusHours(1));
        r.setStockQuantity(stockQty > 0 ? stockQty : 100);
        return r;
    }

    public static OrderRequest orderRequest(Long itemId, int quantity) {
        OrderRequest r = new OrderRequest();
        r.setItemId(itemId != null ? itemId : 1L);
        r.setQuantity(quantity > 0 ? quantity : 1);
        return r;
    }
}
