package com.yeonieum.orderservice.domain.statistics.repository;

import com.yeonieum.orderservice.domain.order.dto.response.OrderResponse;
import com.yeonieum.orderservice.global.enums.Gender;
import com.yeonieum.orderservice.global.enums.OrderType;

import java.util.List;

public interface StatisticsRepositoryCustom {

    List<OrderResponse.ProductOrderCount> findTop3ProductsByGender(Long customerId, Gender gender);
    List<OrderResponse.ProductOrderCount> findTop3ProductsByAgeRange(Long customerId, int ageRange);
    List<OrderResponse.ProductOrderCount> findAllProductsByOrderType(Long customerId, OrderType orderType);
}