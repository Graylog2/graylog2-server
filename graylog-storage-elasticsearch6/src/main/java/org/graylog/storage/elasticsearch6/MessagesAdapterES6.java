package org.graylog.storage.elasticsearch6;

import com.codahale.metrics.Meter;
import com.codahale.metrics.MetricRegistry;
import com.github.joschi.jadconfig.util.Duration;
import com.github.rholder.retry.Attempt;
import com.github.rholder.retry.RetryException;
import com.github.rholder.retry.RetryListener;
import com.github.rholder.retry.Retryer;
import com.github.rholder.retry.RetryerBuilder;
import com.github.rholder.retry.WaitStrategies;
import com.github.rholder.retry.WaitStrategy;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Iterables;
import com.google.common.collect.Sets;
import io.searchbox.client.JestClient;
import io.searchbox.client.JestResult;
import io.searchbox.core.Bulk;
import io.searchbox.core.BulkResult;
import io.searchbox.core.DocumentResult;
import io.searchbox.core.Get;
import io.searchbox.core.Index;
import io.searchbox.indices.Analyze;
import org.apache.http.client.config.RequestConfig;
import org.graylog2.indexer.ElasticsearchException;
import org.graylog2.indexer.IndexFailure;
import org.graylog2.indexer.IndexFailureImpl;
import org.graylog2.indexer.IndexMapping;
import org.graylog2.indexer.cluster.jest.JestUtils;
import org.graylog2.indexer.messages.DocumentNotFoundException;
import org.graylog2.indexer.messages.IndexBlockRetryAttempt;
import org.graylog2.indexer.messages.IndexingRequest;
import org.graylog2.indexer.messages.Messages;
import org.graylog2.indexer.messages.MessagesAdapter;
import org.graylog2.indexer.results.ResultMessage;
import org.graylog2.plugin.Message;
import org.graylog2.system.processing.ProcessingStatusRecorder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.inject.Named;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.function.Function;
import java.util.stream.Collectors;

import static com.codahale.metrics.MetricRegistry.name;

public class MessagesAdapterES6 implements MessagesAdapter {
    private static final Duration MAX_WAIT_TIME = Duration.seconds(30L);
    private static final Logger LOG = LoggerFactory.getLogger(MessagesAdapterES6.class);

    @VisibleForTesting
    static final WaitStrategy exponentialWaitMilliseconds = WaitStrategies.exponentialWait(MAX_WAIT_TIME.getQuantity(), MAX_WAIT_TIME.getUnit());

    // the wait strategy uses powers of 2 to compute wait times.
    // see https://github.com/rholder/guava-retrying/blob/177b6c9b9f3e7957f404f0bdb8e23374cb1de43f/src/main/java/com/github/rholder/retry/WaitStrategies.java#L304
    // using 500 leads to the expected exponential pattern of 1000, 2000, 4000, 8000, ...
    private static final int retrySecondsMultiplier = 500;

    @VisibleForTesting
    static final WaitStrategy exponentialWaitSeconds = WaitStrategies.exponentialWait(retrySecondsMultiplier, MAX_WAIT_TIME.getQuantity(), MAX_WAIT_TIME.getUnit());

    static final String INDEX_BLOCK_ERROR = "cluster_block_exception";
    static final String INDEX_BLOCK_REASON = "blocked by: [FORBIDDEN/12/index read-only / allow delete (api)];";

    private final JestClient client;
    private final boolean useExpectContinue;

    private static final Retryer<BulkResult> BULK_REQUEST_RETRYER = RetryerBuilder.<BulkResult>newBuilder()
            .retryIfException(t -> t instanceof IOException)
            .withWaitStrategy(WaitStrategies.exponentialWait(MAX_WAIT_TIME.getQuantity(), MAX_WAIT_TIME.getUnit()))
            .withRetryListener(new RetryListener() {
                @Override
                public <V> void onRetry(Attempt<V> attempt) {
                    if (attempt.hasException()) {
                        LOG.error("Caught exception during bulk indexing: {}, retrying (attempt #{}).", attempt.getExceptionCause(), attempt.getAttemptNumber());
                    } else if (attempt.getAttemptNumber() > 1) {
                        LOG.info("Bulk indexing finally successful (attempt #{}).", attempt.getAttemptNumber());
                    }
                }
            })
            .build();

    private final Meter invalidTimestampMeter;
    private final ProcessingStatusRecorder processingStatusRecorder;

    @Inject
    public MessagesAdapterES6(JestClient client,
                              @Named("elasticsearch_use_expect_continue") boolean useExpectContinue,
                              MetricRegistry metricRegistry, ProcessingStatusRecorder processingStatusRecorder) {
        this.client = client;
        this.useExpectContinue = useExpectContinue;
        invalidTimestampMeter = metricRegistry.meter(name(Messages.class, "invalid-timestamps"));
        this.processingStatusRecorder = processingStatusRecorder;
    }

