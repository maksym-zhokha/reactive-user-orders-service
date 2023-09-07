package com.mzhokha.reactive_user_orders_service.response;

public record UserOrder(
        String orderNumber,
        String userName,
        String phoneNumber,
        String productCode,
        String productName,
        String productId) {
}
