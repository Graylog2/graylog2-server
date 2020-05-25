package org.graylog.storage.elasticsearch6;

import io.searchbox.client.JestClient;
import io.searchbox.core.Search;
import org.graylog2.indexer.ElasticsearchException;
import org.graylog2.indexer.cluster.jest.JestUtils;
import org.graylog2.indexer.results.ScrollResult;

import javax.inject.Inject;
import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;

public class Scroll {
    private final ScrollResult.Factory scrollResultFactory;
    private final JestClient jestClient;

    @Inject
    public Scroll(ScrollResult.Factory scrollResultFactory, JestClient jestClient) {
        this.scrollResultFactory = scrollResultFactory;
        this.jestClient = jestClient;
    }

    public ScrollResult scroll(Search search, Supplier<String> errorMessage,  String query, String scrollTime, List<String> fields) {
        final io.searchbox.core.SearchResult result = JestUtils.execute(jestClient, search, errorMessage);
        final Optional<ElasticsearchException> elasticsearchException = JestUtils.checkForFailedShards(result);
        elasticsearchException.ifPresent(e -> { throw e; });
        return scrollResultFactory.create(result, query, scrollTime, fields);
    }
}
