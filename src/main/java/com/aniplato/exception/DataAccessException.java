package com.aniplato.exception;

/** Wraps a SQLException so DAOs and services need no checked exceptions. This is a bug or an outage, not a rule failure. */
public class DataAccessException extends RuntimeException {
    public DataAccessException(String message, Throwable cause) {
        super(message, cause);
    }
}
