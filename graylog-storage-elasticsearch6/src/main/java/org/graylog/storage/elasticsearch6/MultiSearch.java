package org.graylog.storage.elasticsearch6;

import com.fasterxml.jackson.databind.JsonNode;
import io.searchbox.client.JestClient;
import io.searchbox.client.JestResult;
import io.searchbox.core.MultiSearchResult;
import io.searchbox.core.Search;
import io.searchbox.core.SearchResult;
import org.graylog2.indexer.ElasticsearchException;
import org.graylog.storage.elasticsearch6.jest.JestUtils;

import javax.inject.Inject;
import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;

import static org.graylog.storage.elasticsearch6.jest.JestUtils.checkForFailedShards;

public class MultiSearch {
    private final JestClient jestClient;

    @Inject
    public MultiSearch(JestClient jestClient) {
        this.jestClient = jestClient;
    }

    public SearchResult wrap(Search search, Supplier<String> errorMessage) {
        final io.searchbox.core.MultiSearch multiSearch = new io.searchbox.core.MultiSearch.Builder(search).build();
        final MultiSearchResult multiSearchResult = JestUtils.execute(jestClient, multiSearch, errorMessage);

        final List<MultiSearchResult.MultiSearchResponse> responses = multiSearchResult.getResponses();
        if (responses.size() != 1) {
            throw new ElasticsearchException("Expected exactly 1 search result, but got " + responses.size());
        }

        final MultiSearchResult.MultiSearchResponse response = responses.get(0);
        if (response.isError) {
            throw JestUtils.specificException(errorMessage, response.error);
        }

        final Optional<ElasticsearchException> elasticsearchException = checkForFailedShards(response.searchResult);
        elasticsearchException.ifPresent(e -> { throw e; });
        return response.searchResult;
    }

    public long tookMsFromSearchResult(JestResult searchResult) {
        final JsonNode tookMs = searchResult.getJsonObject().path("took");
        if (tookMs.isNumber()) {
            return tookMs.asLong();
        } else {
            throw new ElasticsearchException("Unexpected response structure: " + searchResult.getJsonString());
        }
    }
}
