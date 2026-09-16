package com.teamportal.exception;

/** La operación viola una regla de negocio, por ejemplo un email repetido (HTTP 409). */
public class BusinessRuleException extends RuntimeException {
    public BusinessRuleException(String message) {
        super(message);
    }
}
