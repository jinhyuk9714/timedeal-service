package com.timedeal.api.infrastructure.lock;

import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;

import java.util.List;

/**
 * 재고 차감 구간용 Redis 분산 락 핸들.
 * tryLock 성공 시 반환되며, 크리티컬 섹션 종료 후 반드시 {@link #release()} 호출.
 */
public class StockLockHandle {

    private static final String RELEASE_SCRIPT =
            "if redis.call('get', KEYS[1]) == ARGV[1] then return redis.call('del', KEYS[1]) else return 0 end";

    private final String key;
    private final String value;
    private final RedisTemplate<String, String> redisTemplate;
    private volatile boolean released;

    public StockLockHandle(String key, String value, RedisTemplate<String, String> redisTemplate) {
        this.key = key;
        this.value = value;
        this.redisTemplate = redisTemplate;
        this.released = false;
    }

    /**
     * 락 해제. 보유한 경우에만 삭제 (value 일치 시).
     */
    public void release() {
        if (released) {
            return;
        }
        try {
            DefaultRedisScript<Long> script = new DefaultRedisScript<>(RELEASE_SCRIPT, Long.class);
            redisTemplate.execute(script, List.of(key), value);
        } finally {
            released = true;
        }
    }
}
