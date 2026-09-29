package com.urlshortener.exception;

public class DuplicateAliasException extends RuntimeException {
    public DuplicateAliasException(String alias) {
        super("The alias '" + alias + "' is already taken.");
    }
}
