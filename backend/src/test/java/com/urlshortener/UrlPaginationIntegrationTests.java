package com.urlshortener;

import com.urlshortener.controller.UrlsController;
import com.urlshortener.exception.ApiExceptionHandler;
import com.urlshortener.repository.ShortenedUrlRepository;
import com.urlshortener.service.UrlShortenerService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.SingleConnectionDataSource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.Instant;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class UrlPaginationIntegrationTests {
    private SingleConnectionDataSource dataSource;
    private JdbcTemplate jdbc;
    private ShortenedUrlRepository repository;
    private MockMvc mvc;

    @BeforeEach
    void setUp() {
        dataSource = new SingleConnectionDataSource("jdbc:sqlite::memory:", true);
        jdbc = new JdbcTemplate(dataSource);
        repository = new ShortenedUrlRepository(jdbc);
        repository.init();
        mvc = MockMvcBuilders.standaloneSetup(new UrlsController(new UrlShortenerService(repository)))
                .setControllerAdvice(new ApiExceptionHandler()).build();
        // Insert out of chronological order; equal timestamps must use descending ID.
        save("recent", "2026-09-27T12:00:10Z");
        save("oldest", "2026-09-27T12:00:09Z");
        save("recent-tie", "2026-09-27T12:00:10Z");
    }

    @AfterEach
    void tearDown() {
        dataSource.destroy();
    }

    @Test
    void returnsDefaultPageAndMetadata() throws Exception {
        mvc.perform(get("/urls"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items", hasSize(3)))
                .andExpect(jsonPath("$.page").value(0))
                .andExpect(jsonPath("$.size").value(10))
                .andExpect(jsonPath("$.totalItems").value(3))
                .andExpect(jsonPath("$.totalPages").value(1));
    }

    @Test
    void ordersPageByTimestampThenIdAndIncludesLinkDetails() throws Exception {
        mvc.perform(get("/urls?page=0&size=2"))
                .andExpect(jsonPath("$.items", hasSize(2)))
                .andExpect(jsonPath("$.items[0].alias").value("recent-tie"))
                .andExpect(jsonPath("$.items[1].alias").value("recent"))
                .andExpect(jsonPath("$.items[0].fullUrl").value("https://example.com/recent-tie"))
                .andExpect(jsonPath("$.items[0].shortUrl").value("http://localhost/recent-tie"));
    }

    @Test
    void returnsPartialLastPageWithTotalsAcrossAllRecords() throws Exception {
        mvc.perform(get("/urls?page=1&size=2"))
                .andExpect(jsonPath("$.items", hasSize(1)))
                .andExpect(jsonPath("$.items[0].alias").value("oldest"))
                .andExpect(jsonPath("$.page").value(1))
                .andExpect(jsonPath("$.size").value(2))
                .andExpect(jsonPath("$.totalItems").value(3))
                .andExpect(jsonPath("$.totalPages").value(2));
    }

    @Test
    void doesNotAddAnExtraPageForAnExactMultiple() throws Exception {
        mvc.perform(get("/urls?size=1"))
                .andExpect(jsonPath("$.items", hasSize(1)))
                .andExpect(jsonPath("$.totalPages").value(3));
    }

    @Test
    void emptyDatabaseHasNoItemsOrPages() throws Exception {
        jdbc.update("DELETE FROM shortened_urls");

        mvc.perform(get("/urls"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items", hasSize(0)))
                .andExpect(jsonPath("$.totalItems").value(0))
                .andExpect(jsonPath("$.totalPages").value(0));
    }

    @ParameterizedTest
    @CsvSource({"2, 2", "2147483647, 100"})
    void outOfRangePagesAreEmptyButPreserveTotals(int page, int size) throws Exception {
        mvc.perform(get("/urls").param("page", Integer.toString(page)).param("size", Integer.toString(size)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items", hasSize(0)))
                .andExpect(jsonPath("$.page").value(page))
                .andExpect(jsonPath("$.totalItems").value(3));
    }

    @ParameterizedTest
    @CsvSource({"-1, 10", "0, 0", "0, -1", "0, 101"})
    void rejectsInvalidPageOrSize(int page, int size) throws Exception {
        mvc.perform(get("/urls").param("page", Integer.toString(page)).param("size", Integer.toString(size)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").isNotEmpty());
    }

    @ParameterizedTest
    @CsvSource({"page, abc", "size, 1.5", "page, 2147483648"})
    void rejectsParametersThatCannotBeBoundToIntegers(String parameter, String value) throws Exception {
        mvc.perform(get("/urls").param(parameter, value))
                .andExpect(status().isBadRequest());
    }

    private void save(String alias, String createdAt) {
        repository.save(alias, "https://example.com/" + alias, Instant.parse(createdAt));
    }
}
