package com.mzhokha.reactive_user_orders_service.controller;

import com.mzhokha.reactive_user_orders_service.response.UserOrder;
import com.mzhokha.reactive_user_orders_service.service.UserOrdersService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;

@RestController
@RequestMapping("/userOrdersService")
public class UserOrdersController {

    @Autowired
    private UserOrdersService userOrdersService;

    @GetMapping(value = "/user/orders", produces = MediaType.APPLICATION_NDJSON_VALUE)
    public Flux<UserOrder> getUserOrders(@RequestParam String userId) {
        return this.userOrdersService.getOrdersByUserId(userId);
    }
}
