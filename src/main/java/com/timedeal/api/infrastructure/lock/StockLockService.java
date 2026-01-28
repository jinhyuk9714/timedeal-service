package com.timedeal.api.infrastructure.lock;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * 재고 차감 시 상품별 Redis 분산 락을 제공.
 * order.lock-strategy=distributed 일 때 사용.
 * TTL·재시도는 order.distributed-lock.* (application-perf-distributed.yml)로 조정 가능.
 */
@Service
public class StockLockService {

    private static final String KEY_PREFIX = "stock:lock:";

    private final RedisTemplate<String, String> redisTemplate;
    private final long ttlSeconds;

    public StockLockService(RedisTemplate<String, String> redisTemplate,
                            @Value("${order.distributed-lock.ttl-seconds:5}") long ttlSeconds) {
        this.redisTemplate = redisTemplate;
        this.ttlSeconds = ttlSeconds;
    }

    /**
     * 상품별 락 시도. 성공 시 핸들을 반환하며, 사용 후 반드시 {@link StockLockHandle#release()} 호출.
     *
     * @param itemId 상품 ID
     * @return 락을 잡은 경우에만 비어 있지 않은 Optional
     */
    public Optional<StockLockHandle> tryLock(Long itemId) {
        String key = KEY_PREFIX + itemId;
        String value = UUID.randomUUID().toString();
        Boolean ok = redisTemplate.opsForValue().setIfAbsent(key, value, ttlSeconds, TimeUnit.SECONDS);
        if (Boolean.TRUE.equals(ok)) {
            return Optional.of(new StockLockHandle(key, value, redisTemplate));
        }
        return Optional.empty();
    }
}
