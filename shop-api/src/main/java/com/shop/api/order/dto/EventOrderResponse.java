package com.shop.api.order.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class EventOrderResponse {
    private String message;
    private Long orderId;
    private Long eventId;
    private Long memberId;
}
