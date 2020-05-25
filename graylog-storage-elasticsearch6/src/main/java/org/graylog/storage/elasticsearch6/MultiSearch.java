package org.graylog.storage.elasticsearch6;

import com.fasterxml.jackson.databind.JsonNode;
import io.searchbox.client.JestClient;
import io.searchbox.client.JestResult;
import io.searchbox.core.MultiSearchResult;
import io.searchbox.core.Search;
import io.searchbox.core.SearchResult;
import org.graylog2.indexer.ElasticsearchException;
import org.graylog2.indexer.FieldTypeException;
import org.graylog2.indexer.cluster.jest.JestUtils;
import org.graylog2.indexer.searches.SearchFailure;

import javax.inject.Inject;
import java.util.List;
import java.util.function.Supplier;

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

        return checkForFailedShards(response.searchResult);
    }

    public long tookMsFromSearchResult(JestResult searchResult) {
        final JsonNode tookMs = searchResult.getJsonObject().path("took");
        if (tookMs.isNumber()) {
            return tookMs.asLong();
        } else {
            throw new ElasticsearchException("Unexpected response structure: " + searchResult.getJsonString());
        }
    }

    public <T extends JestResult> T checkForFailedShards(T result) throws FieldTypeException {
        // unwrap shard failure due to non-numeric mapping. this happens when searching across index sets
        // if at least one of the index sets comes back with a result, the overall result will have the aggregation
        // but not considered failed entirely. however, if one shard has the error, we will refuse to respond
        // otherwise we would be showing empty graphs for non-numeric fields.
        final JsonNode shards = result.getJsonObject().path("_shards");
        final double failedShards = shards.path("failed").asDouble();

        if (failedShards > 0) {
            final SearchFailure searchFailure = new SearchFailure(shards);
            final List<String> nonNumericFieldErrors = searchFailure.getNonNumericFieldErrors();

            if (!nonNumericFieldErrors.isEmpty()) {
                throw new FieldTypeException("Unable to perform search query", nonNumericFieldErrors);
            }

            throw new ElasticsearchException("Unable to perform search query", searchFailure.getErrors());
        }

        return result;
    }
}
