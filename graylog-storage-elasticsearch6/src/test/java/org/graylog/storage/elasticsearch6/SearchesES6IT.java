package org.graylog.storage.elasticsearch6;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.graylog.storage.elasticsearch6.testing.ElasticsearchInstanceES6;
import org.graylog.testing.elasticsearch.ElasticsearchInstance;
import org.graylog2.Configuration;
import org.graylog2.indexer.searches.Searches;
import org.graylog2.indexer.searches.SearchesAdapter;
import org.graylog2.indexer.searches.SearchesIT;
import org.junit.Rule;

import static org.graylog.storage.elasticsearch6.testing.TestUtils.jestClient;

public class SearchesES6IT extends SearchesIT {
    @Rule
    public final ElasticsearchInstance elasticsearch = ElasticsearchInstanceES6.create();

    @Override
    protected ElasticsearchInstance elasticsearch() {
        return this.elasticsearch;
    }

    private SearchesAdapter createSearchesAdapter() {
        final ScrollResultES6.Factory scrollResultFactory = (initialResult, query, scroll, fields, limit) -> new ScrollResultES6(
                jestClient(elasticsearch), new ObjectMapper(), initialResult, query, scroll, fields, limit
        );

        return new SearchesAdapterES6(
                new Configuration(),
                new MultiSearch(jestClient(elasticsearch)), new Scroll(scrollResultFactory, jestClient(elasticsearch)),
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
