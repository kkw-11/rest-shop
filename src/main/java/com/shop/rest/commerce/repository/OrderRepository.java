package com.shop.rest.commerce.repository;

import com.shop.rest.commerce.entity.Order;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OrderRepository extends JpaRepository<Order, Long> {
}
