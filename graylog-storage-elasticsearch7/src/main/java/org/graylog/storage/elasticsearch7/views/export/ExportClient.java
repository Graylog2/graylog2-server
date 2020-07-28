package org.graylog.storage.elasticsearch7.views.export;

import org.graylog.plugins.views.search.export.ExportException;
import org.graylog.shaded.elasticsearch7.org.elasticsearch.action.search.SearchRequest;
import org.graylog.shaded.elasticsearch7.org.elasticsearch.action.search.SearchResponse;
import org.graylog.shaded.elasticsearch7.org.elasticsearch.client.RequestOptions;
import org.graylog.shaded.elasticsearch7.org.elasticsearch.client.RestHighLevelClient;
import org.graylog.storage.elasticsearch7.ElasticsearchClient;
import org.graylog.storage.elasticsearch7.ThrowingBiFunction;
import org.graylog2.indexer.ElasticsearchException;

import javax.inject.Inject;
import java.io.IOException;

public class ExportClient {
    private final ElasticsearchClient client;

    @Inject
    public ExportClient(ElasticsearchClient client) {
        this.client = client;
    }

    public SearchResponse search(SearchRequest request, String errorMessage) {
        try {
            return this.client.search(request, errorMessage);
        } catch (Exception e) {
            throw wrapException(e);
        }
    }

    private ExportException wrapException(Exception e) {
        return new ExportException("Unable to complete export: ", new ElasticsearchException(e));
    }

    public SearchResponse singleSearch(SearchRequest request, String errorMessage) {
        try {
            return this.client.singleSearch(request, errorMessage);
        } catch (Exception e) {
            throw wrapException(e);
        }
    }

    public <R> R execute(ThrowingBiFunction<RestHighLevelClient, RequestOptions, R, IOException> fn, String errorMessage) {
        try {
            return this.client.execute(fn, errorMessage);
        } catch (Exception e) {
            throw wrapException(e);
        }
    }
}
