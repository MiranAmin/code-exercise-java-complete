package com.urlshortener;

import com.urlshortener.exception.AliasGenerationException;
import com.urlshortener.exception.DuplicateAliasException;
import com.urlshortener.model.ShortenUrlRequest;
import com.urlshortener.model.ShortenUrlResponse;
import com.urlshortener.repository.ShortenedUrlRepository;
import com.urlshortener.service.UrlShortenerService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.springframework.dao.DataAccessResourceFailureException;


import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class UrlShortenerServiceTests {
    private static final String BASE_URL = "http://localhost:8080";
    private static final String FULL_URL = "https://example.com/test";
    private static final int ATTEMPT_LIMIT = 10;

    private ShortenedUrlRepository repository;
    private UrlShortenerService service;
    private ShortenUrlRequest request;
    private ArgumentCaptor<String> candidates;

    @BeforeEach
    void setUp() {
        repository = mock(ShortenedUrlRepository.class);
        service = new UrlShortenerService(repository);
        request = new ShortenUrlRequest();
        request.setFullUrl(FULL_URL);
        candidates = ArgumentCaptor.forClass(String.class);
    }

    @Test
    void deleteReturnsTrueWhenAliasExists() {
        when(repository.deleteByAlias("example")).thenReturn(true);
        assertTrue(service.delete("example"));
        verify(repository).deleteByAlias("example");
    }

    @Test
    void deleteReturnsFalseWhenAliasDoesNotExist() {
        when(repository.deleteByAlias("missing")).thenReturn(false);
        assertFalse(service.delete("missing"));
        verify(repository).deleteByAlias("missing");
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"   ", "\t\n"})
    void generatesAndSavesAliasWhenCustomAliasIsAbsentOrBlank(String customAlias) {
        request.setCustomAlias(customAlias);

        ShortenUrlResponse result = service.shorten(request, BASE_URL);

        assertTrue(result.getAlias().matches("[A-Za-z0-9]{7}"));
        verify(repository).save(eq(result.getAlias()), eq(FULL_URL), any());
    }

    @Test
    void buildsResponseFromSavedAliasAndFullUrl() {
        ShortenUrlResponse result = service.shorten(request, BASE_URL);

        assertEquals(FULL_URL, result.getFullUrl());
        assertEquals(BASE_URL + "/" + result.getAlias(), result.getShortUrl());
    }

    @Test
    void retriesOccupiedCandidatesAndSavesTheAvailableAlias() {
        doThrow(new DuplicateAliasException("occupied"))
                .doThrow(new DuplicateAliasException("occupied"))
                .doNothing()
                .when(repository).save(anyString(), eq(FULL_URL), any());

        ShortenUrlResponse result = service.shorten(request, BASE_URL);
        verify(repository, times(3)).save(candidates.capture(), eq(FULL_URL), any());
        assertEquals(candidates.getAllValues().get(2), result.getAlias());
    }

    @Test
    void throwsAliasGenerationExceptionAfterTenCollisions() {
        doThrow(new DuplicateAliasException("occupied"))
                .when(repository).save(anyString(), eq(FULL_URL), any());

        assertThrows(AliasGenerationException.class,
                () -> service.shorten(request, BASE_URL));

        verify(repository, times(ATTEMPT_LIMIT)).save(anyString(), eq(FULL_URL), any());
    }

    @ParameterizedTest
    @ValueSource(strings = {"My-custom-42", " My-custom-42 ", "\tMy-custom-42\n"})
    void trimsAndPreservesCustomAlias(String customAlias) {
        request.setCustomAlias(customAlias);
        assertEquals("My-custom-42", service.shorten(request, BASE_URL).getAlias());
        verify(repository).save(eq("My-custom-42"), eq(FULL_URL), any());
    }

    @Test
    void rejectsOccupiedCustomAliasWithoutGeneratingAReplacement() {
        doThrow(new DuplicateAliasException("custom"))
                .when(repository)
                .save(eq("custom"), eq(FULL_URL), any());

        request.setCustomAlias("custom");

        assertThrows(DuplicateAliasException.class, () -> service.shorten(request, BASE_URL));

        verify(repository).save(any(), any(), any());
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"custom"})
    void propagatesDatabaseFailureWithoutRetrying(String customAlias) {
        request.setCustomAlias(customAlias);
        doThrow(new DataAccessResourceFailureException("Database unavailable"))
                .when(repository)
                .save(anyString(), eq(FULL_URL), any());

        assertThrows(DataAccessResourceFailureException.class, () -> service.shorten(request, BASE_URL));

        verify(repository).save(any(), any(), any());
    }
}
