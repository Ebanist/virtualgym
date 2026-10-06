package com.gymplanner.common.error;

import org.springframework.http.HttpStatus;

/** Naruszenie reguły biznesowej przy poprawnym składniowo żądaniu (HTTP 422). */
public class BusinessRuleException extends ApiException {

    public BusinessRuleException(String code, String message) {
        super(HttpStatus.UNPROCESSABLE_ENTITY, code, message);
    }
}
