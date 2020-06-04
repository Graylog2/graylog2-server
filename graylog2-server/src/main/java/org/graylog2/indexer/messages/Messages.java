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
import com.codahale.metrics.MetricRegistry;
import io.searchbox.core.Index;
import org.graylog2.indexer.IndexFailure;
import org.graylog2.indexer.IndexMapping;
import org.graylog2.indexer.IndexSet;
import org.graylog2.indexer.results.ResultMessage;
import org.graylog2.plugin.GlobalMetricNames;
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
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Singleton
public class Messages {
    private static final Logger LOG = LoggerFactory.getLogger(Messages.class);

    private final LinkedBlockingQueue<List<IndexFailure>> indexFailureQueue;
    private final Counter outputByteCounter;
    private final Counter systemTrafficCounter;
    private final MessagesAdapter messagesAdapter;
    private final ProcessingStatusRecorder processingStatusRecorder;

    @Inject
    public Messages(MetricRegistry metricRegistry,
                    MessagesAdapter messagesAdapter, ProcessingStatusRecorder processingStatusRecorder) {
        outputByteCounter = metricRegistry.counter(GlobalMetricNames.OUTPUT_TRAFFIC);
        systemTrafficCounter = metricRegistry.counter(GlobalMetricNames.SYSTEM_OUTPUT_TRAFFIC);
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

        final List<IndexFailure> indexFailures = messagesAdapter.bulkIndex(indexingRequestList);

        final Set<String> failedIds = indexFailures.stream().map(IndexFailure::letterId).collect(Collectors.toSet());
        final List<IndexingRequest> successfullRequests = indexingRequestList.stream()
                .filter(indexingRequest -> !failedIds.contains(indexingRequest.message().getId()))
                .collect(Collectors.toList());

        recordTimestamp(successfullRequests);
        accountTotalMessageSizes(successfullRequests, isSystemTraffic);

        return propagateFailure(indexFailures);
    }

    private void accountTotalMessageSizes(List<IndexingRequest> successfullRequests, boolean isSystemTraffic) {
        final long totalSizeOfIndexedMessages = successfullRequests.stream()
                .map(IndexingRequest::message)
                .mapToLong(Message::getSize)
                .sum();

        if (isSystemTraffic) {
            systemTrafficCounter.inc(totalSizeOfIndexedMessages);
        } else {
            outputByteCounter.inc(totalSizeOfIndexedMessages);
        }
    }

    private void recordTimestamp(List<IndexingRequest> messageList) {
        for (final IndexingRequest entry : messageList) {
            final Message message = entry.message();

            processingStatusRecorder.updatePostIndexingReceiveTime(message.getReceiveTime());
        }
    }

    private List<String> propagateFailure(List<IndexFailure> indexFailures) {
        if (indexFailures.isEmpty()) {
            return Collections.emptyList();
        }

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
}
