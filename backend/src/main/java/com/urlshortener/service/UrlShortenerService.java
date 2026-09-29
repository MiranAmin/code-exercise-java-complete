package com.urlshortener.service;

import com.urlshortener.exception.AliasGenerationException;
import com.urlshortener.exception.DuplicateAliasException;
import com.urlshortener.model.ShortenUrlRequest;
import com.urlshortener.model.ShortenUrlResponse;
import com.urlshortener.model.UrlListItem;
import com.urlshortener.model.UrlPageResponse;
import com.urlshortener.repository.ShortenedUrlRepository;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.URISyntaxException;
import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;
import java.util.regex.Pattern;

@Service
public class UrlShortenerService {

    private static final String ALIAS_CHARS = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
    private static final int GENERATED_ALIAS_LENGTH = 7;
    private static final int MAX_ALIAS_GENERATION_ATTEMPTS = 10;
    private static final int MIN_ALIAS_LENGTH = 2;
    private static final int MAX_ALIAS_LENGTH = 64;
    private static final int MIN_PAGE_SIZE = 1;
    private static final int MAX_PAGE_SIZE = 100;
    private static final Pattern ALIAS_PATTERN = Pattern.compile(
            "[A-Za-z0-9-]{" + MIN_ALIAS_LENGTH + "," + MAX_ALIAS_LENGTH + "}");
    private static final Set<String> RESERVED_ALIASES = Set.of("urls", "shorten", "error");

    private final ShortenedUrlRepository repository;

    public UrlShortenerService(ShortenedUrlRepository repository) {
        this.repository = repository;
    }

    public ShortenUrlResponse shorten(ShortenUrlRequest request, String baseUrl) {
        String fullUrl = normalizeUrl(request.getFullUrl());
        String alias = request.getCustomAlias() == null || request.getCustomAlias().isBlank()
                ? generateAndSaveAlias(fullUrl)
                : saveCustomAlias(request.getCustomAlias().trim(), fullUrl);

        return new ShortenUrlResponse(baseUrl + "/" + alias, alias, fullUrl);
    }

    private String saveCustomAlias(String alias, String fullUrl) {
        if (!isValidAlias(alias)) {
            throw new IllegalArgumentException("Alias may only contain letters, numbers, and hyphens ("
                    + MIN_ALIAS_LENGTH + "–" + MAX_ALIAS_LENGTH + " characters).");
        }
        if (RESERVED_ALIASES.contains(alias)) {
            throw new IllegalArgumentException("The alias '" + alias + "' is reserved. Please choose another alias.");
        }
        repository.save(alias, fullUrl, Instant.now());
        return alias;
    }

    public String getFullUrl(String alias) {
        return repository.findFullUrlByAlias(alias).orElse(null);
    }

    public UrlPageResponse getAll(String baseUrl, int page, int size) {
        if (page < 0) {
            throw new IllegalArgumentException("page must be zero or greater.");
        }
        if (size < MIN_PAGE_SIZE || size > MAX_PAGE_SIZE) {
            throw new IllegalArgumentException("size must be between " + MIN_PAGE_SIZE + " and " + MAX_PAGE_SIZE + ".");
        }
        long offset = (long) page * size;
        List<UrlListItem> items = repository.findAll(baseUrl, size, offset);
        long totalItems = repository.count();
        long totalPages = totalItems / size + (totalItems % size == 0 ? 0 : 1);
        return new UrlPageResponse(items, page, size, totalItems, totalPages);
    }

    public boolean delete(String alias) {
        return repository.deleteByAlias(alias);
    }

    private static String generateAlias() {
        StringBuilder alias = new StringBuilder(GENERATED_ALIAS_LENGTH);
        for (int i = 0; i < GENERATED_ALIAS_LENGTH; i++) {
            int index = ThreadLocalRandom.current().nextInt(ALIAS_CHARS.length());
            alias.append(ALIAS_CHARS.charAt(index));
        }
        return alias.toString();
    }

    private String generateAndSaveAlias(String fullUrl) {
        for (int attempt = 0; attempt < MAX_ALIAS_GENERATION_ATTEMPTS; attempt++) {
            String alias = generateAlias();
            try {
                repository.save(alias, fullUrl, Instant.now());
                return alias;
            } catch (DuplicateAliasException ex) {
                // Alias collision. Try the next candidate.
            }
        }
        throw new AliasGenerationException();
    }

    private static boolean isValidAlias(String alias) {
        return alias != null && ALIAS_PATTERN.matcher(alias).matches();
    }

    private static String normalizeUrl(String fullUrl) {
        if (fullUrl == null || fullUrl.isBlank()) {
            throw new IllegalArgumentException("fullUrl is required.");
        }

        try {
            var uri = new URI(fullUrl.trim());
            if (uri.getScheme() == null || uri.getHost() == null) {
                throw new IllegalArgumentException("fullUrl must be a valid URL.");
            }

            var scheme = uri.getScheme().toLowerCase();
            if (!scheme.equals("http") && !scheme.equals("https")) {
                throw new IllegalArgumentException("fullUrl must be a valid URL.");
            }

            var path = uri.getPath();
            if (path == null || path.isBlank()) {
                path = "/";
            }

            return new URI(
                    uri.getScheme(),
                    uri.getUserInfo(),
                    uri.getHost(),
                    uri.getPort(),
                    path,
                    uri.getQuery(),
                    uri.getFragment()
            ).toString();
        } catch (URISyntaxException ex) {
            throw new IllegalArgumentException("fullUrl must be a valid URL.");
        }
    }

}
