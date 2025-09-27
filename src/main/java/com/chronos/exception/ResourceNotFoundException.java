package com.chronos.exception;

import org.springframework.http.HttpStatus;

public class ResourceNotFoundException extends ChronosException {
    public ResourceNotFoundException(String message) {
        super(message, HttpStatus.NOT_FOUND, "RESOURCE_NOT_FOUND");
    }

    public static ResourceNotFoundException forResource(String resourceType, String identifier) {
        return new ResourceNotFoundException(
                String.format("%s with identifier '%s' not found", resourceType, identifier)
        );
    }
}
