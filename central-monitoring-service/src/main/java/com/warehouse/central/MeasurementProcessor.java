package com.warehouse.central;

import java.time.Clock;
import java.time.Duration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class MeasurementProcessor {
    private static final Logger log = LoggerFactory.getLogger(MeasurementProcessor.class);
    private final CentralProperties properties;
    private final Clock clock;

    @Autowired
    public MeasurementProcessor(CentralProperties properties) {
        this(properties, Clock.systemUTC());
    }

    MeasurementProcessor(CentralProperties properties, Clock clock) {
        this.properties = properties;
        this.clock = clock;
    }

    public void process(Measurement measurement) {
//        log.info("Processing measurement warehouse={} sensor={} type={} value={} receivedAt={}",
//                measurement.warehouseId(), measurement.sensorId(), measurement.type(),
//                measurement.value(), measurement.receivedAt());

        if (Duration.between(measurement.receivedAt(), clock.instant()).compareTo(properties.maxMeasurementAge()) > 0) {
            log.debug("Ignoring expired measurement warehouse={} sensor={}",
                    measurement.warehouseId(), measurement.sensorId());
            return;
        }
        double threshold = measurement.type() == MeasurementType.TEMPERATURE
                ? properties.temperatureThreshold() : properties.humidityThreshold();
        if (measurement.value() > threshold) {
            log.warn("ALARM warehouse={} sensor={} type={} value={} threshold={} receivedAt={}",
                    measurement.warehouseId(), measurement.sensorId(), measurement.type(),
                    measurement.value(), threshold, measurement.receivedAt());
        }
    }
}
