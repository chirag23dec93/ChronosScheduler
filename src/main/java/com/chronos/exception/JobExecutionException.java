package com.chronos.exception;

import org.springframework.http.HttpStatus;

public class JobExecutionException extends ChronosException {
    public JobExecutionException(String message) {
        super(message, HttpStatus.INTERNAL_SERVER_ERROR, "JOB_EXECUTION_ERROR");
    }

    public JobExecutionException(String message, Throwable cause) {
        super(message, HttpStatus.INTERNAL_SERVER_ERROR, "JOB_EXECUTION_ERROR", cause);
    }
}
