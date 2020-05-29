package org.graylog.storage.elasticsearch6;

import io.searchbox.client.JestClient;
import io.searchbox.core.MultiSearch;
import io.searchbox.core.MultiSearchResult;
import io.searchbox.core.Search;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.graylog2.indexer.cluster.jest.JestUtils;
import org.graylog2.indexer.counts.CountsAdapter;

import javax.inject.Inject;
import java.util.List;

public class CountsAdapterES6 implements CountsAdapter {
    private final JestClient jestClient;

    @Inject
    public CountsAdapterES6(JestClient jestClient) {
        this.jestClient = jestClient;
    }

    @Override
    public long totalCount(List<String> indices) {
        final String query = new SearchSourceBuilder()
                .query(QueryBuilders.matchAllQuery())
                .size(0)
                .toString();
        final Search request = new Search.Builder(query)
                .addIndex(indices)
                .build();
        final MultiSearch multiSearch = new MultiSearch.Builder(request).build();
        final MultiSearchResult searchResult = JestUtils.execute(jestClient, multiSearch, () -> "Fetching message count failed for indices " + indices);
        final List<MultiSearchResult.MultiSearchResponse> responses = searchResult.getResponses();

        long total = 0L;
        for (MultiSearchResult.MultiSearchResponse response : responses) {
            if (response.isError) {
                throw JestUtils.specificException(() -> "Fetching message count failed for indices " + indices, response.error);
            }
            total += response.searchResult.getTotal();
        }

        return total;
    }
}
