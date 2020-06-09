package org.graylog.storage.elasticsearch6;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.graylog2.Configuration;
import org.graylog2.indexer.results.ScrollResult;
import org.graylog2.indexer.searches.Searches;
import org.graylog2.indexer.searches.SearchesAdapter;
import org.graylog2.indexer.searches.SearchesIT;

import java.util.List;

public class SearchesES6IT extends SearchesIT {
    private SearchesAdapter createSearchesAdapter() {
        final ScrollResult.Factory scrollResultFactory = new ScrollResult.Factory() {
            @Override
            public ScrollResult create(io.searchbox.core.SearchResult initialResult, String query, List<String> fields) {
                return new ScrollResult(jestClient(), new ObjectMapper(), initialResult, query, fields);
            }

            @Override
            public ScrollResult create(io.searchbox.core.SearchResult initialResult, String query, String scroll, List<String> fields) {
                return new ScrollResult(jestClient(), new ObjectMapper(), initialResult, query, scroll, fields);
            }
        };

        return new SearchesAdapterES6(
                new Configuration(),
                new MultiSearch(jestClient()), new Scroll(scrollResultFactory, jestClient()),
                new SortOrderFactory()
        );
    }
    @Override
    public Searches createSearches() {
        return new Searches(
                indexRangeService,
                metricRegistry,
                streamService,
                indices,
                indexSetRegistry,
                createSearchesAdapter()
        );
    }
}
