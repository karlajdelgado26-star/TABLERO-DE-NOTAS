package com.teamportal.exception;

/** El usuario está autenticado pero no puede realizar esta acción (HTTP 403). */
public class ForbiddenOperationException extends RuntimeException {
    public ForbiddenOperationException(String message) {
        super(message);
    }
}
