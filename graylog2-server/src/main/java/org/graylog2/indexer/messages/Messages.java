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

import com.codahale.metrics.Meter;
import com.codahale.metrics.MetricRegistry;
import com.github.joschi.jadconfig.util.Duration;
import com.github.rholder.retry.Attempt;
import com.github.rholder.retry.RetryException;
import com.github.rholder.retry.RetryListener;
import com.github.rholder.retry.Retryer;
import com.github.rholder.retry.RetryerBuilder;
import com.github.rholder.retry.WaitStrategies;
import com.google.common.collect.ImmutableMap;
import io.searchbox.client.JestClient;
import io.searchbox.client.JestResult;
import io.searchbox.core.Bulk;
import io.searchbox.core.BulkResult;
import io.searchbox.core.DocumentResult;
import io.searchbox.core.Get;
import io.searchbox.core.Index;
import io.searchbox.indices.Analyze;
import io.searchbox.params.Parameters;
import org.graylog2.indexer.IndexFailure;
import org.graylog2.indexer.IndexFailureImpl;
import org.graylog2.indexer.IndexMapping;
import org.graylog2.indexer.IndexSet;
import org.graylog2.indexer.results.ResultMessage;
import org.graylog2.plugin.Message;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
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
    private final JestClient client;
    private final LinkedBlockingQueue<List<IndexFailure>> indexFailureQueue;

    @Inject
    public Messages(MetricRegistry metricRegistry,
                    JestClient client) {
        invalidTimestampMeter = metricRegistry.meter(name(Messages.class, "invalid-timestamps"));
        this.client = client;

        // TODO: Magic number
        this.indexFailureQueue =  new LinkedBlockingQueue<>(1000);
    }

    public ResultMessage get(String messageId, String index) throws DocumentNotFoundException, IOException {
        final Get get = new Get.Builder(index, messageId).type(IndexMapping.TYPE_MESSAGE).build();
        final DocumentResult result = client.execute(get);

        if (!result.isSucceeded()) {
            throw new DocumentNotFoundException(index, messageId);
        }

        @SuppressWarnings("unchecked")
        final Map<String, Object> message = (Map<String, Object>) result.getSourceAsObject(Map.class, false);

        return ResultMessage.parseFromSource(result.getId(), result.getIndex(), message);
    }

    public List<String> analyze(String toAnalyze, String index, String analyzer) throws IOException {
        final Analyze analyze = new Analyze.Builder().index(index).analyzer(analyzer).text(toAnalyze).build();
        final JestResult result = client.execute(analyze);

        @SuppressWarnings("unchecked")
        final List<Map<String, Object>> tokens = (List<Map<String, Object>>) result.getValue("tokens");
        final List<String> terms = new ArrayList<>(tokens.size());
        tokens.forEach(token -> terms.add((String)token.get("token")));

        return terms;
    }

    public List<String> bulkIndex(final List<Map.Entry<IndexSet, Message>> messageList) {
        if (messageList.isEmpty()) {
            return Collections.emptyList();
        }

        final Bulk.Builder bulk = new Bulk.Builder();
        for (Map.Entry<IndexSet, Message> entry : messageList) {
            final String id = entry.getValue().getId();
            bulk.addAction(new Index.Builder(entry.getValue().toElasticSearchObject(invalidTimestampMeter))
                .index(entry.getKey().getWriteIndexAlias())
                .type(IndexMapping.TYPE_MESSAGE)
                .id(id)
                .build());
        }

        final BulkResult result = runBulkRequest(bulk.build(), messageList.size());
        final List<BulkResult.BulkResultItem> failedItems = result.getFailedItems();

        if (LOG.isDebugEnabled()) {
            LOG.debug("Index: Bulk indexed {} messages, took {} ms, failures: {}",
                    result.getItems().size(), result, failedItems.size());
        }

        if (!failedItems.isEmpty()) {
            return propagateFailure(failedItems, messageList, result.getErrorMessage());
        } else {
            return Collections.emptyList();
        }
    }

    private BulkResult runBulkRequest(final Bulk request, int count) {
        try {
            return BULK_REQUEST_RETRYER.call(() -> client.execute(request));
        } catch (ExecutionException | RetryException e) {
            if (e instanceof RetryException) {
                LOG.error("Could not bulk index {} messages. Giving up after {} attempts.", count, ((RetryException) e).getNumberOfFailedAttempts());
            } else {
                LOG.error("Couldn't bulk index " + count + " messages.", e);
            }
            throw new RuntimeException(e);
        }
    }

    private List<String> propagateFailure(List<BulkResult.BulkResultItem> items, List<Map.Entry<IndexSet, Message>> messageList, String errorMessage) {
        final Map<String, Message> messageMap = messageList.stream()
            .map(Map.Entry::getValue)
            .distinct()
            .collect(Collectors.toMap(Message::getId, Function.identity()));
        final List<String> failedMessageIds = new ArrayList<>(items.size());
        final List<IndexFailure> indexFailures = new ArrayList<>(items.size());
        for (BulkResult.BulkResultItem item : items) {
            LOG.trace("Failed to index message: {}", item.error);

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

        LOG.error("Failed to index [{}] messages. Please check the index error log in your web interface for the reason. Error: {}",
                indexFailures.size(), errorMessage);

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
                .setParameter(Parameters.CONSISTENCY, "one")
                .build();
    }

    public LinkedBlockingQueue<List<IndexFailure>> getIndexFailureQueue() {
        return indexFailureQueue;
    }
}
