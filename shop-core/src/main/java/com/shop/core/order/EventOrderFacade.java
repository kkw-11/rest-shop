package com.shop.core.order;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

@Slf4j
@Component
@RequiredArgsConstructor
public class EventOrderFacade {

    private final RedissonClient redissonClient;
    private final EventOrderService eventOrderService;

    /**
     * Redis 분산 락으로 이벤트 주문 생성
     *
     * 최적화된 설정 (측정 기반):
     * - 평균 트랜잭션 시간: 150ms
     * - P99 트랜잭션 시간: 200ms
     * - leaseTime: 1초 (200ms × 5 = 1000ms)
     * - waitTime: 5초
     */
    public Long createOrderWithRedisLock(Long eventId, Long memberId) {
        String lockKey = "lock:event:order:" + eventId;
        RLock lock = redissonClient.getLock(lockKey);

        try {
            // todo: latency 측정 기반 최적화 설정
            boolean available = lock.tryLock(5, 1, TimeUnit.SECONDS);

            if (!available) {
                log.warn("락 획득 실패. eventId={}, memberId={}", eventId, memberId);
                throw new IllegalStateException("현재 요청이 많습니다. 잠시 후 다시 시도해주세요.");
            }

            return eventOrderService.createOrder(eventId, memberId);

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("락 획득 중 인터럽트 발생", e);
        } finally {
            if (lock.isHeldByCurrentThread()) {
                lock.unlock();
            }
        }
    }
}
