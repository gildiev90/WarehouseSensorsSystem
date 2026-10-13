package com.warehouse.central;

import java.time.Instant;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

@RestController
public class MeasurementController {
    private final MeasurementProcessor processor;

    public MeasurementController(MeasurementProcessor processor) {
        this.processor = processor;
    }

    @PostMapping("/api/v1/measurements")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public Mono<Void> accept(@RequestBody Mono<Measurement> body) {
        return body.doOnNext(this::validate).doOnNext(processor::process).then();
    }

    private void validate(Measurement measurement) {
        if (measurement == null || isBlank(measurement.warehouseId()) || isBlank(measurement.sensorId())
                || measurement.type() == null || measurement.value() == null
                || !Double.isFinite(measurement.value()) || measurement.receivedAt() == null) {
            throw new InvalidMeasurementException();
        }
        if (measurement.type() == MeasurementType.HUMIDITY
                && (measurement.value() < 0 || measurement.value() > 100)) {
            throw new InvalidMeasurementException();
        }
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
