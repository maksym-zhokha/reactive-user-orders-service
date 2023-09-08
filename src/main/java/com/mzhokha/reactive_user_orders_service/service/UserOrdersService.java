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
import reactor.core.scheduler.Schedulers;

import java.time.Duration;

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

    public Flux<UserOrder> getOrdersByUserId(String userId) {
        log.info("Getting UserOrders for user: {}", userId);

        var userOrdersFlux = this.userRepository.findById(userId)
                .flatMapMany(user -> {
                    log.info("Inside flatMapMany(user -> ...");
                    return this.orderSearchServiceClient.getOrdersByPhoneNumber(user.phone())
                            .doOnEach(logOnNext(order -> log.debug("Received order: {}", order)))
                            .map(order -> new UserAndOrder(user, order));
                })
                //.publishOn(Schedulers.parallel()) //todo: are you sure?
                .flatMap(userAndOrder -> {
                    log.info("Inside flatMap(userAndOrder -> ...");
                    return this.productInfoServiceClient
                            // todo: doesn't make sense to have Flux because endpoint is not reactive, so better to have Mono and log whole response at once
                            .getProductsByCode(userAndOrder.order.productCode())
                            .timeout(Duration.ofSeconds(5))
                            .doOnEach(logOnError(throwable -> log.error("Error happened during fetching product with code {}", userAndOrder.order.productCode(), throwable)))
                            .onErrorReturn(new Product(null, null, null, 0))
                            .doOnEach(logOnNext(product -> log.debug("Received product: {}", product)))
                            .reduce((p1, p2) -> {
                                if (p1.score() > p2.score()) {
                                    return p1;
                                } else {
                                    return p2;
                                }
                            })
                            .map(product -> {
                                log.info("Inside map(product -> ... ");
                                return new UserOrder(
                                        userAndOrder.order().orderNumber(),
                                        userAndOrder.user().name(),
                                        userAndOrder.user().phone(),
                                        product.productCode(),
                                        product.productName(),
                                        product.productId());
                            });
                })
                .contextWrite(ctx -> ctx.put(CONTEXT_REQUEST_ID, getRequestIdFromMdc()));

        return userOrdersFlux;
    }

}
