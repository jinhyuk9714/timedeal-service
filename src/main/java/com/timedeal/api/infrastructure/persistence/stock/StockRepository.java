package com.timedeal.api.infrastructure.persistence.stock;

import com.timedeal.api.domain.stock.Stock;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import jakarta.persistence.LockModeType;
import java.util.Optional;

@Repository
public interface StockRepository extends JpaRepository<Stock, Long> {
    
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT s FROM Stock s WHERE s.item.id = :itemId")
    Optional<Stock> findByItemIdWithLock(@Param("itemId") Long itemId);
    
    Optional<Stock> findByItemId(Long itemId);
}
