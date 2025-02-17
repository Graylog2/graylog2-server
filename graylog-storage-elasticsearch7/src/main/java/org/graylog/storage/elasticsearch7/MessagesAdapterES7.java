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

import com.codahale.metrics.Meter;
import com.codahale.metrics.MetricRegistry;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.inject.Inject;
import org.graylog.shaded.elasticsearch7.org.elasticsearch.ElasticsearchException;
import org.graylog.shaded.elasticsearch7.org.elasticsearch.action.bulk.BulkItemResponse;
import org.graylog.shaded.elasticsearch7.org.elasticsearch.action.bulk.BulkRequest;
import org.graylog.shaded.elasticsearch7.org.elasticsearch.action.bulk.BulkResponse;
import org.graylog.shaded.elasticsearch7.org.elasticsearch.action.get.GetRequest;
import org.graylog.shaded.elasticsearch7.org.elasticsearch.action.get.GetResponse;
import org.graylog.shaded.elasticsearch7.org.elasticsearch.action.index.IndexRequest;
import org.graylog.shaded.elasticsearch7.org.elasticsearch.client.indices.AnalyzeRequest;
import org.graylog.shaded.elasticsearch7.org.elasticsearch.client.indices.AnalyzeResponse;
import org.graylog.shaded.elasticsearch7.org.elasticsearch.common.xcontent.XContentType;
import org.graylog.shaded.elasticsearch7.org.elasticsearch.rest.RestStatus;
import org.graylog2.indexer.messages.ChunkedBulkIndexer;
import org.graylog2.indexer.messages.DocumentNotFoundException;
import org.graylog2.indexer.messages.Indexable;
import org.graylog2.indexer.messages.IndexingError;
import org.graylog2.indexer.messages.IndexingRequest;
import org.graylog2.indexer.messages.IndexingResult;
import org.graylog2.indexer.messages.IndexingResults;
import org.graylog2.indexer.messages.IndexingSuccess;
import org.graylog2.indexer.messages.Messages;
import org.graylog2.indexer.messages.MessagesAdapter;
import org.graylog2.indexer.messages.SerializationContext;
import org.graylog2.indexer.results.ResultMessage;
import org.graylog2.indexer.results.ResultMessageFactory;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

import static com.codahale.metrics.MetricRegistry.name;

public class MessagesAdapterES7 implements MessagesAdapter {
    static final String INDEX_BLOCK_ERROR = "cluster_block_exception";
    static final String MAPPER_PARSING_EXCEPTION = "mapper_parsing_exception";
    static final String INDEX_BLOCK_REASON = "blocked by: [TOO_MANY_REQUESTS/12/index read-only / allow delete (api)";
    static final String FLOOD_STAGE_WATERMARK = "blocked by: [TOO_MANY_REQUESTS/12/disk usage exceeded flood-stage watermark";
    static final String UNAVAILABLE_SHARDS_EXCEPTION = "unavailable_shards_exception";
    static final String PRIMARY_SHARD_NOT_ACTIVE_REASON = "primary shard is not active";

    static final String ILLEGAL_ARGUMENT_EXCEPTION = "illegal_argument_exception";
    static final String NO_WRITE_INDEX_DEFINED_FOR_ALIAS = "no write index is defined for alias";

    static final String CIRCUIT_BREAKING_EXCEPTION = "circuit_breaking_exception";
    static final String DATA_TOO_LARGE = "Data too large";

    private final ResultMessageFactory resultMessageFactory;
    private final ElasticsearchClient client;
    private final Meter invalidTimestampMeter;
    private final ChunkedBulkIndexer chunkedBulkIndexer;
    private final ObjectMapper objectMapper;

    @Inject
    public MessagesAdapterES7(ResultMessageFactory resultMessageFactory, ElasticsearchClient elasticsearchClient,
                              MetricRegistry metricRegistry, ChunkedBulkIndexer chunkedBulkIndexer, ObjectMapper objectMapper) {
        this.resultMessageFactory = resultMessageFactory;
        this.client = elasticsearchClient;
        this.invalidTimestampMeter = metricRegistry.meter(name(Messages.class, "invalid-timestamps"));
        this.chunkedBulkIndexer = chunkedBulkIndexer;
        this.objectMapper = objectMapper;
    }

    @Override
    public ResultMessage get(String messageId, String index) throws DocumentNotFoundException {
        final GetRequest getRequest = new GetRequest(index, messageId);

        final GetResponse result = this.client.execute((c, requestOptions) -> c.get(getRequest, requestOptions));

        if (!result.isExists()) {
            throw new DocumentNotFoundException(index, messageId);
        }

        return resultMessageFactory.parseFromSource(messageId, index, result.getSource());
    }

    @Override
    public List<String> analyze(String toAnalyze, String index, String analyzer) {
        final AnalyzeRequest analyzeRequest = AnalyzeRequest.withIndexAnalyzer(index, analyzer, toAnalyze);

        final AnalyzeResponse result = client.execute((c, requestOptions) -> c.indices().analyze(analyzeRequest, requestOptions));
        return result.getTokens().stream()
                .map(AnalyzeResponse.AnalyzeToken::getTerm)
                .collect(Collectors.toList());
    }

    @Override
    public IndexingResults bulkIndex(List<IndexingRequest> messageList) throws IOException {
        return chunkedBulkIndexer.index(messageList, this::runBulkRequest);
    }

