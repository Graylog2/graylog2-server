package org.graylog.storage.elasticsearch7;

import org.graylog.storage.elasticsearch7.testing.ElasticsearchInstanceES7;
import org.graylog.testing.elasticsearch.ElasticsearchInstance;
import org.graylog2.indexer.searches.Searches;
import org.graylog2.indexer.searches.SearchesAdapter;
import org.graylog2.indexer.searches.SearchesIT;
import org.junit.Rule;

public class SearchesES7IT extends SearchesIT {
    @Rule
    public final ElasticsearchInstanceES7 elasticsearch = ElasticsearchInstanceES7.create();

    @Override
    protected ElasticsearchInstance elasticsearch() {
        return this.elasticsearch;
    }

    private SearchesAdapter createSearchesAdapter() {
        final ScrollResultES7.Factory scrollResultFactory = (initialResult, query, scroll, fields, limit) -> new ScrollResultES7(
                elasticsearch.elasticsearchClient(), initialResult, query, scroll, fields, limit
        );
        final SortOrderMapper sortOrderMapper = new SortOrderMapper();
        final boolean allowHighlighting = true;
        final boolean allowLeadingWildcardSearches = true;

        return new SearchesAdapterES7(elasticsearch.elasticsearchClient(),
                new Scroll(elasticsearch.elasticsearchClient(),
                        scrollResultFactory,
                        sortOrderMapper,
                        allowLeadingWildcardSearches,
                        allowHighlighting),
                new Search(sortOrderMapper,
                        allowHighlighting,
                        allowLeadingWildcardSearches));
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
