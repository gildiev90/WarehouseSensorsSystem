package com.warehouse.warehouse;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.netty.http.client.HttpClient;
import reactor.netty.resources.ConnectionProvider;

@Configuration
public class CentralServerClientConfiguration {
    @Bean(destroyMethod = "dispose")
    ConnectionProvider warehouseConnectionProvider(WarehouseProperties properties) {
        return ConnectionProvider.builder("warehouse-http")
                .maxConnections(properties.maxInFlightRequests())
                .pendingAcquireMaxCount(properties.maxInFlightRequests())
                .build();
    }

    @Bean
    WebClient centralServerWebClient(WarehouseProperties properties, ConnectionProvider warehouseConnectionProvider) {
        HttpClient httpClient = HttpClient.create(warehouseConnectionProvider)
                .responseTimeout(properties.httpResponseTimeout());

        return WebClient.builder()
                .baseUrl(properties.centralServiceBaseUrl())
                .clientConnector(new ReactorClientHttpConnector(httpClient))
                .build();
    }
}
