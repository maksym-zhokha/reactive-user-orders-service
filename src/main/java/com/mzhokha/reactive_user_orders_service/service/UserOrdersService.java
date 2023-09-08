package com.mzhokha.reactive_user_orders_service.service;

import com.mzhokha.reactive_user_orders_service.model.UserRepository;
import com.mzhokha.reactive_user_orders_service.response.UserOrder;
import com.mzhokha.reactive_user_orders_service.model.User;
import com.mzhokha.reactive_user_orders_service.external.client.OrderSearchServiceClient;
import com.mzhokha.reactive_user_orders_service.external.client.ProductInfoServiceClient;
import com.mzhokha.reactive_user_orders_service.external.model.Order;
import com.mzhokha.reactive_user_orders_service.external.model.Product;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

import java.time.Duration;
import java.util.Collections;
import java.util.List;

import static com.mzhokha.reactive_user_orders_service.util.LogUtil.*;

@Service
public class UserOrdersService {

    private static Logger log = LoggerFactory.getLogger(UserOrdersService.class);

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private OrderSearchServiceClient orderSearchServiceClient;

    @Autowired
    private ProductInfoServiceClient productInfoServiceClient;

    public UserOrdersService(UserRepository userRepository,
                             OrderSearchServiceClient orderSearchServiceClient,
                             ProductInfoServiceClient productInfoServiceClient) {
        this.userRepository = userRepository;
        this.orderSearchServiceClient = orderSearchServiceClient;
        this.productInfoServiceClient = productInfoServiceClient;
    }

    record UserAndOrder(User user, Order order) {
    }

    record UserAndOrderAndProducts(User user, Order order, List<Product> products) {
    }

    /*
        Flow overview:
        DB: userId -> User
        order-search-service: User.phone -> Order 1, Order 2, Order 3
        product-info-service: Order.productCode -> Product 1, Product 2, Product 3
        return combined object from User, Order, Product

        The main idea of this logic to not wait for all the orders of user and retrieve products for each order as soon
        as order received.

        So that we are retrieving all the products (Product 1, Product 2, ...) for current order (Order 1),
        reduce them into one needed product and return combined UserOrder object
        in the same time while Order 2 is still returned from order-search-service.
     */
    public Flux<UserOrder> getOrdersByUserId(String userId) {
        log.info("Getting UserOrders for user: {}", userId);

        var userOrdersFlux = this.userRepository.findById(userId)
                .flatMapMany(user -> {
                    // log.info("Inside flatMapMany(user -> ...");
                    return this.orderSearchServiceClient.getOrdersByPhoneNumber(user.phone())
                            .doOnEach(logOnNext(order -> log.debug("Received order: {}", order)))
                            .map(order -> new UserAndOrder(user, order));
                })
                .flatMap(userAndOrder -> {
                    // log.info("Inside flatMap(userAndOrder -> ...");
                    return this.productInfoServiceClient.getProductsByCode(userAndOrder.order.productCode())
                            .timeout(Duration.ofSeconds(7))
                            .doOnEach(logOnError(throwable -> log.error("Error happened during fetching products by code {}", userAndOrder.order.productCode(), throwable)))
                            .onErrorReturn(Collections.emptyList())
                            .doOnEach(logOnNext(products -> log.debug("Received products: {}", products)))
                            .map(products -> new UserAndOrderAndProducts(userAndOrder.user(), userAndOrder.order(), products));
                })
                .map(userAndOrderAndProducts -> {
                            log.info("Inside map(userAndOrderAndProducts -> ... ");
                            var product = userAndOrderAndProducts.products().stream()
                                    .reduce((p1, p2) -> {
                                        if (p1.score() > p2.score()) {
                                            return p1;
                                        } else {
                                            return p2;
                                        }
                                    })
                                    .orElse(new Product(null, null, null, 0));

                            return new UserOrder(
                                    userAndOrderAndProducts.order().orderNumber(),
                                    userAndOrderAndProducts.user().name(),
                                    userAndOrderAndProducts.user().phone(),
                                    product.productCode(),
                                    product.productName(),
                                    product.productId());
                        }
                )
                .log()
                .contextWrite(ctx -> ctx.put(CONTEXT_REQUEST_ID, getRequestIdFromMdc()));

        return userOrdersFlux;
    }

}
