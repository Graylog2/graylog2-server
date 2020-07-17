package org.graylog.storage.elasticsearch7;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.Streams;
import org.graylog.shaded.elasticsearch7.org.elasticsearch.ElasticsearchException;
import org.graylog.shaded.elasticsearch7.org.elasticsearch.action.search.MultiSearchRequest;
import org.graylog.shaded.elasticsearch7.org.elasticsearch.action.search.MultiSearchResponse;
import org.graylog.shaded.elasticsearch7.org.elasticsearch.action.search.SearchRequest;
import org.graylog.shaded.elasticsearch7.org.elasticsearch.action.search.SearchResponse;
import org.graylog.shaded.elasticsearch7.org.elasticsearch.client.RequestOptions;
import org.graylog.shaded.elasticsearch7.org.elasticsearch.client.RestHighLevelClient;
import org.graylog2.indexer.IndexNotFoundException;

import javax.inject.Inject;
import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

import static com.google.common.base.Preconditions.checkArgument;

public class ElasticsearchClient {
    private final RestHighLevelClient client;

    @Inject
    public ElasticsearchClient(RestHighLevelClient client) {
        this.client = client;
    }

    public SearchResponse search(SearchRequest searchRequest, String errorMessage) {
        final MultiSearchRequest multiSearchRequest = new MultiSearchRequest()
                .add(searchRequest);

        final MultiSearchResponse result = this.execute((c, requestOptions) -> c.msearch(multiSearchRequest, requestOptions), errorMessage);

        return firstResponseFrom(result, errorMessage);
    }

    public SearchResponse singleSearch(SearchRequest searchRequest, String errorMessage) {
        return execute((c, requestOptions) -> c.search(searchRequest, requestOptions), errorMessage);
    }

    public List<MultiSearchResponse.Item> msearch(List<SearchRequest> searchRequests, String errorMessage) {
        final MultiSearchRequest multiSearchRequest = new MultiSearchRequest();

        searchRequests.forEach(multiSearchRequest::add);

        final MultiSearchResponse result = this.execute((c, requestOptions) -> c.msearch(multiSearchRequest, requestOptions), errorMessage);

        return Streams.stream(result)
                .collect(Collectors.toList());
    }

    private SearchResponse firstResponseFrom(MultiSearchResponse result, String errorMessage) {
        checkArgument(result != null);
        checkArgument(result.getResponses().length == 1);

        final MultiSearchResponse.Item firstResponse = result.getResponses()[0];
        if (firstResponse.getResponse() == null) {
            throw exceptionFrom(firstResponse.getFailure(), errorMessage);
        }

        return firstResponse.getResponse();
    }

    public <R> R execute(ThrowingBiFunction<RestHighLevelClient, RequestOptions, R, IOException> fn) {
        return execute(fn, "An error occurred: ");
    }

    public <R> R execute(ThrowingBiFunction<RestHighLevelClient, RequestOptions, R, IOException> fn, String errorMessage) {
        try {
            return fn.apply(client, requestOptions());
        } catch (Exception e) {
            throw exceptionFrom(e, errorMessage);
        }
    }

    public <R> R executeWithIOException(ThrowingBiFunction<RestHighLevelClient, RequestOptions, R, IOException> fn, String errorMessage) throws IOException {
        try {
            return fn.apply(client, requestOptions());
        } catch (IOException e) {
            throw e;
        } catch (Exception e) {
            throw exceptionFrom(e, errorMessage);
        }
    }

    private RequestOptions requestOptions() {
        return RequestOptions.DEFAULT;
    }

    private ElasticsearchException exceptionFrom(Exception e, String errorMessage) {
        if (e instanceof ElasticsearchException) {
            final ElasticsearchException elasticsearchException = (ElasticsearchException)e;
            if (isIndexNotFoundException(elasticsearchException)) {
                throw IndexNotFoundException.create(errorMessage + elasticsearchException.getResourceId(), elasticsearchException.getIndex().getName());
            }
        }
        return new ElasticsearchException(errorMessage, e);
    }

    private boolean isIndexNotFoundException(ElasticsearchException e) {
        return e.getMessage().contains("index_not_found_exception");
    }
}
