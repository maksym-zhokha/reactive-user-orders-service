package com.mzhokha.reactive_user_orders_service.external.client;

import com.mzhokha.reactive_user_orders_service.external.model.Product;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;

@Component
public class ProductInfoServiceClient implements InitializingBean {

    @Value("${product.info.service.base.url}")
    String productInfoServiceBaseUrl;

    private WebClient webClient;

    @Override
    public void afterPropertiesSet() throws Exception {
        this.webClient = WebClient.builder()
                .baseUrl(this.productInfoServiceBaseUrl + "/productInfoService")
                .build();
    }

    // http://localhost:8082/productInfoService/product/names?productCode=Milk
    // [{"productId":"111","productCode":"Milk","productName":"IceCream","score":6787.14},...]
    public Flux<Product> getProductsByCode(String productCode) {
        return this.webClient.get().uri(uriBuilder ->
                        uriBuilder.path("/product/names")
                                .queryParam("productCode", productCode)
                                .build())
                .retrieve()
                .bodyToFlux(Product.class);
    }

    public void setProductInfoServiceBaseUrl(String productInfoServiceBaseUrl) {
        this.productInfoServiceBaseUrl = productInfoServiceBaseUrl;
    }
}