    @Override
    public ResultMessage get(String messageId, String index) throws IOException, DocumentNotFoundException {
        final Get get = new Get.Builder(index, messageId).type(IndexMapping.TYPE_MESSAGE).build();
        final DocumentResult result = client.execute(get);

        if (!result.isSucceeded()) {
            throw new DocumentNotFoundException(index, messageId);
        }

        @SuppressWarnings("unchecked")
        final Map<String, Object> message = (Map<String, Object>) result.getSourceAsObject(Map.class, false);

        return ResultMessage.parseFromSource(result.getId(), result.getIndex(), message);
    }

    @Override
    public List<String> analyze(String toAnalyze, String index, String analyzer) throws IOException {
        final Analyze analyze = new Analyze.Builder().index(index).analyzer(analyzer).text(toAnalyze).build();
        final JestResult result = client.execute(analyze);

        @SuppressWarnings("unchecked")
        final List<Map<String, Object>> tokens = (List<Map<String, Object>>) result.getValue("tokens");
        final List<String> terms = new ArrayList<>(tokens.size());
        tokens.forEach(token -> terms.add((String)token.get("token")));

        return terms;
    }

    @Override
    public List<IndexFailure> bulkIndex(List<IndexingRequest> messageList) {
        if (messageList.isEmpty()) {
            return Collections.emptyList();
        }

        int chunkSize = messageList.size();
        int offset = 0;
        final List<BulkResult.BulkResultItem> failedItems = new ArrayList<>();
        for (;;) {
            try {
                final List<BulkResult.BulkResultItem> failures = bulkIndexChunked(messageList, offset, chunkSize);
                failedItems.addAll(failures);
                break; // on success
            } catch (EntityTooLargeException e) {
                LOG.warn("Bulk index failed with 'Request Entity Too Large' error. Retrying by splitting up batch size <{}>.", chunkSize);
                if (chunkSize == messageList.size()) {
                    LOG.warn("Consider lowering the \"output_batch_size\" setting.");
                }
                failedItems.addAll(e.failedItems);
                offset += e.indexedSuccessfully;
                chunkSize /= 2;
            }
            if (chunkSize == 0) {
                throw new ElasticsearchException("Bulk index cannot split output batch any further.");
            }
        }

        return indexFailuresFromMessages(failedItems, messageList);
    }

    private List<IndexFailure> indexFailuresFromMessages(List<BulkResult.BulkResultItem> failedItems, List<IndexingRequest> messageList) {
        if (failedItems.isEmpty()) {
            return Collections.emptyList();
        }

        final Map<String, Message> messageMap = messageList.stream()
                .map(IndexingRequest::message)
                .distinct()
                .collect(Collectors.toMap(Message::getId, Function.identity()));
        final List<IndexFailure> indexFailures = new ArrayList<>(failedItems.size());
        for (BulkResult.BulkResultItem item : failedItems) {
            LOG.warn("Failed to index message: index=<{}> id=<{}> error=<{}>", item.index, item.id, item.error);

            // Write failure to index_failures.
            final Message messageEntry = messageMap.get(item.id);

            final IndexFailure indexFailure = indexFailureFromResultItem(item, messageEntry);

            indexFailures.add(indexFailure);
        }

        return indexFailures;
    }

    private IndexFailure indexFailureFromResultItem(BulkResult.BulkResultItem item, Message messageEntry) {
        final Map<String, Object> doc = ImmutableMap.<String, Object>builder()
                .put("letter_id", item.id)
                .put("index", item.index)
                .put("type", item.type)
                .put("message", item.error)
                .put("timestamp", messageEntry.getTimestamp())
                .build();

        return new IndexFailureImpl(doc);
    }

    private BulkResult runBulkRequest(final Bulk request, int count) {
        try {
            if (useExpectContinue) {
                // Enable Expect-Continue to catch 413 errors before we send the actual data
                final RequestConfig requestConfig = RequestConfig.custom().setExpectContinueEnabled(true).build();
                return BULK_REQUEST_RETRYER.call(() -> JestUtils.execute(client, requestConfig, request));
            } else {
                return BULK_REQUEST_RETRYER.call(() -> client.execute(request));
            }
        } catch (ExecutionException | RetryException e) {
            if (e instanceof RetryException) {
                LOG.error("Could not bulk index {} messages. Giving up after {} attempts.", count, ((RetryException) e).getNumberOfFailedAttempts());
            } else {
                LOG.error("Couldn't bulk index " + count + " messages.", e);
            }
            throw new RuntimeException(e);
        }
    }


