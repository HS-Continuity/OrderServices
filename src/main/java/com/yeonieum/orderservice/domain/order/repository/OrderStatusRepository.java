package com.yeonieum.orderservice.domain.order.repository;

import com.yeonieum.orderservice.domain.order.entity.OrderStatus;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OrderStatusRepository extends JpaRepository<OrderStatus,Long> {
}