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
package org.graylog2.indexer.messages;

import com.github.rholder.retry.Attempt;
import com.github.rholder.retry.RetryException;
import com.github.rholder.retry.RetryListener;
import com.github.rholder.retry.Retryer;
import com.github.rholder.retry.RetryerBuilder;
import com.github.rholder.retry.WaitStrategies;
import com.google.common.collect.Sets;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import org.graylog.failure.FailureSubmissionService;
import org.graylog2.indexer.InvalidWriteTargetException;
import org.graylog2.indexer.MasterNotDiscoveredException;
import org.graylog2.indexer.results.ResultMessage;
import org.graylog2.shared.utilities.ExceptionUtils;
import org.graylog2.system.processing.ProcessingStatusRecorder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nullable;
import java.io.IOException;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

import static org.graylog2.indexer.messages.RetryWait.MAX_WAIT_TIME;

@Singleton
public class Messages {
    public interface IndexingListener {
        void onRetry(long attemptNumber);

        void onSuccess(long delaySinceFirstAttempt);
    }

    private static final Logger LOG = LoggerFactory.getLogger(Messages.class);


    // the wait strategy uses powers of 2 to compute wait times.
    // see https://github.com/rholder/guava-retrying/blob/177b6c9b9f3e7957f404f0bdb8e23374cb1de43f/src/main/java/com/github/rholder/retry/WaitStrategies.java#L304
    // using 500 leads to the expected exponential pattern of 1000, 2000, 4000, 8000, ...
    private static final int retrySecondsMultiplier = 500;

    static final RetryWait retryWait = new RetryWait(retrySecondsMultiplier);

    @SuppressWarnings("UnstableApiUsage")
    private RetryerBuilder<IndexingResults> createBulkRequestRetryerBuilder() {
        return RetryerBuilder.<IndexingResults>newBuilder()
                .retryIfException(t -> ExceptionUtils.hasCauseOf(t, IOException.class)
                        || t instanceof InvalidWriteTargetException
                        || t instanceof MasterNotDiscoveredException)
                .withWaitStrategy(WaitStrategies.exponentialWait(MAX_WAIT_TIME.getQuantity(), MAX_WAIT_TIME.getUnit()))
                .withRetryListener(new RetryListener() {
                    @Override
                    public <V> void onRetry(Attempt<V> attempt) {
                        if (attempt.hasException()) {
                            LOG.warn("Caught exception during bulk indexing: {}, retrying (attempt #{}).", attempt.getExceptionCause(), attempt.getAttemptNumber());
                        } else if (attempt.getAttemptNumber() > 1) {
                            LOG.info("Bulk indexing finally successful (attempt #{}).", attempt.getAttemptNumber());
                        }
                    }
                });
    }

    private final FailureSubmissionService failureSubmissionService;
    private final MessagesAdapter messagesAdapter;
    private final ProcessingStatusRecorder processingStatusRecorder;
    private final TrafficAccounting trafficAccounting;

    @Inject
    public Messages(TrafficAccounting trafficAccounting,
                    MessagesAdapter messagesAdapter,
                    ProcessingStatusRecorder processingStatusRecorder,
                    FailureSubmissionService failureSubmissionService) {
        this.trafficAccounting = trafficAccounting;
        this.messagesAdapter = messagesAdapter;
        this.processingStatusRecorder = processingStatusRecorder;
        this.failureSubmissionService = failureSubmissionService;
    }

    public ResultMessage get(String messageId, String index) throws DocumentNotFoundException, IOException {
        return messagesAdapter.get(messageId, index);
    }

    public List<String> analyze(String toAnalyze, String index, String analyzer) throws IOException {
        return messagesAdapter.analyze(toAnalyze, index, analyzer);
    }

    public IndexingResults bulkIndex(final List<MessageWithIndex> messageList) {
        return bulkIndex(messageList, false, null);
    }

    public IndexingResults bulkIndex(final List<MessageWithIndex> messageList, IndexingListener indexingListener) {
        return bulkIndex(messageList, false, indexingListener);
    }

    public IndexingResults bulkIndex(final List<MessageWithIndex> messageList, boolean isSystemTraffic) {
        return bulkIndex(messageList, isSystemTraffic, null);
    }

    public IndexingResults bulkIndex(final List<MessageWithIndex> messageList, boolean isSystemTraffic, IndexingListener indexingListener) {
        if (messageList.isEmpty()) {
            return IndexingResults.empty();
        }

        final List<IndexingRequest> indexingRequestList = messageList.stream()
                .map(entry -> IndexingRequest.create(entry.indexSet(), entry.message()))
                .collect(Collectors.toList());

        return bulkIndexRequests(indexingRequestList, isSystemTraffic, indexingListener);
    }

    public IndexingResults bulkIndexRequests(List<IndexingRequest> indexingRequestList, boolean isSystemTraffic) {
        return bulkIndexRequests(indexingRequestList, isSystemTraffic, null);
    }

