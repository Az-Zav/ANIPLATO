package com.aniplato.exception;

/** Thrown by services when a business rule is broken. Controllers show the message in a dialog. */
public class RuleViolationException extends RuntimeException {
    public RuleViolationException(String message) {
        super(message);
    }
}
