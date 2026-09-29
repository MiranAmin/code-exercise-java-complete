package com.urlshortener;

import com.urlshortener.exception.AliasGenerationException;
import com.urlshortener.exception.ApiExceptionHandler;
import com.urlshortener.exception.DuplicateAliasException;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ApiExceptionHandlerTests {
    private final ApiExceptionHandler handler = new ApiExceptionHandler();

    @Test
    void duplicateAliasReturnsBadRequestWithError() {
        DuplicateAliasException exception = new DuplicateAliasException("taken");

        ResponseEntity<Map<String, String>> response = handler.handleDuplicateAlias(exception);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertEquals(Map.of("error", exception.getMessage()), response.getBody());
    }

    @Test
    void aliasGenerationFailureReturnsInternalServerErrorWithError() {
        AliasGenerationException exception = new AliasGenerationException();

        ResponseEntity<Map<String, String>> response = handler.handleAliasGeneration(exception);

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        assertEquals(Map.of("error", exception.getMessage()), response.getBody());
    }
}
