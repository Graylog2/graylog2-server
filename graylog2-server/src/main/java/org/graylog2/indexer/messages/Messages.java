/**
 * This file is part of Graylog.
 *
 * Graylog is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Graylog is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Graylog.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.graylog2.indexer.messages;

import com.codahale.metrics.Counter;
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
import org.graylog2.indexer.IndexSet;
import org.graylog2.indexer.cluster.jest.JestUtils;
import org.graylog2.indexer.results.ResultMessage;
import org.graylog2.plugin.GlobalMetricNames;
import org.graylog2.plugin.Message;
import org.graylog2.system.processing.ProcessingStatusRecorder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.stream.Collectors;

import static com.codahale.metrics.MetricRegistry.name;

@Singleton
public class Messages {
    private static final Logger LOG = LoggerFactory.getLogger(Messages.class);

    private static final Duration MAX_WAIT_TIME = Duration.seconds(30L);

    @VisibleForTesting
    static final WaitStrategy exponentialWaitMilliseconds = WaitStrategies.exponentialWait(MAX_WAIT_TIME.getQuantity(), MAX_WAIT_TIME.getUnit());

    // the wait strategy uses powers of 2 to compute wait times.
    // see https://github.com/rholder/guava-retrying/blob/177b6c9b9f3e7957f404f0bdb8e23374cb1de43f/src/main/java/com/github/rholder/retry/WaitStrategies.java#L304
    // using 500 leads to the expected exponential pattern of 1000, 2000, 4000, 8000, ...
    private static final int retrySecondsMultiplier = 500;

    @VisibleForTesting
    static final WaitStrategy exponentialWaitSeconds = WaitStrategies.exponentialWait(retrySecondsMultiplier, MAX_WAIT_TIME.getQuantity(), MAX_WAIT_TIME.getUnit());

    private static final Retryer<BulkResult> BULK_REQUEST_RETRYER = RetryerBuilder.<BulkResult>newBuilder()
            .retryIfException(t -> t instanceof IOException)
            .retryIfResult(Messages::requestFailedForRetryableReason)
            .withWaitStrategy(exponentialWaitMilliseconds)
            .withRetryListener(new RetryListener() {
                @Override
                public <V> void onRetry(Attempt<V> attempt) {
                    if (attempt.hasException()) {
                        LOG.error("Caught exception during bulk indexing: {}, retrying (attempt #{}).", attempt.getExceptionCause(), attempt.getAttemptNumber());
                    } else if (attempt.getResult() instanceof BulkResult && requestFailedForRetryableReason((BulkResult)attempt.getResult())) {
                        final BulkResult bulkResult = (BulkResult) attempt.getResult();
                        LOG.error("Bulk indexing failed: {}, retrying (attempt #{})", reasonOfFailure(bulkResult), attempt.getAttemptNumber());
                    } else if (attempt.getAttemptNumber() > 1) {
                        LOG.info("Bulk indexing finally successful (attempt #{}).", attempt.getAttemptNumber());
                    }
                }
            })
            .build();

    private static boolean requestFailedForRetryableReason(BulkResult result) {
        return isAliasWithInvalidTargets(result);
    }

    private static boolean isAliasWithInvalidTargets(BulkResult result) {
        return result.getResponseCode() == 400 && reasonOfFailure(result).startsWith("no write index is defined for alias");
    }

    private static String reasonOfFailure(BulkResult result) {
        return result.getJsonObject().path("error").path("reason").asText();
    }

    static final String INDEX_BLOCK_ERROR = "cluster_block_exception";
    static final String INDEX_BLOCK_REASON = "blocked by: [FORBIDDEN/12/index read-only / allow delete (api)];";

    private final Meter invalidTimestampMeter;
    private final JestClient client;
    private final ProcessingStatusRecorder processingStatusRecorder;
    private final boolean useExpectContinue;
    private final LinkedBlockingQueue<List<IndexFailure>> indexFailureQueue;
    private final Counter outputByteCounter;
    private final Counter systemTrafficCounter;

    @Inject
    public Messages(MetricRegistry metricRegistry,
                    JestClient client,
                    ProcessingStatusRecorder processingStatusRecorder,
                    @Named("elasticsearch_use_expect_continue") boolean useExpectContinue) {
        invalidTimestampMeter = metricRegistry.meter(name(Messages.class, "invalid-timestamps"));
        outputByteCounter = metricRegistry.counter(GlobalMetricNames.OUTPUT_TRAFFIC);
        systemTrafficCounter = metricRegistry.counter(GlobalMetricNames.SYSTEM_OUTPUT_TRAFFIC);
        this.client = client;
        this.processingStatusRecorder = processingStatusRecorder;
        this.useExpectContinue = useExpectContinue;

        // TODO: Magic number
        this.indexFailureQueue = new LinkedBlockingQueue<>(1000);
    }

    public ResultMessage get(String messageId, String index) throws DocumentNotFoundException, IOException {
        final Get get = new Get.Builder(index, messageId).type(IndexMapping.TYPE_MESSAGE).build();
        final DocumentResult result = client.execute(get);

        if (!result.isSucceeded()) {
            throw new DocumentNotFoundException(index, messageId);
        }

        @SuppressWarnings("unchecked") final Map<String, Object> message = (Map<String, Object>) result.getSourceAsObject(Map.class, false);

        return ResultMessage.parseFromSource(result.getId(), result.getIndex(), message);
    }

    public List<String> analyze(String toAnalyze, String index, String analyzer) throws IOException {
        final Analyze analyze = new Analyze.Builder().index(index).analyzer(analyzer).text(toAnalyze).build();
        final JestResult result = client.execute(analyze);

        @SuppressWarnings("unchecked") final List<Map<String, Object>> tokens = (List<Map<String, Object>>) result.getValue("tokens");
        final List<String> terms = new ArrayList<>(tokens.size());
        tokens.forEach(token -> terms.add((String) token.get("token")));

        return terms;
    }

    public List<String> bulkIndex(final List<Map.Entry<IndexSet, Message>> messageList) {
        return bulkIndex(messageList, false);
    }

    public List<String> bulkIndex(final List<Map.Entry<IndexSet, Message>> messageList, boolean isSystemTraffic) {
        if (messageList.isEmpty()) {
            return Collections.emptyList();
        }

        int chunkSize = messageList.size();
        int offset = 0;
        List<BulkResult.BulkResultItem> failedItems = new ArrayList<>();
        for (;;) {
            try {
                List<BulkResult.BulkResultItem> failures = bulkIndexChunked(messageList, isSystemTraffic, offset, chunkSize);
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

        if (!failedItems.isEmpty()) {
            final Set<String> failedIds = failedItems.stream().map(item -> item.id).collect(Collectors.toSet());
            recordTimestamp(messageList, failedIds);
            return propagateFailure(failedItems, messageList);
        } else {
            recordTimestamp(messageList, Collections.emptySet());
            return Collections.emptyList();
        }
    }

    private List<BulkResult.BulkResultItem> bulkIndexChunked(final List<Map.Entry<IndexSet, Message>> messageList, boolean isSystemTraffic, int offset, int chunkSize) throws EntityTooLargeException {
        chunkSize = Math.min(messageList.size(), chunkSize);

        final List<BulkResult.BulkResultItem> failedItems = new ArrayList<>();
        final Iterable<List<Map.Entry<IndexSet, Message>>> chunks = Iterables.partition(messageList.subList(offset, messageList.size()), chunkSize);
        int chunkCount = 1;
        int indexedSuccessfully = 0;
        for (List<Map.Entry<IndexSet, Message>> chunk : chunks) {

            long messageSizes = chunk.stream().mapToLong(m -> m.getValue().getSize()).sum();

            final BulkResult result = bulkIndexChunk(chunk);

            if (result.getResponseCode() == 413) {
                throw new EntityTooLargeException(indexedSuccessfully, failedItems);
            }

            // TODO should we check result.isSucceeded()?

            indexedSuccessfully += chunk.size();

            Set<BulkResult.BulkResultItem> remainingFailures = retryOnlyIndexBlockItemsForever(chunk, result.getFailedItems());

            failedItems.addAll(remainingFailures);
            if (isSystemTraffic) {
                systemTrafficCounter.inc(messageSizes);
            } else {
                outputByteCounter.inc(messageSizes);
            }
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

    private BulkResult bulkIndexChunk(List<Map.Entry<IndexSet, Message>> chunk) {
        Bulk.Builder bulk = new Bulk.Builder();

        for (Map.Entry<IndexSet, Message> entry : chunk) {
            final Message message = entry.getValue();

            bulk.addAction(new Index.Builder(message.toElasticSearchObject(invalidTimestampMeter))
                    .index(entry.getKey().getWriteIndexAlias())
                    .type(IndexMapping.TYPE_MESSAGE)
                    .id(message.getId())
                    .build());
        }

        return runBulkRequest(bulk.build(), chunk.size());
    }

    private Set<BulkResult.BulkResultItem> retryOnlyIndexBlockItemsForever(List<Map.Entry<IndexSet, Message>> chunk, List<BulkResult.BulkResultItem> allFailedItems) {
        Set<BulkResult.BulkResultItem> indexBlocks = indexBlocksFrom(allFailedItems);
        final Set<BulkResult.BulkResultItem> otherFailures = new HashSet<>(Sets.difference(new HashSet<>(allFailedItems), indexBlocks));
        List<Map.Entry<IndexSet, Message>> blockedMessages = messagesForResultItems(chunk, indexBlocks);

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

    private List<Map.Entry<IndexSet, Message>> messagesForResultItems(List<Map.Entry<IndexSet, Message>> chunk, Set<BulkResult.BulkResultItem> indexBlocks) {
        final Set<String> blockedMessageIds = indexBlocks.stream().map(item -> item.id).collect(Collectors.toSet());

        return chunk.stream().filter(entry -> blockedMessageIds.contains(entry.getValue().getId())).collect(Collectors.toList());
    }

    private Set<BulkResult.BulkResultItem> indexBlocksFrom(List<BulkResult.BulkResultItem> allFailedItems) {
        return allFailedItems.stream().filter(this::hasFailedDueToBlockedIndex).collect(Collectors.toSet());
    }

    private boolean hasFailedDueToBlockedIndex(BulkResult.BulkResultItem item) {
        return item.errorType.equals(INDEX_BLOCK_ERROR) && item.errorReason.equals(INDEX_BLOCK_REASON);
    }

    private void recordTimestamp(List<Map.Entry<IndexSet, Message>> messageList, Set<String> failedIds) {
        for (final Map.Entry<IndexSet, Message> entry : messageList) {
            final Message message = entry.getValue();

            if (failedIds.contains(message.getId())) {
                continue;
            }

            processingStatusRecorder.updatePostIndexingReceiveTime(message.getReceiveTime());
        }
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

    private List<String> propagateFailure(List<BulkResult.BulkResultItem> items, List<Map.Entry<IndexSet, Message>> messageList) {
        final Map<String, Message> messageMap = messageList.stream()
                .map(Map.Entry::getValue)
                .distinct()
                .collect(Collectors.toMap(Message::getId, Function.identity()));
        final List<String> failedMessageIds = new ArrayList<>(items.size());
        final List<IndexFailure> indexFailures = new ArrayList<>(items.size());
        for (BulkResult.BulkResultItem item : items) {
            LOG.warn("Failed to index message: index=<{}> id=<{}> error=<{}>", item.index, item.id, item.error);

            // Write failure to index_failures.
            final Message messageEntry = messageMap.get(item.id);
            final Map<String, Object> doc = ImmutableMap.<String, Object>builder()
                    .put("letter_id", item.id)
                    .put("index", item.index)
                    .put("type", item.type)
                    .put("message", item.error)
                    .put("timestamp", messageEntry.getTimestamp())
                    .build();

            indexFailures.add(new IndexFailureImpl(doc));

            failedMessageIds.add(item.id);
        }

        try {
            // TODO: Magic number
            indexFailureQueue.offer(indexFailures, 25, TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
            LOG.warn("Couldn't save index failures.", e);
        }

        return failedMessageIds;
    }

    public Index prepareIndexRequest(String index, Map<String, Object> source, String id) {
        source.remove(Message.FIELD_ID);

        return new Index.Builder(source)
                .index(index)
                .type(IndexMapping.TYPE_MESSAGE)
                .id(id)
                .build();
    }

    public LinkedBlockingQueue<List<IndexFailure>> getIndexFailureQueue() {
        return indexFailureQueue;
    }

    private class EntityTooLargeException extends Exception {
        private final int indexedSuccessfully;
        private final List<BulkResult.BulkResultItem> failedItems;

        public EntityTooLargeException(int indexedSuccessfully, List<BulkResult.BulkResultItem> failedItems) {
            this.indexedSuccessfully = indexedSuccessfully;
            this.failedItems = failedItems;
        }
    }
}
