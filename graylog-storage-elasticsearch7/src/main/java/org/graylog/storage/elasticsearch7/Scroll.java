package org.graylog.storage.elasticsearch7;

import org.graylog.shaded.elasticsearch7.org.elasticsearch.action.search.SearchRequest;
import org.graylog.shaded.elasticsearch7.org.elasticsearch.action.search.SearchResponse;
import org.graylog.shaded.elasticsearch7.org.elasticsearch.search.builder.SearchSourceBuilder;
import org.graylog2.indexer.results.ScrollResult;
import org.graylog2.indexer.searches.ScrollCommand;

import javax.inject.Inject;
import java.util.Set;

public class Scroll {
    private static final String DEFAULT_SCROLLTIME = "1m";
    private final ElasticsearchClient client;
    private final ScrollResultES7.Factory scrollResultFactory;
    private final Search search;

    @Inject
    public Scroll(ElasticsearchClient client,
                  ScrollResultES7.Factory scrollResultFactory,
                  Search search) {
        this.client = client;
        this.scrollResultFactory = scrollResultFactory;
        this.search = search;
    }

    public ScrollResult scroll(ScrollCommand scrollCommand) {
        final SearchSourceBuilder searchQuery = search.create(scrollCommand);

        searchQuery.fetchSource(scrollCommand.fields().toArray(new String[0]), new String[0]);
        scrollCommand.batchSize()
                .ifPresent(batchSize -> searchQuery.size(Math.toIntExact(batchSize)));
        final SearchRequest request = scrollBuilder(searchQuery, scrollCommand.indices());

        final SearchResponse result = client.singleSearch(request, "Unable to perform scroll search");

        return scrollResultFactory.create(result, searchQuery.toString(), DEFAULT_SCROLLTIME, scrollCommand.fields(), scrollCommand.limit().orElse(-1));
    }

    private SearchRequest scrollBuilder(SearchSourceBuilder query, Set<String> indices) {
        return new SearchRequest(indices.toArray(new String[0]))
                .source(query)
                .scroll(DEFAULT_SCROLLTIME);
    }
}
