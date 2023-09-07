package com.mzhokha.reactive_user_orders_service.external.client;

import com.mzhokha.reactive_user_orders_service.external.model.Order;
import com.mzhokha.reactive_user_orders_service.service.UserOrdersService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;

@Component
public class OrderSearchServiceClient implements InitializingBean {

    private static Logger log = LoggerFactory.getLogger(UserOrdersService.class);

    @Value("${order.search.service.base.url}")
    String orderSearchServiceBaseUrl;

    private WebClient webClient;

    @Override
    public void afterPropertiesSet() throws Exception {
        this.webClient = WebClient.builder()
                .baseUrl(this.orderSearchServiceBaseUrl + "/orderSearchService")
                .build();
    }

    // http://localhost:8083/orderSearchService/order/phone?phoneNumber=123456789
    // [{"phoneNumber":"123456789","orderNumber":"Order_0","productCode":"3852"},...]
    public Flux<Order> getOrdersByPhoneNumber(String phoneNumber) {
        return this.webClient.get().uri(uriBuilder ->
                        uriBuilder.path("/order/phone")
                                .queryParam("phoneNumber", phoneNumber)
                                .build()
                )
                .retrieve()
                .bodyToFlux(Order.class)
                .doOnNext(order -> log.debug("Using phoneNumber {} got response from orderSearchService, order: {}", phoneNumber, order));
    }

    public void setOrderSearchServiceBaseUrl(String orderSearchServiceBaseUrl) {
        this.orderSearchServiceBaseUrl = orderSearchServiceBaseUrl;
    }
}
