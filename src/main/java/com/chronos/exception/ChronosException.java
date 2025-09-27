package com.chronos.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public class ChronosException extends RuntimeException {
    private final HttpStatus status;
    private final String code;

    public ChronosException(String message, HttpStatus status, String code) {
        super(message);
        this.status = status;
        this.code = code;
    }

    public ChronosException(String message, HttpStatus status, String code, Throwable cause) {
        super(message, cause);
        this.status = status;
        this.code = code;
    }
}