    private List<BulkResult.BulkResultItem> bulkIndexChunked(final List<IndexingRequest> messageList, int offset, int chunkSize) throws EntityTooLargeException {
        chunkSize = Math.min(messageList.size(), chunkSize);

        final List<BulkResult.BulkResultItem> failedItems = new ArrayList<>();
        final Iterable<List<IndexingRequest>> chunks = Iterables.partition(messageList.subList(offset, messageList.size()), chunkSize);
        int chunkCount = 1;
        int indexedSuccessfully = 0;
        for (List<IndexingRequest> chunk : chunks) {
            final BulkResult result = bulkIndexChunk(chunk);

            if (result.getResponseCode() == 413) {
                throw new EntityTooLargeException(indexedSuccessfully, failedItems);
            }

            // TODO should we check result.isSucceeded()?

            indexedSuccessfully += chunk.size();

            final Set<BulkResult.BulkResultItem> remainingFailures = retryOnlyIndexBlockItemsForever(chunk, result.getFailedItems());

            failedItems.addAll(remainingFailures);
            if (LOG.isDebugEnabled()) {
                String chunkInfo = "";
                if (chunkSize != messageList.size()) {
                    chunkInfo = String.format(Locale.ROOT, " (chunk %d/%d offset %d)", chunkCount,
                            (int) Math.ceil((double) messageList.size() / chunkSize), offset);
                }
                LOG.debug("Index: Bulk indexed {} messages{}, failures: {}",
                        result.getItems().size(), chunkInfo, failedItems.size());
            }
            if (!remainingFailures.isEmpty()) {
                LOG.error("Failed to index [{}] messages. Please check the index error log in your web interface for the reason. Error: {}",
                        remainingFailures.size(), result.getErrorMessage());
            }
            chunkCount++;
        }
        return failedItems;
    }

    private BulkResult bulkIndexChunk(List<IndexingRequest> chunk) {
        final Bulk.Builder bulk = new Bulk.Builder();

        for (IndexingRequest entry : chunk) {
            final Message message = entry.message();

            bulk.addAction(new Index.Builder(message.toElasticSearchObject(invalidTimestampMeter))
                    .index(entry.indexSet().getWriteIndexAlias())
                    .type(IndexMapping.TYPE_MESSAGE)
                    .id(message.getId())
                    .build());
        }

        return runBulkRequest(bulk.build(), chunk.size());
    }

    private Set<BulkResult.BulkResultItem> retryOnlyIndexBlockItemsForever(List<IndexingRequest> chunk, List<BulkResult.BulkResultItem> allFailedItems) {
        Set<BulkResult.BulkResultItem> indexBlocks = indexBlocksFrom(allFailedItems);
        final Set<BulkResult.BulkResultItem> otherFailures = new HashSet<>(Sets.difference(new HashSet<>(allFailedItems), indexBlocks));
        List<IndexingRequest> blockedMessages = messagesForResultItems(chunk, indexBlocks);

        if (!indexBlocks.isEmpty()) {
            LOG.warn("Retrying {} messages, because their indices are blocked with status [read-only / allow delete]", indexBlocks.size());
        }

        long attempt = 1;

        while (!indexBlocks.isEmpty()) {
            waitBeforeRetrying(attempt++);

            final BulkResult bulkResult = bulkIndexChunk(blockedMessages);

            final List<BulkResult.BulkResultItem> failedItems = bulkResult.getFailedItems();

            indexBlocks = indexBlocksFrom(failedItems);
            blockedMessages = messagesForResultItems(blockedMessages, indexBlocks);

            final Set<BulkResult.BulkResultItem> newOtherFailures = Sets.difference(new HashSet<>(failedItems), indexBlocks);
            otherFailures.addAll(newOtherFailures);

            if (indexBlocks.isEmpty()) {
                LOG.info("Retries were successful after {} attempts. Ingestion will continue now.", attempt);
            }
        }

        return otherFailures;
    }

    private void waitBeforeRetrying(long attempt) {
        try {
            final long sleepTime = exponentialWaitSeconds.computeSleepTime(new IndexBlockRetryAttempt(attempt));
            Thread.sleep(sleepTime);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    private List<IndexingRequest> messagesForResultItems(List<IndexingRequest> chunk, Set<BulkResult.BulkResultItem> indexBlocks) {
        final Set<String> blockedMessageIds = indexBlocks.stream().map(item -> item.id).collect(Collectors.toSet());

        return chunk.stream().filter(entry -> blockedMessageIds.contains(entry.message().getId())).collect(Collectors.toList());
    }

    private Set<BulkResult.BulkResultItem> indexBlocksFrom(List<BulkResult.BulkResultItem> allFailedItems) {
        return allFailedItems.stream().filter(this::hasFailedDueToBlockedIndex).collect(Collectors.toSet());
    }

    private boolean hasFailedDueToBlockedIndex(BulkResult.BulkResultItem item) {
        return item.errorType.equals(INDEX_BLOCK_ERROR) && item.errorReason.equals(INDEX_BLOCK_REASON);
    }

    private void recordTimestamp(List<IndexingRequest> messageList, Set<String> failedIds) {
        for (final IndexingRequest entry : messageList) {
            final Message message = entry.message();

            if (failedIds.contains(message.getId())) {
                continue;
            }

            processingStatusRecorder.updatePostIndexingReceiveTime(message.getReceiveTime());
        }
    }

    private static class EntityTooLargeException extends Exception {
        private final int indexedSuccessfully;
        private final List<BulkResult.BulkResultItem> failedItems;

        public EntityTooLargeException(int indexedSuccessfully, List<BulkResult.BulkResultItem> failedItems)  {
            this.indexedSuccessfully = indexedSuccessfully;
            this.failedItems = failedItems;
        }
    }
}
