package com.yeonieum.orderservice.domain.order.service;

import com.yeonieum.orderservice.domain.order.dto.response.OrderResponse;
import com.yeonieum.orderservice.domain.order.entity.OrderDetail;
import com.yeonieum.orderservice.domain.order.entity.ProductOrderEntity;
import com.yeonieum.orderservice.domain.order.exception.OrderException;
import com.yeonieum.orderservice.domain.order.repository.OrderDetailRepository;
import com.yeonieum.orderservice.domain.order.repository.OrderStatusRepository;
import com.yeonieum.orderservice.global.enums.OrderStatusCode;
import com.yeonieum.orderservice.global.responses.ApiResponse;
import com.yeonieum.orderservice.infrastructure.feignclient.MemberServiceFeignClient;
import com.yeonieum.orderservice.infrastructure.feignclient.ProductServiceFeignClient;
import com.yeonieum.orderservice.infrastructure.feignclient.dto.response.RetrieveOrderInformationResponse;
import feign.FeignException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import java.util.*;

import java.util.stream.Collectors;

import static com.yeonieum.orderservice.domain.order.exception.OrderExceptionCode.ORDER_NOT_FOUND;

@Service
@RequiredArgsConstructor
public class OrderTrackingService {
    private final OrderDetailRepository orderDetailRepository;
    private final OrderStatusRepository orderStatusRepository;
    private final MemberServiceFeignClient memberServiceFeignClient;
    private final ProductServiceFeignClient productServiceFeignClient;

    /**
     * 고객용 주문 조회 서비스
     * @param customerId
     * @param orderStatusCode
     * @param pageable
     * @return
     */
    @Transactional(readOnly = true)
    public Page<OrderResponse.OfRetrieveForCustomer> retrieveOrdersForCustomer(Long customerId, OrderStatusCode orderStatusCode, String orderDetailId, LocalDateTime orderDateTime, String recipient, String recipientPhoneNumber, String recipientAddress, String memberId, String memberName, String memberPhoneNumber, LocalDate startDate, LocalDate endDate, Pageable pageable) {
        List<String> filteredMemberIds = null;
        ResponseEntity<ApiResponse<Map<String, OrderResponse.MemberInfo>>> memberInfoMapResponse = null;
        Map<String, OrderResponse.MemberInfo> memberMap = null;
        boolean isAvailableMemberService = true;
        boolean isFilteredMember = false;

        if (memberName != null || memberPhoneNumber != null) {
            isFilteredMember = true;
            try {
                memberInfoMapResponse = memberServiceFeignClient.getFilterMemberMap(memberName, memberPhoneNumber);
                if(!memberInfoMapResponse.getStatusCode().is2xxSuccessful()) {
                    isAvailableMemberService = false;
                }
            } catch (FeignException e) {
                e.printStackTrace();
                return Page.empty(pageable);
            }
            if (!isAvailableMemberService || memberInfoMapResponse.getBody().getResult().isEmpty()) {
                // 필터링된 멤버 ID가 없으면 비어 있는 페이지 반환
                return Page.empty(pageable);
            }
            memberMap = memberInfoMapResponse.getBody().getResult();
            filteredMemberIds = memberInfoMapResponse.getBody().getResult().values().stream().map(OrderResponse.MemberInfo::getMemberId).toList();
        }

        Page<OrderDetail> orderDetailsPage = orderDetailRepository.findOrders(customerId, orderStatusCode, orderDetailId, orderDateTime, recipient, recipientPhoneNumber, recipientAddress, memberId, isFilteredMember && isAvailableMemberService ? memberMap.values().stream().map(OrderResponse.MemberInfo::getMemberId).toList() : null, startDate, endDate, pageable);
        if(!isFilteredMember) {
            List<String> memberIds = orderDetailsPage.getContent().stream().map(orderDetail -> orderDetail.getMemberId()).toList();
            try {
                memberInfoMapResponse = memberServiceFeignClient.getOrderMemberInfo(memberIds);
                if(!memberInfoMapResponse.getStatusCode().is2xxSuccessful()) {
                    isAvailableMemberService = false;
                } else {
                    memberMap = memberInfoMapResponse.getBody().getResult();
                }
            } catch (FeignException e) {
                e.printStackTrace();
                isAvailableMemberService = false;
            }
        }

        List<OrderResponse.OfRetrieveForCustomer> convertedOrders = new ArrayList<>();
        List<Long> productIdList = orderDetailsPage.stream()
                .flatMap(orderDetail -> orderDetail.getOrderList()
                        .getProductOrderEntityList()
                        .stream()
                        .map(ProductOrderEntity::getProductId))
                .collect(Collectors.toList());

        boolean isAvailableProductService = true;
        ResponseEntity<ApiResponse<Set<RetrieveOrderInformationResponse>>> productResponse = null;

        try{
            productResponse = productServiceFeignClient.retrieveOrderProductInformation(productIdList);
            isAvailableProductService = productResponse.getStatusCode().is2xxSuccessful();
        } catch (FeignException e) {
            e.printStackTrace();
            isAvailableProductService = false;
        }

        ResponseEntity<ApiResponse<OrderResponse.MemberInfo>> memberResponse = null;
        for (OrderDetail orderDetail : orderDetailsPage) {
            OrderResponse.MemberInfo memberInfo = isAvailableMemberService ? memberMap.get(orderDetail.getMemberId()) : null;

            OrderResponse.OfRetrieveForCustomer orderResponse =
                    OrderResponse.OfRetrieveForCustomer.convertedBy(orderDetail, memberInfo, isAvailableProductService, isAvailableMemberService);

            if(isAvailableProductService) {
                Set<RetrieveOrderInformationResponse> productInformation = productResponse.getBody().getResult();
                final Map<Long, RetrieveOrderInformationResponse> productInformationMap = new HashMap<>();

                for(RetrieveOrderInformationResponse product : productInformation) {
                    productInformationMap.put(product.getProductId(), product);
                }

                orderResponse.getProductOrderList().getProductOrderList().stream().map(
                        productOrder -> {
                            productOrder.changeName(productInformationMap.get(productOrder.getProductId()).getProductName());
                            return productOrder;
                        }).collect(Collectors.toList());
            }
            convertedOrders.add(orderResponse);
        }
        return new PageImpl<>(convertedOrders, pageable, orderDetailsPage.getTotalElements());
    }


