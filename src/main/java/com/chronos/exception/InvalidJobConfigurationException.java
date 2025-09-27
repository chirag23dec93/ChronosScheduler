package com.chronos.exception;

import org.springframework.http.HttpStatus;

public class InvalidJobConfigurationException extends ChronosException {
    public InvalidJobConfigurationException(String message) {
        super(message, HttpStatus.BAD_REQUEST, "INVALID_JOB_CONFIGURATION");
    }
}
