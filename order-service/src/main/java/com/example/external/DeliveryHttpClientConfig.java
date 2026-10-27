package com.example.external;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.support.RestClientAdapter;
import org.springframework.web.service.invoker.HttpServiceProxyFactory;

@Configuration
public class DeliveryHttpClientConfig {

    @Value("${delivery-service.base-url:http://localhost:8082}")
    private String deliveryServiceBaseUrl;

    @Bean
    RestClient deliveryRestClient() {
        return RestClient.builder()
                .baseUrl(deliveryServiceBaseUrl)
                .build();
    }

    @Bean
    DeliveryHttpClient deliveryHttpClient(RestClient deliveryRestClient) {
        return HttpServiceProxyFactory.builder()
                .exchangeAdapter(RestClientAdapter.create(deliveryRestClient))
                .build()
                .createClient(DeliveryHttpClient.class);
    }
}
