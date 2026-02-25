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
package org.graylog.storage.opensearch3;

import com.codahale.metrics.Meter;
import com.codahale.metrics.MetricRegistry;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.inject.Inject;
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
import org.graylog2.indexer.results.ResultMessage;
import org.graylog2.indexer.results.ResultMessageFactory;
import org.opensearch.client.json.JsonData;
import org.opensearch.client.opensearch._types.ErrorCause;
import org.opensearch.client.opensearch._types.ErrorResponse;
import org.opensearch.client.opensearch._types.OpenSearchException;
import org.opensearch.client.opensearch.core.BulkRequest;
import org.opensearch.client.opensearch.core.BulkResponse;
import org.opensearch.client.opensearch.core.GetRequest;
import org.opensearch.client.opensearch.core.GetResponse;
import org.opensearch.client.opensearch.core.bulk.BulkOperation;
import org.opensearch.client.opensearch.core.bulk.BulkResponseItem;
import org.opensearch.client.opensearch.indices.AnalyzeRequest;
import org.opensearch.client.opensearch.indices.AnalyzeResponse;
import org.opensearch.client.opensearch.indices.analyze.AnalyzeToken;
import org.opensearch.client.transport.httpclient5.ResponseException;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

import static com.codahale.metrics.MetricRegistry.name;
import static org.graylog2.shared.utilities.StringUtils.f;

public class MessagesAdapterOS implements MessagesAdapter {
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
    private final OfficialOpensearchClient client;
    private final Meter invalidTimestampMeter;
    private final ChunkedBulkIndexer chunkedBulkIndexer;
    private final ObjectMapper objectMapper;

    @Inject
    public MessagesAdapterOS(ResultMessageFactory resultMessageFactory, OfficialOpensearchClient openSearchClient,
                             MetricRegistry metricRegistry, ChunkedBulkIndexer chunkedBulkIndexer, ObjectMapper objectMapper) {
        this.resultMessageFactory = resultMessageFactory;
        this.client = openSearchClient;
        this.invalidTimestampMeter = metricRegistry.meter(name(Messages.class, "invalid-timestamps"));
        this.chunkedBulkIndexer = chunkedBulkIndexer;
        this.objectMapper = objectMapper;
    }

    @Override
    public ResultMessage get(String messageId, String index) throws DocumentNotFoundException {
        final GetRequest getRequest = GetRequest.of(r -> r
                .index(index)
                .id(messageId)
        );

        GetResponse<JsonData> result = client.sync(c -> c.get(getRequest, JsonData.class), "Error getting message");

        if (!result.found()) {
            throw new DocumentNotFoundException(index, messageId);
        }

        return resultMessageFactory.parseFromSource(messageId, index, OSSerializationUtils.toMap(result.source()));
    }

