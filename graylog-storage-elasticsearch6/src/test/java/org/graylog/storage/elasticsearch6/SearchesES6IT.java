package org.graylog.storage.elasticsearch6;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.graylog2.Configuration;
import org.graylog2.indexer.searches.Searches;
import org.graylog2.indexer.searches.SearchesAdapter;
import org.graylog2.indexer.searches.SearchesIT;

public class SearchesES6IT extends SearchesIT {
    private SearchesAdapter createSearchesAdapter() {
        final ScrollResultES6.Factory scrollResultFactory = (initialResult, query, scroll, fields) -> new ScrollResultES6(
                jestClient(), new ObjectMapper(), initialResult, query, scroll, fields
        );

        return new SearchesAdapterES6(
                new Configuration(),
                new MultiSearch(jestClient()), new Scroll(scrollResultFactory, jestClient()),
                new SortOrderMapper()
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