    private ChunkedBulkIndexer.BulkIndexResult runBulkRequest(int indexedSuccessfully, IndexingResults previousResults, List<IndexingRequest> chunk) throws ChunkedBulkIndexer.EntityTooLargeException {
        final BulkRequest bulkRequest = createBulkRequest(chunk);

        final BulkResponse result;
        try {
            result = this.client.execute((c, requestOptions) -> c.bulk(bulkRequest, requestOptions));
        } catch (ElasticsearchException e) {
            for (ElasticsearchException cause : e.guessRootCauses()) {
                if (cause.status().equals(RestStatus.REQUEST_ENTITY_TOO_LARGE)) {
                    throw new ChunkedBulkIndexer.EntityTooLargeException(indexedSuccessfully, previousResults);
                }
                if (cause.status().equals(RestStatus.TOO_MANY_REQUESTS)) {
                    if (cause.getDetailedMessage().contains(CIRCUIT_BREAKING_EXCEPTION)) {
                        throw new ChunkedBulkIndexer.CircuitBreakerException(indexedSuccessfully, previousResults, durabilityFrom(cause));
                    }
                    throw new ChunkedBulkIndexer.TooManyRequestsException(indexedSuccessfully, previousResults);
                }
            }
            throw new org.graylog2.indexer.ElasticsearchException(e);
        }
        return new ChunkedBulkIndexer.BulkIndexResult(indexingResultsFrom(result, chunk), result::buildFailureMessage, result.getItems().length);
    }

    private ChunkedBulkIndexer.CircuitBreakerException.Durability durabilityFrom(ElasticsearchException elasticsearchException) {
        return Optional.ofNullable(elasticsearchException.getMetadata("es.durability"))
                .map(durabilities -> durabilities.get(0))
                .map(durability -> switch (durability) {
                    case "TRANSIENT" -> ChunkedBulkIndexer.CircuitBreakerException.Durability.Transient;
                    case "PERMANENT" -> ChunkedBulkIndexer.CircuitBreakerException.Durability.Permanent;
                    default -> throw new IllegalStateException("Invalid durability: " + durability);
                })
                .orElse(ChunkedBulkIndexer.CircuitBreakerException.Durability.Permanent);
    }

    private BulkRequest createBulkRequest(List<IndexingRequest> chunk) {
        final BulkRequest bulkRequest = new BulkRequest();
        chunk.forEach(request -> bulkRequest.add(
                indexRequestFrom(request)
        ));
        return bulkRequest;
    }

    private IndexingResults indexingResultsFrom(BulkResponse response, List<IndexingRequest> request) {
        final Map<Boolean, List<BulkItemResponse>> partitionedResults = Arrays.stream(response.getItems()).collect(Collectors.partitioningBy(BulkItemResponse::isFailed));
        final List<BulkItemResponse> failures = partitionedResults.get(true);
        final List<BulkItemResponse> successes = partitionedResults.get(false);

        final Map<String, Indexable> messageMap = request.stream()
                .map(IndexingRequest::message)
                .distinct()
                .collect(Collectors.toMap(Indexable::getId, Function.identity(), (a, b) -> a));

        return IndexingResults.create(indexingSuccessFrom(successes, messageMap), indexingErrorsFrom(failures, messageMap));
    }

    private List<IndexingError> indexingErrorsFrom(List<BulkItemResponse> failedItems, Map<String, Indexable> messageMap) {
        return indexingResultsFrom(failedItems, messageMap)
                .stream().filter(IndexingError.class::isInstance).map(IndexingError.class::cast).toList();
    }

    private List<IndexingSuccess> indexingSuccessFrom(List<BulkItemResponse> failedItems, Map<String, Indexable> messageMap) {
        return indexingResultsFrom(failedItems, messageMap)
                .stream().filter(IndexingSuccess.class::isInstance).map(IndexingSuccess.class::cast).toList();
    }

    private List<IndexingResult> indexingResultsFrom(List<BulkItemResponse> responses, Map<String, Indexable> messageMap) {
        return responses.stream()
                .map(item -> {
                    final Indexable message = messageMap.get(item.getId());
                    return indexingResultFromResponse(item, message);
                })
                .collect(Collectors.toList());
    }

    private IndexingResult indexingResultFromResponse(BulkItemResponse response, Indexable message) {
        if (response.isFailed()) {
            return IndexingError.create(message, response.getIndex(), errorTypeFromResponse(response), response.getFailureMessage());
        }
        return IndexingSuccess.create(message, response.getIndex());
    }

    private IndexingError.Type errorTypeFromResponse(BulkItemResponse item) {
        final ParsedElasticsearchException exception = ParsedElasticsearchException.from(item.getFailureMessage());
        switch (exception.type()) {
            case MAPPER_PARSING_EXCEPTION:
                return IndexingError.Type.MappingError;
            case INDEX_BLOCK_ERROR:
                if (exception.reason().contains(INDEX_BLOCK_REASON) || exception.reason().contains(FLOOD_STAGE_WATERMARK))
                    return IndexingError.Type.IndexBlocked;
            case UNAVAILABLE_SHARDS_EXCEPTION:
                if (exception.reason().contains(PRIMARY_SHARD_NOT_ACTIVE_REASON))
                    return IndexingError.Type.IndexBlocked;
            case ILLEGAL_ARGUMENT_EXCEPTION:
                if (exception.reason().contains(NO_WRITE_INDEX_DEFINED_FOR_ALIAS))
                    return IndexingError.Type.IndexBlocked;
            case CIRCUIT_BREAKING_EXCEPTION:
                if (exception.reason().contains(DATA_TOO_LARGE))
                    return IndexingError.Type.DataTooLarge;
            default:
                return IndexingError.Type.Unknown;
        }
    }

    private IndexRequest indexRequestFrom(IndexingRequest request) {
        final byte[] body;
        try {
            body = request.message().serialize(SerializationContext.of(objectMapper, this.invalidTimestampMeter));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return new IndexRequest(request.indexSet().getWriteIndexAlias())
                .id(request.message().getId())
                .source(body, XContentType.JSON);
    }
}