    public IndexingResults bulkIndexRequests(List<IndexingRequest> indexingRequestList, boolean isSystemTraffic, IndexingListener indexingListener) {
        final IndexingResults indexingResults = runBulkRequest(indexingRequestList, indexingRequestList.size(), indexingListener);

        final IndexingResults retryBlockResults = retryQualifyingIndividualItems(indexingRequestList, indexingResults.errors(), indexingListener);

        final IndexingResults finalResults = retryBlockResults.mergeWith(indexingResults.successes(), List.of());

        recordTimestamp(finalResults.successes());
        accountTotalMessageSizes(finalResults.successes(), isSystemTraffic);

        if (!finalResults.errors().isEmpty()) {
            failureSubmissionService.submitIndexingErrors(finalResults.errors());
        }

        return finalResults;
    }

    private IndexingResults retryQualifyingIndividualItems(List<IndexingRequest> messages, List<IndexingError> allFailedItems, IndexingListener indexingListener) {
        Set<IndexingError> retryableErrors = retryableErrorsFrom(allFailedItems);
        final Set<IndexingError> otherFailures = new HashSet<>(Sets.difference(new HashSet<>(allFailedItems), retryableErrors));
        List<IndexingRequest> blockedMessages = messagesForResultItems(messages, retryableErrors);

        if (!retryableErrors.isEmpty()) {
            LOG.warn("Retrying {} messages, because their indices are blocked with status [read-only / allow delete]", retryableErrors.size());
        }

        long attempt = 1;

        final IndexingResults.Builder builder = IndexingResults.Builder.create();
        while (!retryableErrors.isEmpty()) {
            retryWait.waitBeforeRetrying(attempt++);

            final IndexingResults indexingResults = runBulkRequest(blockedMessages, messages.size(), indexingListener);

            builder.addSuccesses(indexingResults.successes());
            final var failedItems = indexingResults.errors();
            retryableErrors = retryableErrorsFrom(failedItems);
            blockedMessages = messagesForResultItems(blockedMessages, retryableErrors);

            final Set<IndexingError> newOtherFailures = Sets.difference(new HashSet<>(failedItems), retryableErrors);
            otherFailures.addAll(newOtherFailures);

            if (retryableErrors.isEmpty()) {
                LOG.info("Retries were successful after {} attempts. Ingestion will continue now.", attempt);
            }
        }

        builder.addErrors(otherFailures.stream().toList());
        return builder.build();
    }

    private List<IndexingRequest> messagesForResultItems(List<IndexingRequest> chunk, Set<IndexingError> indexBlocks) {
        final Set<String> blockedMessageIds = indexBlocks.stream().map(item -> item.message().getId()).collect(Collectors.toSet());

        return chunk.stream().filter(entry -> blockedMessageIds.contains(entry.message().getId())).collect(Collectors.toList());
    }

    private Set<IndexingError> retryableErrorsFrom(List<IndexingError> allFailedItems) {
        return allFailedItems.stream().filter(this::isRetryable).collect(Collectors.toSet());
    }

    private boolean isRetryable(IndexingError indexingError) {
        final var errorType = indexingError.error().type();
        return errorType.equals(IndexingError.Type.IndexBlocked) || errorType.equals(IndexingError.Type.DataTooLarge);
    }

    @SuppressWarnings("UnstableApiUsage")
    private IndexingResults runBulkRequest(List<IndexingRequest> indexingRequestList, int count, @Nullable IndexingListener indexingListener) {
        final Retryer<IndexingResults> bulkRequestRetryer = indexingListener == null
                ? createBulkRequestRetryerBuilder().build()
                : createBulkRequestRetryerBuilder().withRetryListener(retryListenerFor(indexingListener)).build();

        try {
            return bulkRequestRetryer.call(() -> messagesAdapter.bulkIndex(indexingRequestList));
        } catch (ExecutionException | RetryException e) {
            if (e instanceof RetryException) {
                LOG.error("Could not bulk index {} messages. Giving up after {} attempts.", count, ((RetryException) e).getNumberOfFailedAttempts());
            } else {
                LOG.error("Couldn't bulk index " + count + " messages.", e);
            }
            throw new RuntimeException(e);
        }
    }

    @SuppressWarnings("UnstableApiUsage")
    private RetryListener retryListenerFor(IndexingListener indexingListener) {
        return new RetryListener() {
            @Override
            public <V> void onRetry(Attempt<V> attempt) {
                if (attempt.hasException()) {
                    indexingListener.onRetry(attempt.getAttemptNumber());
                } else {
                    indexingListener.onSuccess(attempt.getDelaySinceFirstAttempt());
                }
            }
        };
    }

    private void accountTotalMessageSizes(List<IndexingSuccess> requests, boolean isSystemTraffic) {
        final long totalSizeOfIndexedMessages = requests.stream()
                .map(IndexingSuccess::message)
                .mapToLong(Indexable::getSize)
                .sum();

        if (isSystemTraffic) {
            trafficAccounting.addSystemTraffic(totalSizeOfIndexedMessages);
        } else {
            trafficAccounting.addOutputTraffic(totalSizeOfIndexedMessages);
        }
    }

    private void recordTimestamp(List<IndexingSuccess> messageList) {
        for (final IndexingSuccess entry : messageList) {
            final Indexable message = entry.message();

            processingStatusRecorder.updatePostIndexingReceiveTime(message.getReceiveTime());
        }
    }
}
