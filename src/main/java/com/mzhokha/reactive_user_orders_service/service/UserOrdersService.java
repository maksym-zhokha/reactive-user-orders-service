package com.mzhokha.reactive_user_orders_service.service;

import com.mzhokha.reactive_user_orders_service.model.UserRepository;
import com.mzhokha.reactive_user_orders_service.response.UserOrder;
import com.mzhokha.reactive_user_orders_service.model.User;
import com.mzhokha.reactive_user_orders_service.external.client.OrderSearchServiceClient;
import com.mzhokha.reactive_user_orders_service.external.client.ProductInfoServiceClient;
import com.mzhokha.reactive_user_orders_service.external.model.Order;
import com.mzhokha.reactive_user_orders_service.external.model.Product;
import com.mzhokha.reactive_user_orders_service.util.LogUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.scheduler.Schedulers;

import java.time.Duration;

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

    record UserAndOrder(User user, Order order){}

    public Flux<UserOrder> getOrdersByUserId(String userId) {
        var userOrdersFlux = this.userRepository.findById(userId)
                .flatMapMany(user -> {
                    log.info("Inside flatMapMany with user");
                    return this.orderSearchServiceClient.getOrdersByPhoneNumber(user.phone())
                            .doOnEach(LogUtil.logOnNext(order -> log.info("Fetched order: {}", order)))
                            .map(order -> {
                                return new UserAndOrder(user, order);
                            })
                            .contextWrite(ctx -> ctx.put("contextRequestId", "testRequestId"));
                })
                .publishOn(Schedulers.parallel())
                .flatMap(userAndOrder -> {
                    log.info("Inside flatMap with userAndOrder");
                    return this.productInfoServiceClient
                            .getProductsByCode(userAndOrder.order.productCode())
                            .timeout(Duration.ofSeconds(5))
                            .onErrorReturn(new Product(null, null, null, 0))
                            .reduce((p1, p2) -> {
                                if (p1.score() > p2.score()) {
                                    return p1;
                                } else {
                                    return p2;
                                }
                            })
                            .map(product -> {
                                log.info("Mapping product including userAndOrder into UserOrder");
                                return new UserOrder(
                                        userAndOrder.order().orderNumber(),
                                        userAndOrder.user().name(),
                                        userAndOrder.user().phone(),
                                        product.productCode(),
                                        product.productName(),
                                        product.productId());
                            });
                })
                .contextWrite(ctx -> ctx.put("contextRequestId", "testRequestId"));

        return userOrdersFlux;
    }

}
