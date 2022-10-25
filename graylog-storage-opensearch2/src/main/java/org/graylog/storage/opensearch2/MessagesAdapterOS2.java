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
package org.graylog.storage.opensearch2;

import com.codahale.metrics.Meter;
import com.codahale.metrics.MetricRegistry;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.Iterables;
import org.graylog2.indexer.messages.ChunkedBulkIndexer;
import org.graylog2.indexer.messages.DocumentNotFoundException;
import org.graylog2.indexer.messages.Indexable;
import org.graylog2.indexer.messages.IndexingRequest;
import org.graylog2.indexer.messages.Messages;
import org.graylog2.indexer.messages.MessagesAdapter;
import org.graylog2.indexer.results.ResultMessage;
import org.graylog.shaded.opensearch2.org.opensearch.OpenSearchException;
import org.graylog.shaded.opensearch2.org.opensearch.action.bulk.BulkItemResponse;
import org.graylog.shaded.opensearch2.org.opensearch.action.bulk.BulkRequest;
import org.graylog.shaded.opensearch2.org.opensearch.action.bulk.BulkResponse;
import org.graylog.shaded.opensearch2.org.opensearch.action.get.GetRequest;
import org.graylog.shaded.opensearch2.org.opensearch.action.get.GetResponse;
import org.graylog.shaded.opensearch2.org.opensearch.action.index.IndexRequest;
import org.graylog.shaded.opensearch2.org.opensearch.client.indices.AnalyzeRequest;
import org.graylog.shaded.opensearch2.org.opensearch.client.indices.AnalyzeResponse;
import org.graylog.shaded.opensearch2.org.opensearch.common.xcontent.XContentType;
import org.graylog.shaded.opensearch2.org.opensearch.rest.RestStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import static com.codahale.metrics.MetricRegistry.name;

public class MessagesAdapterOS2 implements MessagesAdapter {
    private static final Logger LOG = LoggerFactory.getLogger(MessagesAdapterOS2.class);
    static final String INDEX_BLOCK_ERROR = "cluster_block_exception";
    static final String MAPPER_PARSING_EXCEPTION = "mapper_parsing_exception";
    static final String INDEX_BLOCK_REASON = "blocked by: [TOO_MANY_REQUESTS/12/index read-only / allow delete (api)";
    static final String FLOOD_STAGE_WATERMARK = "blocked by: [TOO_MANY_REQUESTS/12/disk usage exceeded flood-stage watermark";
    static final String UNAVAILABLE_SHARDS_EXCEPTION = "unavailable_shards_exception";
    static final String PRIMARY_SHARD_NOT_ACTIVE_REASON = "primary shard is not active";

    static final String ILLEGAL_ARGUMENT_EXCEPTION = "illegal_argument_exception";
    static final String NO_WRITE_INDEX_DEFINED_FOR_ALIAS = "no write index is defined for alias";

    private final OpenSearchClient client;
    private final Meter invalidTimestampMeter;
    private final ChunkedBulkIndexer chunkedBulkIndexer;
    private final ObjectMapper objectMapper;

