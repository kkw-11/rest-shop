package com.shop.core.event;

import com.shop.domain.event.Event;
import com.shop.domain.event.EventRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;


@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class EventService {

    private final EventRepository eventRepository;

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
     * Redis 분산 락용 재고 차감 (Facade에서 호출)
     */
    @Transactional
    public void decreaseStock(Long eventId, int quantity) {
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new IllegalArgumentException("이벤트를 찾을 수 없습니다."));

        event.decreaseStock(quantity);

        log.debug("재고 차감 완료. eventId={}, 남은재고={}",
                eventId, event.getRemainingStock());
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
