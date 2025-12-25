package com.shop.core.event;

import com.shop.domain.event.Event;
import com.shop.domain.event.EventRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class EventService {

    private final EventRepository eventRepository;
    private final RedissonClient redissonClient;

    /**
     * 비관적 락을 사용한 재고 차감
     */
    @Transactional
    public void decreaseStockWithPessimisticLock(Long eventId, int quantity) {
        Event event = eventRepository.findByIdForUpdate(eventId)
            .orElseThrow(() -> new IllegalArgumentException("이벤트를 찾을 수 없습니다."));
        
        event.decreaseStock(quantity);
        
        log.debug("이벤트 재고 차감 완료. eventId={}, 남은재고={}", eventId, event.getRemainingStock());
    }

    /**
     * Redis 분산 락을 사용한 재고 차감 (1차 시도 코드)
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
            
            // 같은 클래스 내부 메서드 호출 (문제 발생 지점!)
            decreaseStockInternal(eventId, quantity);
            
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("락 획득 중 인터럽트 발생", e);
        } finally {
            // 락 해제
            if (lock.isHeldByCurrentThread()) {
                lock.unlock();
            }
        }
    }
    
    /**
     * 실제 재고 차감 로직 (내부 호출용)
     */
    @Transactional
    public void decreaseStockInternal(Long eventId, int quantity) {
        Event event = eventRepository.findById(eventId)
            .orElseThrow(() -> new IllegalArgumentException("이벤트를 찾을 수 없습니다."));
        
        event.decreaseStock(quantity);
        
        log.debug("Redis 락 - 재고 차감 완료. eventId={}, 남은재고={}", eventId, event.getRemainingStock());
    }

    /**
     * 동시성 제어 없는 재고 차감 (문제 재현용)
     */
    @Transactional
    public void decreaseStockWithoutLock(Long eventId, int quantity) {
        Event event = eventRepository.findById(eventId)
            .orElseThrow(() -> new IllegalArgumentException("이벤트를 찾을 수 없습니다."));
        
        event.decreaseStock(quantity);
    }
}
