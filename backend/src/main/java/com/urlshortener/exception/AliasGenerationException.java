package com.urlshortener.exception;

public class AliasGenerationException extends RuntimeException {
    public AliasGenerationException() {
        super("Unable to generate an available alias. Please try again.");
    }
}
