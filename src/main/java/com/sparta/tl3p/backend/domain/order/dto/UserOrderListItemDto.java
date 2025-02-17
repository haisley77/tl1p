package com.sparta.tl3p.backend.domain.order.dto;

import com.sparta.tl3p.backend.domain.order.enums.OrderType;
import com.sparta.tl3p.backend.domain.order.enums.PaymentMethod;
import com.sparta.tl3p.backend.domain.order.entity.Order;
import lombok.*;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserOrderListItemDto {
    private String orderId;
    private String storeId;
    private OrderType orderType;
    private PaymentMethod paymentMethod;
    private String deliveryAddress;
    private String storeRequest;
    private String createdAt;

    // 편의 생성자: Order 엔티티로부터 DTO 필드 초기화
    public UserOrderListItemDto(Order order) {
        this.orderId = order.getOrderId().toString();
        this.storeId = order.getStore().getId().toString();
        this.orderType = order.getOrderType();
        this.paymentMethod = order.getPaymentMethod();
        this.deliveryAddress = order.getDeliveryAddress();
        this.storeRequest = order.getStoreRequest();
        this.createdAt = order.getCreatedAt().toString();
    }
}
