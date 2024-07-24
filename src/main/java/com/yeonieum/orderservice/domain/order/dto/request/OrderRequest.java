package com.yeonieum.orderservice.domain.order.dto.request;

import com.yeonieum.orderservice.domain.order.entity.OrderDetail;
import com.yeonieum.orderservice.domain.order.entity.PaymentInformation;
import com.yeonieum.orderservice.domain.order.entity.ProductOrderEntity;
import com.yeonieum.orderservice.domain.order.entity.ProductOrderListEntity;
import com.yeonieum.orderservice.global.enums.OrderStatusCode;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

public class OrderRequest {

    @Getter
    @NoArgsConstructor
    public static class OfRetrieve {
        OrderStatusCode orderStatusCode;

    }

    @Getter
    @NoArgsConstructor
    public static class OfUpdateOrderStatus {
        String orderId;
        OrderStatusCode orderStatusCode;
    }

    @Getter
    @NoArgsConstructor
    public static class OfBulkUpdateOrderStatus {
        List<String> orderIds;
        OrderStatusCode orderStatusCode;
    }

    @Getter
    @NoArgsConstructor
    public static class OfUpdateProductOrderStatus {
        String orderId;
        Long productId;
        OrderStatusCode orderStatusCode;
    }


    @Getter
    @NoArgsConstructor
    public static class OfCreation {
        Long customerId;
        Long memberCouponId;
        String storeName;
        ProductOrderList productOrderList;
        Recipient recipient;
        int originProductAmount;
        int totalDiscountAmount;
        int paymentAmount;
        int deliveryFee;
        String orderMemo;
        PaymentCard paymentCard;

        public OrderDetail toOrderDetailEntity(String memberId) {
            return OrderDetail.builder()
                    .orderMemo(this.getOrderMemo())
                    .deliveryAddress(this.getRecipient().getRecipientAddress())
                    .recipient(this.getRecipient().getRecipient())
                    .recipientPhoneNumber(this.getRecipient().getRecipientPhoneNumber())
                    .storeName(this.getStoreName())
                    .memberId(memberId)
                    .orderDateTime(LocalDateTime.now())
                    .orderList(this.getProductOrderList().toEntity()) // json 컨버터 객체 생성하기
                    .build();
        }

        public PaymentInformation toPaymentInformationEntity(OrderDetail orderDetail,
                                                             int canceledDiscountAmount,
                                                             int canceledPaymentAmount,
                                                             int canceledOriginProductPrice) {
            return PaymentInformation.builder()
                    .orderDetail(orderDetail)
                    .deliveryFee(this.getDeliveryFee())
                    .discountAmount(this.getTotalDiscountAmount() - canceledDiscountAmount)
                    .cardNumber(this.getPaymentCard().getCardNumber())
                    .paymentAmount(this.getPaymentAmount() - canceledPaymentAmount)
                    .originProductPrice(this.getOriginProductAmount() - canceledOriginProductPrice)
                    .build();
        }

        public void changePaymentAmount(int paymentAmount) {
            this.paymentAmount = paymentAmount;
        }
    }

    @Getter
    @NoArgsConstructor
    public static class PaymentCard {
        String cardNumber;
        String cardCompany;
    }

    @Getter
    @NoArgsConstructor
    public static class Recipient {
        String recipient;
        String recipientPhoneNumber;
        String recipientAddress;
    }

    @Getter
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class ProductOrder {
        Long productId;
        Long couponId;
        String name;
        int originPrice;
        int discountAmount;
        int finalPrice;
        int quantity;
        @Builder.Default
        OrderStatusCode status = OrderStatusCode.PENDING;
        // 상품 주문 상태
        public void changeStatus(OrderStatusCode status) {
            this.status = status;
        }

        public ProductOrderEntity toEntity() {
            return ProductOrderEntity.builder()
                    .productId(this.productId)
                    .name(this.getName())
                    .originPrice(this.getOriginPrice())
                    .discountAmount(this.getDiscountAmount())
                    .finalPrice(this.getFinalPrice())
                    .quantity(this.getQuantity())
                    .status(this.getStatus())
                    .build();
        }
    }

    @Getter
    @NoArgsConstructor
    public static class ProductOrderList {
        List<ProductOrder> productOrderList;

        public ProductOrderListEntity toEntity() {
            return ProductOrderListEntity.builder()
                    .productOrderEntityList(this.getProductOrderList().stream().map(productOrder ->
                            productOrder.toEntity()).collect(Collectors.toList()))
                    .build();
        }
    }
}
