package com.warehouse.central;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.BAD_REQUEST)
public class InvalidMeasurementException extends RuntimeException {
    public InvalidMeasurementException() {
        super("Invalid measurement");
    }
}
