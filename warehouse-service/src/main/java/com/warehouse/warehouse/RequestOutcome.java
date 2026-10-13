package com.warehouse.warehouse;

public enum RequestOutcome {
    SUCCESS("success"),
    ERROR("error");

    private final String metricValue;

    RequestOutcome(String metricValue) {
        this.metricValue = metricValue;
    }

    public String metricValue() {
        return metricValue;
    }
}