    @Override
    public List<String> analyze(String toAnalyze, String index, String analyzer) {
        final AnalyzeRequest analyzeRequest = AnalyzeRequest.of(r -> r
                .index(index)
                .analyzer(analyzer)
                .text(toAnalyze)
        );

        AnalyzeResponse result = client.sync(c -> c.indices().analyze(analyzeRequest), "Error analyzing message");
        return result.tokens().stream()
                .map(AnalyzeToken::token)
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
            result = this.client.syncWithoutErrorMapping().bulk(bulkRequest);
        } catch (ResponseException e) {
            if (e.status() == 413) { // REQUEST_ENTITY_TOO_LARGE
                throw new ChunkedBulkIndexer.EntityTooLargeException(indexedSuccessfully, previousResults);
            } else if (e.status() == 429) { // TOO_MANY_REQUESTS
                OpenSearchException error = toOpenSearchException(e);
                for (ErrorCause cause : error.error().rootCause()) {
                    if (cause.type().equalsIgnoreCase(CIRCUIT_BREAKING_EXCEPTION)) {
                        throw new ChunkedBulkIndexer.CircuitBreakerException(indexedSuccessfully, previousResults, durabilityFrom(cause));
                    }
                    throw new ChunkedBulkIndexer.TooManyRequestsException(indexedSuccessfully, previousResults);
                }
            }
            throw new org.graylog2.indexer.ElasticsearchException(e);
        } catch (IOException e) {
            throw new org.graylog2.indexer.ElasticsearchException(e);
        }
        return new ChunkedBulkIndexer.BulkIndexResult(indexingResultsFrom(result, chunk), () -> buildFailureMessage(result), result.items().size());
    }

    private OpenSearchException toOpenSearchException(ResponseException re) {
        String[] split = re.getMessage().split("\n");
        if (split.length != 2) {
            return new OpenSearchException(ErrorResponse.of(r -> r
                    .status(re.status())
                    .error(cause -> cause
                            .type("unknown")
                            .reason(re.getMessage())
                    )
            ));
        }
        String json = split[1];
        return new OpenSearchException(OSSerializationUtils.fromJson(json, ErrorResponse._DESERIALIZER));
    }

    private ChunkedBulkIndexer.CircuitBreakerException.Durability durabilityFrom(ErrorCause openSearchException) {
        return Optional.ofNullable(openSearchException.metadata().get("durability"))
                .map(durability ->
                        Arrays.stream(ChunkedBulkIndexer.CircuitBreakerException.Durability.values())
                                .filter(d -> d.name().equalsIgnoreCase(durability.toString()))
                                .findFirst()
                                .orElse(ChunkedBulkIndexer.CircuitBreakerException.Durability.Permanent)
                )
                .orElse(ChunkedBulkIndexer.CircuitBreakerException.Durability.Permanent);
    }

    private String buildFailureMessage(BulkResponse result) {
        StringBuilder sb = new StringBuilder();
        sb.append("failure in bulk execution:");
        for (int i = 0; i < result.items().size(); i++) {
            final var response = result.items().get(i);
            if (response.error() != null) {
                sb.append("\n[")
                        .append(response.id())
                        .append("]: index [")
                        .append(response.index())
                        .append("], id [")
                        .append(response.id())
                        .append("], message [")
                        .append(response.error().reason())
                        .append("]");
            }
        }
        return sb.toString();
    }

    private BulkRequest createBulkRequest(List<IndexingRequest> chunk) {
        final List<BulkOperation> operations = chunk.stream()
                .map(this::indexRequestFrom)
                .toList();
        return BulkRequest.of(r -> r.operations(operations));
    }

    private IndexingResults indexingResultsFrom(BulkResponse response, List<IndexingRequest> request) {
        final Map<Boolean, List<BulkResponseItem>> partitionedResults = response.items().stream().collect(Collectors.partitioningBy(r -> r.error() != null));
        final List<BulkResponseItem> failures = partitionedResults.get(true);
        final List<BulkResponseItem> successes = partitionedResults.get(false);

        final Map<String, Indexable> messageMap = request.stream()
                .map(IndexingRequest::message)
                .distinct()
                .collect(Collectors.toMap(Indexable::getId, Function.identity(), (a, b) -> a));

        return IndexingResults.create(indexingSuccessFrom(successes, messageMap), indexingErrorsFrom(failures, messageMap));
    }

    private List<IndexingError> indexingErrorsFrom(List<BulkResponseItem> failedItems, Map<String, Indexable> messageMap) {
        return indexingResultsFrom(failedItems, messageMap)
                .stream().filter(IndexingError.class::isInstance).map(IndexingError.class::cast).toList();
    }

    private List<IndexingSuccess> indexingSuccessFrom(List<BulkResponseItem> failedItems, Map<String, Indexable> messageMap) {
        return indexingResultsFrom(failedItems, messageMap)
                .stream().filter(IndexingSuccess.class::isInstance).map(IndexingSuccess.class::cast).toList();
    }

    private List<IndexingResult> indexingResultsFrom(List<BulkResponseItem> responses, Map<String, Indexable> messageMap) {
        return responses.stream()
                .map(item -> {
                    final Indexable message = messageMap.get(item.id());
                    return indexingResultFromResponse(item, message);
                })
                .collect(Collectors.toList());
    }

    private IndexingResult indexingResultFromResponse(BulkResponseItem response, Indexable message) {
        if (response.error() != null) {
            String errorMessage = createErrorMessage(response.error());
            return IndexingError.create(message, response.index(), errorTypeFromResponse(response), errorMessage);
        }
        return IndexingSuccess.create(message, response.index());
    }

    private String createErrorMessage(ErrorCause error) {
        StringBuilder errorMessage = new StringBuilder();
        ErrorCause current = error;
        while (current != null) {
            errorMessage.append(f("OpenSearchException[OpenSearch exception [type=%s, reason=%s]];", current.type(), current.reason()));
            current = current.causedBy();
            if (current != null) {
                errorMessage.append(" nested: ");
            }
        }
        return errorMessage.toString();
    }

    private IndexingError.Type errorTypeFromResponse(BulkResponseItem item) {
        final ErrorCause exception = item.error();
        switch (exception.type()) {
            case MAPPER_PARSING_EXCEPTION:
                return IndexingError.Type.MappingError;
            case INDEX_BLOCK_ERROR:
                if (exception.reason().contains(INDEX_BLOCK_REASON) || exception.reason().contains(FLOOD_STAGE_WATERMARK)) {
                    return IndexingError.Type.IndexBlocked;
                }
            case UNAVAILABLE_SHARDS_EXCEPTION:
                if (exception.reason().contains(PRIMARY_SHARD_NOT_ACTIVE_REASON)) {
                    return IndexingError.Type.IndexBlocked;
                }
            case ILLEGAL_ARGUMENT_EXCEPTION:
                if (exception.reason().contains(NO_WRITE_INDEX_DEFINED_FOR_ALIAS)) {
                    return IndexingError.Type.IndexBlocked;
                }
            case CIRCUIT_BREAKING_EXCEPTION:
                if (exception.reason().contains(DATA_TOO_LARGE)) {
                    return IndexingError.Type.DataTooLarge;
                }
            default:
                return IndexingError.Type.Unknown;
        }
    }

    private BulkOperation indexRequestFrom(IndexingRequest request) {
        return BulkOperation.of(o -> o
                .index(i -> i
                        .index(request.writeIndex())
                        .id(request.message().getId())
                        .document(request.message().toElasticSearchObject(objectMapper, invalidTimestampMeter))
                )
        );
    }
}
