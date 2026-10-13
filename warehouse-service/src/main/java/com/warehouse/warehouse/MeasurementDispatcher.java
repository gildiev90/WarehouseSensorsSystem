package com.warehouse.warehouse;

import java.time.Clock;
import java.time.Duration;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

@Service
public class MeasurementDispatcher {
    private static final Logger log = LoggerFactory.getLogger(MeasurementDispatcher.class);
    private final WarehouseProperties properties;
    private final CentralServerClient centralServerClient;
    private final Clock clock;
    private final ApplicationEventPublisher eventPublisher;
    private final LinkedHashMap<String, PendingRequest> pending = new LinkedHashMap<>();
    private int inFlight;

    MeasurementDispatcher(WarehouseProperties properties, CentralServerClient centralServerClient,
                          ApplicationEventPublisher eventPublisher) {
        this.properties = properties;
        this.clock = Clock.systemUTC();
        this.centralServerClient = centralServerClient;
        this.eventPublisher = eventPublisher;
    }

    public void submit(Measurement measurement, int port) {
        PendingRequest toSend = null;
        synchronized (this) {
            if (isExpired(measurement)) {
                log.debug("Dropping expired measurement before dispatch sensor={}", measurement.sensorId());
                return;
            }
            if (inFlight < properties.maxInFlightRequests()) {
                inFlight++;
                toSend = new PendingRequest(measurement, port);
            } else {
                queueLatest(new PendingRequest(measurement, port));
            }
        }
        if (toSend != null) {
            send(toSend);
        }
    }

    private void queueLatest(PendingRequest request) {
        Measurement measurement = request.measurement();
        if (pending.remove(measurement.sensorId()) != null) {
            log.debug("Replaced pending measurement sensor={}", measurement.sensorId());
        } else if (pending.size() >= properties.maxPendingSensors()) {
            Iterator<Map.Entry<String, PendingRequest>> iterator = pending.entrySet().iterator();
            String evicted = iterator.next().getKey();
            iterator.remove();
            log.debug("Evicted oldest pending measurement sensor={}", evicted);
        }
        pending.put(measurement.sensorId(), request);
    }

    private void send(PendingRequest request) {
        Measurement measurement = request.measurement();
        centralServerClient.sendMeasurement(measurement)
                .doOnSuccess(ignored -> {
                    eventPublisher.publishEvent(new CentralRequestCompletedEvent(request.port(),
                            measurement.receivedAt(), clock.instant(), RequestOutcome.SUCCESS));
                    log.debug("Forwarded measurement sensor={}", measurement.sensorId());
                })
                .doOnError(error -> {
                    eventPublisher.publishEvent(new CentralRequestCompletedEvent(request.port(),
                            measurement.receivedAt(), clock.instant(), RequestOutcome.ERROR));
                    log.warn("Unable to forward measurement sensor={}: {}", measurement.sensorId(), error.getMessage());
                })
                .onErrorResume(error -> Mono.empty())
                .doFinally(signal -> completeRequest())
                .subscribe();
    }

    private void completeRequest() {
        PendingRequest next = null;
        synchronized (this) {
            inFlight--;
            while (!pending.isEmpty() && next == null) {
                Iterator<PendingRequest> iterator = pending.values().iterator();
                PendingRequest candidate = iterator.next();
                iterator.remove();
                if (isExpired(candidate.measurement())) {
                    log.debug("Dropping expired queued measurement sensor={}", candidate.measurement().sensorId());
                } else {
                    inFlight++;
                    next = candidate;
                }
            }
        }
        if (next != null) {
            send(next);
        }
    }

    private boolean isExpired(Measurement measurement) {
        return Duration.between(measurement.receivedAt(), clock.instant())
                .compareTo(properties.maxMeasurementAge()) > 0;
    }

    synchronized int pendingCount() {
        return pending.size();
    }

    private record PendingRequest(Measurement measurement, int port) {
    }
}
