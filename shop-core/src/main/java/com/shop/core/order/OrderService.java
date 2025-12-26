package com.shop.core.order;

public interface OrderService {
    Long createOrder(Long eventId, Object... params);
}
