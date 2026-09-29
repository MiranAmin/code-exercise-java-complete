package com.urlshortener.model;

import java.util.List;

public record UrlPageResponse(
        List<UrlListItem> items,
        int page,
        int size,
        long totalItems,
        long totalPages
) {}
