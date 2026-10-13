package com.warehouse.warehouse;

import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

@Component
public class CentralServerClient {
    private final WebClient webClient;
    private final WarehouseProperties properties;

    public CentralServerClient(WebClient centralServerWebClient, WarehouseProperties properties) {
        this.webClient = centralServerWebClient;
        this.properties = properties;
    }

    public Mono<Void> sendMeasurement(Measurement measurement) {
        return webClient.post()
                .uri("/api/v1/measurements")
                .bodyValue(measurement)
                .retrieve()
                .toBodilessEntity()
                .timeout(properties.httpResponseTimeout())
                .then();
    }
}
