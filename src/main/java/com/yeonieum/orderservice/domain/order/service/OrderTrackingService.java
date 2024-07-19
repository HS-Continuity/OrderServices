package com.yeonieum.orderservice.domain.order.service;

import com.yeonieum.orderservice.domain.order.dto.response.OrderResponse;
import com.yeonieum.orderservice.domain.order.entity.OrderDetail;
import com.yeonieum.orderservice.domain.order.repository.OrderDetailRepository;
import com.yeonieum.orderservice.domain.order.repository.OrderStatusRepository;
import com.yeonieum.orderservice.global.enums.OrderStatusCode;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

@Service
@RequiredArgsConstructor
public class OrderTrackingService {
    private final OrderDetailRepository orderDetailRepository;
    private final OrderStatusRepository orderStatusRepository;

    /**
     * 고객용 주문 조회 서비스
     * @param customerId
     * @param orderStatusCode
     * @param pageable
     * @return
     */
    @Transactional(readOnly = true)
    public Page<OrderResponse.OfRetrieveForCustomer> getOrdersForCustomer(Long customerId, OrderStatusCode orderStatusCode, Pageable pageable) {
        Page<OrderDetail> orderDetailsPage =
                orderDetailRepository.findByCustomerIdAndOrderStatus(customerId, orderStatusRepository.findByStatusName(orderStatusCode), pageable);

        return orderDetailsPage.map(OrderResponse.OfRetrieveForCustomer::convertedBy);
    }

    /**
     * 고객용 주문상태별 주문 건수 조회 서비스
     * @param customerId
     * @param orderStatusCode
     * @return
     */
    @Transactional(readOnly = true)
    public Long getTotalOrderCountForCustomer(Long customerId, OrderStatusCode orderStatusCode) {
        return orderDetailRepository.countByCustomerIdAndOrderStatus(customerId, orderStatusRepository.findByStatusName(orderStatusCode));
    }

    /**
     * 회원용 주문 조회 서비스
     * @param memberId
     * @param startDate
     * @param endDate
     * @param pageable
     * @return
     */
    @Transactional(readOnly = true)
    public Page<OrderResponse.OfRetrieveForMember> retrieveOrderForMember(String memberId, LocalDate startDate, LocalDate endDate, Pageable pageable) {
        Page<OrderDetail> orderDetailsPage =
                orderDetailRepository.findByMemberId(memberId, startDate.atStartOfDay(), endDate.atTime(23, 59, 59), pageable);

        return orderDetailsPage.map(orderDetail -> {
            Long customerId = orderDetail.getCustomerId();
            String storeName = "상품서비스 외부api 호출 대체 예정"; // TODO : 외부 api 호출
            return OrderResponse.OfRetrieveForMember.convertedBy(orderDetail, storeName);
        });
    }
}