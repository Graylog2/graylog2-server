package org.graylog.storage.elasticsearch6;

import io.searchbox.client.JestClient;
import io.searchbox.core.Search;
import org.graylog2.indexer.ElasticsearchException;
import org.graylog.storage.elasticsearch6.jest.JestUtils;
import org.graylog2.indexer.results.ScrollResult;

import javax.inject.Inject;
import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;

public class Scroll {
    private final JestClient jestClient;
    private final ScrollResultES6.Factory scrollResultFactory;

    @Inject
    public Scroll(ScrollResultES6.Factory scrollResultFactory, JestClient jestClient) {
        this.scrollResultFactory = scrollResultFactory;
        this.jestClient = jestClient;
    }

    public ScrollResult scroll(Search search, Supplier<String> errorMessage,  String query, String scrollTime, List<String> fields) {
        return scroll(search, errorMessage, query, scrollTime, fields, -1);
    }

    public ScrollResult scroll(Search search, Supplier<String> errorMessage,  String query, String scrollTime, List<String> fields, int limit) {
        final io.searchbox.core.SearchResult result = JestUtils.execute(jestClient, search, errorMessage);
        final Optional<ElasticsearchException> elasticsearchException = JestUtils.checkForFailedShards(result);
        elasticsearchException.ifPresent(e -> { throw e; });
        return scrollResultFactory.create(result, query, scrollTime, fields, limit);
    }
}
