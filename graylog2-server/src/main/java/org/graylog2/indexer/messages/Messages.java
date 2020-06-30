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

import com.github.joschi.jadconfig.util.Duration;
import com.github.rholder.retry.Attempt;
import com.github.rholder.retry.RetryException;
import com.github.rholder.retry.RetryListener;
import com.github.rholder.retry.Retryer;
import com.github.rholder.retry.RetryerBuilder;
import com.github.rholder.retry.WaitStrategies;
import com.google.auto.value.AutoValue;
import com.google.common.collect.ImmutableMap;
import org.graylog2.indexer.IndexFailure;
import org.graylog2.indexer.IndexFailureImpl;
import org.graylog2.indexer.IndexSet;
import org.graylog2.indexer.results.ResultMessage;
import org.graylog2.plugin.Message;
import org.graylog2.system.processing.ProcessingStatusRecorder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Singleton
public class Messages {
    private static final Logger LOG = LoggerFactory.getLogger(Messages.class);

    private static final Duration MAX_WAIT_TIME = Duration.seconds(30L);

    @SuppressWarnings("UnstableApiUsage")
    private static final Retryer<List<IndexingError>> BULK_REQUEST_RETRYER = RetryerBuilder.<List<IndexingError>>newBuilder()
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


    private final LinkedBlockingQueue<List<IndexFailure>> indexFailureQueue;
    private final MessagesAdapter messagesAdapter;
    private final ProcessingStatusRecorder processingStatusRecorder;
    private final TrafficAccounting trafficAccounting;

    @Inject
    public Messages(TrafficAccounting trafficAccounting,
                    MessagesAdapter messagesAdapter,
                    ProcessingStatusRecorder processingStatusRecorder) {
        this.trafficAccounting = trafficAccounting;
        this.messagesAdapter = messagesAdapter;
        this.processingStatusRecorder = processingStatusRecorder;

        // TODO: Magic number
        this.indexFailureQueue = new LinkedBlockingQueue<>(1000);
    }

    public ResultMessage get(String messageId, String index) throws DocumentNotFoundException, IOException {
        return messagesAdapter.get(messageId, index);
    }

    public List<String> analyze(String toAnalyze, String index, String analyzer) throws IOException {
        return messagesAdapter.analyze(toAnalyze, index, analyzer);
    }

    public List<String> bulkIndex(final List<Map.Entry<IndexSet, Message>> messageList) {
        return bulkIndex(messageList, false);
    }

    public List<String> bulkIndex(final List<Map.Entry<IndexSet, Message>> messageList, boolean isSystemTraffic) {
        if (messageList.isEmpty()) {
            return Collections.emptyList();
        }

        final List<IndexingRequest> indexingRequestList = messageList.stream()
                .map(entry -> IndexingRequest.create(entry.getKey(), entry.getValue()))
                .collect(Collectors.toList());

        final List<IndexingError> indexFailures = runBulkRequest(indexingRequestList, messageList.size());

        final Set<String> failedIds = indexFailures.stream()
                .map(indexingError -> indexingError.message().getId())
                .collect(Collectors.toSet());
        final List<IndexingRequest> successfulRequests = indexingRequestList.stream()
                .filter(indexingRequest -> !failedIds.contains(indexingRequest.message().getId()))
                .collect(Collectors.toList());

        recordTimestamp(successfulRequests);
        accountTotalMessageSizes(indexingRequestList, isSystemTraffic);

        return propagateFailure(indexFailures);
    }

    private List<IndexingError> runBulkRequest(List<IndexingRequest> indexingRequestList, int count) {
        try {
            return BULK_REQUEST_RETRYER.call(() -> messagesAdapter.bulkIndex(indexingRequestList));
        } catch (ExecutionException | RetryException e) {
            if (e instanceof RetryException) {
                LOG.error("Could not bulk index {} messages. Giving up after {} attempts.", count, ((RetryException) e).getNumberOfFailedAttempts());
            } else {
                LOG.error("Couldn't bulk index " + count + " messages.", e);
            }
            throw new RuntimeException(e);
        }
    }

    private void accountTotalMessageSizes(List<IndexingRequest> requests, boolean isSystemTraffic) {
        final long totalSizeOfIndexedMessages = requests.stream()
                .map(IndexingRequest::message)
                .mapToLong(Message::getSize)
                .sum();

        if (isSystemTraffic) {
            trafficAccounting.addSystemTraffic(totalSizeOfIndexedMessages);
        } else {
            trafficAccounting.addOutputTraffic(totalSizeOfIndexedMessages);
        }
    }

    private void recordTimestamp(List<IndexingRequest> messageList) {
        for (final IndexingRequest entry : messageList) {
            final Message message = entry.message();

            processingStatusRecorder.updatePostIndexingReceiveTime(message.getReceiveTime());
        }
    }

    private List<String> propagateFailure(List<IndexingError> indexingErrors) {
        if (indexingErrors.isEmpty()) {
            return Collections.emptyList();
        }

        final List<IndexFailure> indexFailures = indexingErrors.stream()
                .map(IndexingError::toIndexFailure)
                .collect(Collectors.toList());

        try {
            // TODO: Magic number
            indexFailureQueue.offer(indexFailures, 25, TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
            LOG.warn("Couldn't save index failures.", e);
        }

        return indexFailures.stream()
                .map(IndexFailure::letterId)
                .collect(Collectors.toList());
    }

    public LinkedBlockingQueue<List<IndexFailure>> getIndexFailureQueue() {
        return indexFailureQueue;
    }

    @AutoValue
    public abstract static class IndexingError {
        public enum ErrorType {
            IndexBlocked,
            MappingError,
            Unknown;
        }
        public abstract Message message();
        public abstract String index();
        public abstract ErrorType errorType();
        public abstract String errorMessage();

        public static IndexingError create(Message message, String index, ErrorType errorType, String errorMessage) {
            return new AutoValue_Messages_IndexingError(message, index, errorType, errorMessage);
        }

        public static IndexingError create(Message message, String index) {
            return create(message, index, ErrorType.Unknown, "");
        }

        public IndexFailure toIndexFailure() {
            final Message message = this.message();
            final Map<String, Object> doc = ImmutableMap.<String, Object>builder()
                    .put("letter_id", message.getId())
                    .put("index", this.index())
                    .put("type", this.errorType().toString())
                    .put("message", this.errorMessage())
                    .put("timestamp", message.getTimestamp())
                    .build();

            return new IndexFailureImpl(doc);
        }
    }
}
