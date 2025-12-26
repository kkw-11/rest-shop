package com.shop.api.order;

import com.shop.api.order.dto.EventOrderResponse;
import com.shop.common.dto.ApiResponse;
import com.shop.core.order.EventOrderFacade;
import com.shop.core.order.EventOrderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/api/events")
@RequiredArgsConstructor
public class EventOrderController {
    private final EventOrderService eventOrderService;
    private final EventOrderFacade eventOrderFacade;

    /**
     * 이벤트 상품 주문 - 비관적 락
     */
    @PostMapping("/{eventId}/order/pessimistic")
    public ResponseEntity<ApiResponse<EventOrderResponse>> orderWithPessimisticLock(
            @PathVariable Long eventId,
            @RequestParam("memberId") Long memberId
    ) {
        log.info("비관적 락 이벤트 주문 요청. eventId={}, memberId={}", eventId, memberId);

        Long orderId = eventOrderService.createOrderWithPessimisticLock(eventId, memberId);

        EventOrderResponse response = new EventOrderResponse("주문 완료", orderId, eventId, memberId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    /**
     * 이벤트 상품 주문 - Redis 분산 락
     */
    @PostMapping("/{eventId}/order/redis")
    public ResponseEntity<ApiResponse<EventOrderResponse>> orderWithRedisLock(
            @PathVariable Long eventId,
            @RequestParam Long memberId
    ) {
        log.info("Redis 분산 락 이벤트 주문 요청. eventId={}, memberId={}", eventId, memberId);

        Long orderId = eventOrderFacade.createOrderWithRedisLock(eventId, memberId);

        EventOrderResponse response = new EventOrderResponse("주문 완료", orderId, eventId, memberId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }
}