    /**
     * 고객용 주문상태별 주문 건수 조회 서비스
     * @param customerId
     * @param orderStatusCode
     * @return
     */
    @Transactional(readOnly = true)
    public Long retrieveTotalOrderCountForCustomer(Long customerId, OrderStatusCode orderStatusCode) {
        return orderDetailRepository.countByCustomerIdAndOrderStatus(customerId, orderStatusRepository.findByStatusName(orderStatusCode));
    }

    /**
     * 회원용 주문 리스트 조회 서비스
     * @param memberId
     * @param startDate
     * @param endDate
     * @param pageable
     * @return
     */
    // 대표 상품에 대해서만 가져오기
    @Transactional(readOnly = true)
    public Page<OrderResponse.OfRetrieveForMember> retrieveOrderForMember(String memberId, LocalDate startDate, LocalDate endDate, Pageable pageable) {
        Page<OrderDetail> orderDetailsPage =
                orderDetailRepository.findByMemberId(memberId, startDate.atStartOfDay(), endDate.atTime(23, 59, 59), pageable);
        List<Long> productIdList = orderDetailsPage.getContent().stream().map(orderDetail -> orderDetail.getMainProductId()).collect(Collectors.toList());

        boolean isAvailableProductService = true;
        ResponseEntity<ApiResponse<Set<RetrieveOrderInformationResponse>>> productResponse = null;
        try {
            productResponse = productServiceFeignClient.retrieveOrderProductInformation(productIdList);
            isAvailableProductService = productResponse.getStatusCode().is2xxSuccessful();
        } catch (FeignException e) {
            e.printStackTrace();
            isAvailableProductService = false;
        }

        if(isAvailableProductService) {
            Set<RetrieveOrderInformationResponse> productInformationList = productResponse.getBody().getResult();
            Map<Long, RetrieveOrderInformationResponse> productInformationMap = new HashMap();

            productInformationList.forEach(productInformation -> {
                productInformationMap.put(productInformation.getProductId(), productInformation);
            });

            return orderDetailsPage.map(orderDetail -> {
                return OrderResponse.OfRetrieveForMember
                        .convertedBy(orderDetail, productInformationMap.get(orderDetail.getMainProductId()), true);
            });
        }

        return orderDetailsPage.map(orderDetail -> OrderResponse.OfRetrieveForMember
                    .convertedBy(orderDetail, null, false));
    }



    /**
     * 회원용 주문 상세 조회 서비스
     * @param memberId
     *
     * @return
     */
    // 대표 상품에 대해서만 가져오기
    @Transactional(readOnly = true)
    public OrderResponse.OfRetrieveDetailForMember retrieveOrderDetailForMember(String memberId, String orderDetailId) {
       OrderDetail orderDetail = orderDetailRepository.findById(orderDetailId).orElseThrow(
                () -> new OrderException(ORDER_NOT_FOUND, HttpStatus.NOT_FOUND));

        List<Long> productIdList = orderDetail.getOrderList().getProductOrderEntityList()
                .stream().map(orderedProduct -> orderedProduct.getProductId()).collect(Collectors.toList());

        boolean isAvailableProductService = true;
        ResponseEntity<ApiResponse<Set<RetrieveOrderInformationResponse>>> productResponse = null;
        try {
            productResponse = productServiceFeignClient.retrieveOrderProductInformation(productIdList);
            isAvailableProductService = productResponse.getStatusCode().is2xxSuccessful();
        } catch (FeignException e) {
            e.printStackTrace();
            isAvailableProductService = false;
        }

        if(isAvailableProductService) {
            Set<RetrieveOrderInformationResponse> productInformationList = productResponse.getBody().getResult();
            Map<Long, RetrieveOrderInformationResponse> productInformationMap = new HashMap();

            productInformationList.forEach(productInformation -> {
                productInformationMap.put(productInformation.getProductId(), productInformation);
            });

            OrderResponse.OfRetrieveDetailForMember result = OrderResponse.OfRetrieveDetailForMember
                        .convertedBy(orderDetail, productInformationMap.values().stream().findFirst().get().getStoreName() , true);

            result.getProductOrderList().getProductOrderList().forEach(productOrder -> {
                productOrder.changeName(productInformationMap.get(productOrder.getProductId()).getProductName());
                productOrder.changeImage(productInformationMap.get(productOrder.getProductId()).getProductImage());
            });

            return result;
        }
        return OrderResponse.OfRetrieveDetailForMember
                .convertedBy(orderDetail, null, false);
    }
}