    @Inject
    public MessagesAdapterOS2(OpenSearchClient openSearchClient, MetricRegistry metricRegistry, ChunkedBulkIndexer chunkedBulkIndexer, ObjectMapper objectMapper) {
        this.client = openSearchClient;
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

        return ResultMessage.parseFromSource(messageId, index, result.getSource());
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
    public List<Messages.IndexingError> bulkIndex(List<IndexingRequest> messageList) throws IOException {
        return chunkedBulkIndexer.index(messageList, this::bulkIndexChunked);
    }

    private List<Messages.IndexingError> bulkIndexChunked(ChunkedBulkIndexer.Chunk command) throws ChunkedBulkIndexer.EntityTooLargeException {
        final List<IndexingRequest> messageList = command.requests;
        final int offset = command.offset;
        final int chunkSize = command.size;

        if (messageList.isEmpty()) {
            return Collections.emptyList();
        }

        final Iterable<List<IndexingRequest>> chunks = Iterables.partition(messageList.subList(offset, messageList.size()), chunkSize);
        int chunkCount = 1;
        int indexedSuccessfully = 0;
        final List<Messages.IndexingError> indexFailures = new ArrayList<>();
        for (List<IndexingRequest> chunk : chunks) {

            final BulkResponse result = runBulkRequest(indexedSuccessfully, chunk);

            indexedSuccessfully += chunk.size();

            final List<BulkItemResponse> failures = extractFailures(result);

            indexFailures.addAll(indexingErrorsFrom(failures, messageList));

            logDebugInfo(messageList, offset, chunkSize, chunkCount, result, failures);

            logFailures(result, failures.size());

            chunkCount++;
        }

        return indexFailures;
    }

    private List<BulkItemResponse> extractFailures(BulkResponse result) {
        return Arrays.stream(result.getItems())
                        .filter(BulkItemResponse::isFailed)
                        .collect(Collectors.toList());
    }

    private void logFailures(BulkResponse result, int failureCount) {
        if (failureCount > 0) {
            LOG.error("Failed to index [{}] messages. Please check the index error log in your web interface for the reason. Error: {}",
                    failureCount, result.buildFailureMessage());
        }
    }

    private void logDebugInfo(List<IndexingRequest> messageList, int offset, int chunkSize, int chunkCount, BulkResponse result, List<BulkItemResponse> failures) {
        if (LOG.isDebugEnabled()) {
            String chunkInfo = "";
            if (chunkSize != messageList.size()) {
                chunkInfo = String.format(Locale.ROOT, " (chunk %d/%d offset %d)", chunkCount,
                        (int) Math.ceil((double) messageList.size() / chunkSize), offset);
            }
            LOG.debug("Index: Bulk indexed {} messages{}, failures: {}",
                    result.getItems().length, chunkInfo, failures.size());
        }
    }

    private BulkResponse runBulkRequest(int indexedSuccessfully, List<IndexingRequest> chunk) throws ChunkedBulkIndexer.EntityTooLargeException {
        final BulkRequest bulkRequest = createBulkRequest(chunk);

        final BulkResponse result;
        try {
            result = this.client.execute((c, requestOptions) -> c.bulk(bulkRequest, requestOptions));
        } catch (OpenSearchException e) {
            for (OpenSearchException cause : e.guessRootCauses()) {
                if (cause.status().equals(RestStatus.REQUEST_ENTITY_TOO_LARGE)) {
                    throw new ChunkedBulkIndexer.EntityTooLargeException(indexedSuccessfully);
                } else if (cause.status().equals(RestStatus.TOO_MANY_REQUESTS)) {
                    throw new ChunkedBulkIndexer.TooManyRequestsException(indexedSuccessfully);
                }
            }
            throw new org.graylog2.indexer.ElasticsearchException(e);
        }
        return result;
    }

    private BulkRequest createBulkRequest(List<IndexingRequest> chunk) {
        final BulkRequest bulkRequest = new BulkRequest();
        chunk.forEach(request -> bulkRequest.add(
                indexRequestFrom(request)
        ));
        return bulkRequest;
    }

    private List<Messages.IndexingError> indexingErrorsFrom(List<IndexingRequest> messageList) {
        return messageList.stream()
                .map(this::indexingErrorFrom)
                .collect(Collectors.toList());
    }

    private Messages.IndexingError indexingErrorFrom(IndexingRequest indexingRequest) {
        return Messages.IndexingError.create(indexingRequest.message(), indexingRequest.indexSet().getWriteIndexAlias());
    }

    private List<Messages.IndexingError> indexingErrorsFrom(List<BulkItemResponse> failedItems, List<IndexingRequest> messageList) {
        if (failedItems.isEmpty()) {
            return Collections.emptyList();
        }

        final Map<String, Indexable> messageMap = messageList.stream()
                .map(IndexingRequest::message)
                .distinct()
                .collect(Collectors.toMap(Indexable::getId, Function.identity()));

        return failedItems.stream()
                .map(item -> {
                    final Indexable message = messageMap.get(item.getId());

                    return indexingErrorFromResponse(item, message);
                })
                .collect(Collectors.toList());
    }

    private Messages.IndexingError indexingErrorFromResponse(BulkItemResponse item, Indexable message) {
        return Messages.IndexingError.create(message, item.getIndex(), errorTypeFromResponse(item), item.getFailureMessage());
    }

    private Messages.IndexingError.ErrorType errorTypeFromResponse(BulkItemResponse item) {
        final ParsedOpenSearchException exception = ParsedOpenSearchException.from(item.getFailureMessage());
        switch (exception.type()) {
            case MAPPER_PARSING_EXCEPTION: return Messages.IndexingError.ErrorType.MappingError;
            case INDEX_BLOCK_ERROR:
                if (exception.reason().contains(INDEX_BLOCK_REASON) || exception.reason().contains(FLOOD_STAGE_WATERMARK)) {
                    return Messages.IndexingError.ErrorType.IndexBlocked;
                }
            case UNAVAILABLE_SHARDS_EXCEPTION:
                if (exception.reason().contains(PRIMARY_SHARD_NOT_ACTIVE_REASON)) {
                    return Messages.IndexingError.ErrorType.IndexBlocked;
                }
            case ILLEGAL_ARGUMENT_EXCEPTION:
                if (exception.reason().contains(NO_WRITE_INDEX_DEFINED_FOR_ALIAS)) {
                    return Messages.IndexingError.ErrorType.IndexBlocked;
                }
            default: return Messages.IndexingError.ErrorType.Unknown;
        }
    }

    private IndexRequest indexRequestFrom(IndexingRequest request) {
        final byte[] body;
        try {
            body = this.objectMapper.writeValueAsBytes(request.message().toElasticSearchObject(objectMapper, this.invalidTimestampMeter));
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
        return new IndexRequest(request.indexSet().getWriteIndexAlias())
                .id(request.message().getId())
                .source(body, XContentType.JSON);
    }
}
