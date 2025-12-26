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
     * 레디스 분산 락으로 이벤트 주문 생성
     */
    public Long createOrderWithRedisLock(Long eventId, Long memberId) {
        String lockKey = "lock:event:order:" + eventId;
        RLock lock = redissonClient.getLock(lockKey);

        try {
            //락 획득 시도 (최대 5초 대기, 락 점유 3초)
            boolean available = lock.tryLock(5, 3, TimeUnit.SECONDS);

            if (!available) {
                log.warn("락 획득 실패. eventId:{}, memberId={}", eventId , memberId);
                throw new IllegalStateException("현재 요청이 많습니다.");
            }

            Long orderId = eventOrderService.createOrder(eventId, memberId);

            //트랜잭션 실행(주문 생성)
            return orderId;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("락 획득 중 인터럽트 발생" , e);
        } finally {
            //락 해제
            if(lock.isHeldByCurrentThread()){
                lock.unlock();
            }
        }
    }
}
