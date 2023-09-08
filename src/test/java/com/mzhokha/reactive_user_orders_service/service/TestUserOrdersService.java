package com.mzhokha.reactive_user_orders_service.service;

import com.github.tomakehurst.wiremock.junit5.WireMockRuntimeInfo;
import com.github.tomakehurst.wiremock.junit5.WireMockTest;
import com.mzhokha.reactive_user_orders_service.model.UserRepository;
import com.mzhokha.reactive_user_orders_service.model.User;
import com.mzhokha.reactive_user_orders_service.external.client.OrderSearchServiceClient;
import com.mzhokha.reactive_user_orders_service.external.client.ProductInfoServiceClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@WireMockTest
class TestUserOrdersService {

    private final UserRepository userRepositoryMock = mock(UserRepository.class);

    private OrderSearchServiceClient orderSearchServiceClient;

    private ProductInfoServiceClient productInfoServiceClient;

    private UserOrdersService userOrdersService;

    @BeforeEach
    void setUp(WireMockRuntimeInfo wmRuntimeInfo) throws Exception {
        String wireMockBaseUrl = wmRuntimeInfo.getHttpBaseUrl();

        orderSearchServiceClient = new OrderSearchServiceClient();
        orderSearchServiceClient.setOrderSearchServiceBaseUrl(wireMockBaseUrl);
        orderSearchServiceClient.afterPropertiesSet();

        productInfoServiceClient = new ProductInfoServiceClient();
        productInfoServiceClient.setProductInfoServiceBaseUrl(wireMockBaseUrl);
        productInfoServiceClient.afterPropertiesSet();

        this.userOrdersService = new UserOrdersService(
                this.userRepositoryMock,
                this.orderSearchServiceClient,
                this.productInfoServiceClient);
    }

    @Test
    void getOrdersByUserId_whenUserIsPresentInDbAndOrdersPresentAndProductsPresent_returnUserOrders() {
        // given
        var userId = "user1";

        when(this.userRepositoryMock.findById(userId))
                .thenReturn(Mono.just(new User("user1", "John Doe", "123456789")));

        // when
        var userOrdersFlux = userOrdersService.getOrdersByUserId(userId);

        // then
        StepVerifier.create(userOrdersFlux.log())
                .expectNextCount(2)
                .verifyComplete();
    }

    // getOrdersByUserId_whenProductInfoResponseFailed_returnUserOrderWithoutProductInfo
    // getOrdersByUserId_whenProductInfoRequestTimedOut_returnUserOrderWithoutProductInfo
}