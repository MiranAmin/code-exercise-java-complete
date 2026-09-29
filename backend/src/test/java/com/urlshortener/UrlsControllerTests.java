package com.urlshortener;

import com.urlshortener.controller.UrlsController;
import com.urlshortener.model.ShortenUrlRequest;
import com.urlshortener.model.ShortenUrlResponse;
import com.urlshortener.service.UrlShortenerService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;

import java.net.URI;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class UrlsControllerTests {
    private UrlShortenerService service;
    private UrlsController controller;

    @BeforeEach
    void setUp() {
        service = mock(UrlShortenerService.class);
        controller = new UrlsController(service);
    }

    @Test
    void shortenReturnsCreatedWithResponseBody() {
        ShortenUrlRequest request = new ShortenUrlRequest();
        request.setFullUrl("https://example.com/");
        MockHttpServletRequest context = new MockHttpServletRequest();
        context.setScheme("http");
        context.setServerName("localhost");
        context.setServerPort(8080);
        ShortenUrlResponse expected = new ShortenUrlResponse(
                "http://localhost:8080/example", "example", "https://example.com/");
        when(service.shorten(request, "http://localhost:8080")).thenReturn(expected);

        ResponseEntity<ShortenUrlResponse> response = controller.shorten(request, context);

        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        assertSame(expected, response.getBody());
    }

    @Test
    void redirectReturnsFoundWithLocation() {
        when(service.getFullUrl("example")).thenReturn("https://example.com/");
        ResponseEntity<Void> response = controller.redirectToUrl("example");

        assertEquals(HttpStatus.FOUND, response.getStatusCode());
        assertEquals(URI.create("https://example.com/"), response.getHeaders().getLocation());
    }

    @Test
    void redirectReturnsNotFoundForUnknownAlias() {
        when(service.getFullUrl("missing")).thenReturn(null);
        assertEquals(HttpStatus.NOT_FOUND, controller.redirectToUrl("missing").getStatusCode());
    }

    @Test
    void deleteReturnsNoContentWhenAliasIsDeleted() {
        when(service.delete("example")).thenReturn(true);
        assertEquals(HttpStatus.NO_CONTENT, controller.delete("example").getStatusCode());
    }

    @Test
    void deleteReturnsNotFoundForUnknownAlias() {
        when(service.delete("missing")).thenReturn(false);
        assertEquals(HttpStatus.NOT_FOUND, controller.delete("missing").getStatusCode());
    }
}
