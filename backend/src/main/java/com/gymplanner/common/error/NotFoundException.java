package com.gymplanner.common.error;

import org.springframework.http.HttpStatus;

public class NotFoundException extends ApiException {

    public NotFoundException(String resource) {
        super(HttpStatus.NOT_FOUND, "not_found", resource + " not found");
    }
}
