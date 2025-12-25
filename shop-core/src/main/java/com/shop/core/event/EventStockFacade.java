package com.shop.core.event;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

/**
 * Redis 분산 락을 위한 Facade 패턴
 * - 락 획득/해제와 트랜잭션을 분리
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class EventStockFacade {

    private final RedissonClient redissonClient;
    private final EventService eventService;

    /**
     * Redis 분산 락을 사용한 재고 차감
     */
    public void decreaseStockWithRedisLock(Long eventId, int quantity) {
        String lockKey = "lock:event:" + eventId;
        RLock lock = redissonClient.getLock(lockKey);

        try {
            // 락 획득 시도 (최대 5초 대기, 락 점유 시간 3초)
            boolean available = lock.tryLock(5, 3, TimeUnit.SECONDS);

            if (!available) {
                log.warn("락 획득 실패. eventId={}", eventId);
                throw new IllegalStateException("락을 획득할 수 없습니다.");
            }

            // 트랜잭션이 완전히 커밋될 때까지 락 유지
            eventService.decreaseStock(eventId, quantity);

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("락 획득 중 인터럽트 발생", e);
        } finally {
            // 락 해제 (트랜잭션 커밋 후)
            if (lock.isHeldByCurrentThread()) {
                lock.unlock();
            }
        }
    }
}
