/*
 * Copyright (C) 2020 Graylog, Inc.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the Server Side Public License, version 1,
 * as published by MongoDB, Inc.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * Server Side Public License for more details.
 *
 * You should have received a copy of the Server Side Public License
 * along with this program. If not, see
 * <http://www.mongodb.com/licensing/server-side-public-license>.
 */
package org.graylog.storage.elasticsearch7;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.joschi.jadconfig.util.Duration;
import com.google.common.collect.Streams;
import org.graylog.shaded.elasticsearch7.org.apache.http.client.config.RequestConfig;
import org.graylog.shaded.elasticsearch7.org.elasticsearch.ElasticsearchException;
import org.graylog.shaded.elasticsearch7.org.elasticsearch.action.search.MultiSearchRequest;
import org.graylog.shaded.elasticsearch7.org.elasticsearch.action.search.MultiSearchResponse;
import org.graylog.shaded.elasticsearch7.org.elasticsearch.action.search.SearchRequest;
import org.graylog.shaded.elasticsearch7.org.elasticsearch.action.search.SearchResponse;
import org.graylog.shaded.elasticsearch7.org.elasticsearch.client.RequestOptions;
import org.graylog.shaded.elasticsearch7.org.elasticsearch.client.ResponseException;
import org.graylog.shaded.elasticsearch7.org.elasticsearch.client.RestHighLevelClient;
import org.graylog.storage.elasticsearch7.errors.ResponseError;
import org.graylog2.indexer.BatchSizeTooLargeException;
import org.graylog2.indexer.IndexNotFoundException;
import org.graylog2.indexer.InvalidWriteTargetException;
import org.graylog2.indexer.MasterNotDiscoveredException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.inject.Named;
import java.io.IOException;
import java.util.List;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static com.google.common.base.Preconditions.checkArgument;

public class ElasticsearchClient {
    private static final Pattern invalidWriteTarget = Pattern.compile("no write index is defined for alias \\[(?<target>[\\w_]+)\\]");

    private static final Logger LOG = LoggerFactory.getLogger(ElasticsearchClient.class);

    private final RestHighLevelClient client;
    private final boolean compressionEnabled;
    private final ObjectMapper objectMapper;

    @Inject
    public ElasticsearchClient(RestHighLevelClient client,
                               @Named("elasticsearch_compression_enabled") boolean compressionEnabled,
                               ObjectMapper objectMapper) {
        this.client = client;
        this.compressionEnabled = compressionEnabled;
        this.objectMapper = objectMapper;
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
        return compressionEnabled
                ? RequestOptions.DEFAULT.toBuilder()
                .addHeader("Accept-Encoding", "gzip")
                .addHeader("Content-type", "application/json")
                .build()
                : RequestOptions.DEFAULT;
    }

    private ElasticsearchException exceptionFrom(Exception e, String errorMessage) {
        if (e instanceof ElasticsearchException) {
            final ElasticsearchException elasticsearchException = (ElasticsearchException) e;
            if (isIndexNotFoundException(elasticsearchException)) {
                throw IndexNotFoundException.create(errorMessage + elasticsearchException.getResourceId(), elasticsearchException.getIndex().getName());
            }
            if (isMasterNotDiscoveredException(elasticsearchException)) {
                throw new MasterNotDiscoveredException();
            }
            if (isInvalidWriteTargetException(elasticsearchException)) {
                final Matcher matcher = invalidWriteTarget.matcher(elasticsearchException.getMessage());
                if (matcher.find()) {
                    final String target = matcher.group("target");
                    throw InvalidWriteTargetException.create(target);
                }
            }
            if (isBatchSizeTooLargeException(elasticsearchException)) {
                throw new BatchSizeTooLargeException(elasticsearchException.getMessage());
            }
        }
        return new ElasticsearchException(errorMessage, e);
    }

    private boolean isInvalidWriteTargetException(ElasticsearchException elasticsearchException) {
        try {
            final ParsedElasticsearchException parsedException = ParsedElasticsearchException.from(elasticsearchException.getMessage());
            return parsedException.reason().startsWith("no write index is defined for alias");
        } catch (Exception e) {
            return false;
        }
    }

    private boolean isMasterNotDiscoveredException(ElasticsearchException elasticsearchException) {
        try {
            final ParsedElasticsearchException parsedException = ParsedElasticsearchException.from(elasticsearchException.getMessage());
            return parsedException.type().equals("master_not_discovered_exception")
                    || (parsedException.type().equals("cluster_block_exception") && parsedException.reason().contains("no master"));
        } catch (Exception e) {
            return false;
        }
    }

    private boolean isIndexNotFoundException(ElasticsearchException elasticsearchException) {
        return elasticsearchException.getMessage().contains("index_not_found_exception");
    }

    private boolean isBatchSizeTooLargeException(ElasticsearchException elasticsearchException) {
        try {
            final ParsedElasticsearchException parsedException = ParsedElasticsearchException.from(elasticsearchException.getMessage());
            if (parsedException.type().equals("search_phase_execution_exception")) {
                ParsedElasticsearchException parsedCause = ParsedElasticsearchException.from(elasticsearchException.getRootCause().getMessage());
                return parsedCause.reason().contains("Batch size is too large");
            }
        } catch (Exception e) {
            return false;
        }
        return false;
    }

    public static RequestOptions withTimeout(RequestOptions requestOptions, Duration timeout) {
        final RequestConfig.Builder requestConfigBuilder = (requestOptions == null || requestOptions.getRequestConfig() == null)
                ? RequestConfig.custom()
                : RequestConfig.copy(requestOptions.getRequestConfig());
        final RequestConfig requestConfigWithTimeout = requestConfigBuilder
                .setSocketTimeout(Math.toIntExact(timeout.toMilliseconds()))
                .build();
        final RequestOptions.Builder requestOptionsBuilder = requestOptions == null
                ? RequestOptions.DEFAULT.toBuilder()
                : requestOptions.toBuilder();
        return requestOptionsBuilder
                .setRequestConfig(requestConfigWithTimeout)
                .build();

    }

    public Optional<ResponseError> parseResponseException(ElasticsearchException ex) {
        if (ex.getCause() != null) {
            final Throwable[] suppressed = ex.getCause().getSuppressed();
            if (suppressed.length > 0) {
                final Throwable realCause = suppressed[0];
                if (realCause instanceof ResponseException) {
                    try {
                        final ResponseError err = objectMapper.readValue(((ResponseException) realCause).getResponse().getEntity().getContent(), ResponseError.class);
                        return Optional.of(err);
                    } catch (IOException ioe) {
                        LOG.warn("Failed to parse exception", ioe);
                    }
                }
            }
        }
        return Optional.empty();
    }
}
