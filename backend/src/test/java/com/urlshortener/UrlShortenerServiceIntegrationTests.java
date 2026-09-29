package com.urlshortener;

import com.urlshortener.exception.DuplicateAliasException;
import com.urlshortener.model.ShortenUrlRequest;
import com.urlshortener.model.ShortenUrlResponse;
import com.urlshortener.repository.ShortenedUrlRepository;
import com.urlshortener.service.UrlShortenerService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.SingleConnectionDataSource;

import static org.junit.jupiter.api.Assertions.*;

public class UrlShortenerServiceIntegrationTests {

    private static final String BASE_URL = "http://localhost:8080";
    private UrlShortenerService service;

    @BeforeEach
    void setUp() {
        var dataSource = new SingleConnectionDataSource("jdbc:sqlite::memory:", true);
        var jdbcTemplate = new JdbcTemplate(dataSource);
        var repository = new ShortenedUrlRepository(jdbcTemplate);
        repository.init();
        service = new UrlShortenerService(repository);
    }

    @Test
    void shorten_WithValidUrl_ReturnsShortUrlResponse() {
        var request = new ShortenUrlRequest();
        request.setFullUrl("https://example.com/test");

        ShortenUrlResponse response = service.shorten(request, BASE_URL);

        assertNotNull(response.getAlias());
        assertEquals("https://example.com/test", response.getFullUrl());
        assertTrue(response.getShortUrl().startsWith(BASE_URL + "/"));
    }

    @Test
    void shorten_WithCustomAlias_ReturnsCustomAlias() {
        var request = new ShortenUrlRequest();
        request.setFullUrl("https://example.com");
        request.setCustomAlias("my-alias");

        ShortenUrlResponse response = service.shorten(request, BASE_URL);

        assertEquals("my-alias", response.getAlias());
        assertEquals(BASE_URL + "/my-alias", response.getShortUrl());
    }

    @Test
    void shorten_WithDuplicateAlias_PreservesOriginalUrl() {
        ShortenUrlRequest request = new ShortenUrlRequest();
        request.setFullUrl("https://example.com/original");
        request.setCustomAlias("example");
        service.shorten(request, BASE_URL);

        request.setFullUrl("https://example.com/replacement");

        assertThrows(DuplicateAliasException.class, () -> service.shorten(request, BASE_URL));
        assertEquals("https://example.com/original", service.getFullUrl("example"));
    }

    @Test
    void delete_RemovesSavedUrl() {
        ShortenUrlRequest request = new ShortenUrlRequest();
        request.setFullUrl("https://example.com/");
        request.setCustomAlias("example");
        service.shorten(request, BASE_URL);

        assertTrue(service.delete("example"));
        assertNull(service.getFullUrl("example"));
    }

    @Test
    void getFullUrl_ReturnsExistingUrl() {
        var request = new ShortenUrlRequest();
        request.setFullUrl("https://example.com");
        request.setCustomAlias("example");
        service.shorten(request, BASE_URL);

        String fullUrl = service.getFullUrl("example");

        assertEquals("https://example.com/", fullUrl);
    }
}
