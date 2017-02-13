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
import com.github.rholder.retry.RetryException;
import com.github.rholder.retry.Retryer;
import com.github.rholder.retry.RetryerBuilder;
import com.github.rholder.retry.WaitStrategies;
import com.google.common.base.Throwables;
import com.google.common.collect.ImmutableMap;
import org.elasticsearch.ElasticsearchTimeoutException;
import org.elasticsearch.action.WriteConsistencyLevel;
import org.elasticsearch.action.admin.indices.analyze.AnalyzeResponse;
import org.elasticsearch.action.admin.indices.analyze.AnalyzeResponse.AnalyzeToken;
import org.elasticsearch.action.bulk.BulkItemResponse;
import org.elasticsearch.action.bulk.BulkRequest;
import org.elasticsearch.action.bulk.BulkRequestBuilder;
import org.elasticsearch.action.bulk.BulkResponse;
import org.elasticsearch.action.get.GetRequest;
import org.elasticsearch.action.get.GetResponse;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.client.Client;
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
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import static com.codahale.metrics.MetricRegistry.name;
import static com.google.common.base.Preconditions.checkNotNull;

@Singleton
public class Messages {
    private static final Logger LOG = LoggerFactory.getLogger(Messages.class);
    private static final Duration MAX_WAIT_TIME = Duration.seconds(30L);
    private static final Retryer<BulkResponse> BULK_REQUEST_RETRYER = RetryerBuilder.<BulkResponse>newBuilder()
            .retryIfException(t -> t instanceof ElasticsearchTimeoutException)
            .withWaitStrategy(WaitStrategies.exponentialWait(MAX_WAIT_TIME.getQuantity(), MAX_WAIT_TIME.getUnit()))
            .build();

    private final Client c;
    private final Meter invalidTimestampMeter;
    private final LinkedBlockingQueue<List<IndexFailure>> indexFailureQueue;

    @Inject
    public Messages(Client client,
                    MetricRegistry metricRegistry) {
        this.c = client;
        invalidTimestampMeter = metricRegistry.meter(name(Messages.class, "invalid-timestamps"));

        // TODO: Magic number
        this.indexFailureQueue =  new LinkedBlockingQueue<>(1000);
    }

    public ResultMessage get(String messageId, String index) throws DocumentNotFoundException {
        final GetRequest request = c.prepareGet(index, IndexMapping.TYPE_MESSAGE, messageId).request();
        final GetResponse r = c.get(request).actionGet();

        if (!r.isExists()) {
            throw new DocumentNotFoundException(index, messageId);
        }

        return ResultMessage.parseFromSource(r);
    }

    public List<String> analyze(String string, String index, String analyzer) {
        final AnalyzeResponse response = c.admin().indices().prepareAnalyze(index, string)
            .setAnalyzer(analyzer)
            .get();

        final List<AnalyzeToken> tokens = response.getTokens();
        final List<String> terms = new ArrayList<>(tokens.size());
        for (AnalyzeToken token : tokens) {
            terms.add(token.getTerm());
        }

        return terms;
    }

    public boolean bulkIndex(final List<Map.Entry<IndexSet, Message>> messageList) {
        if (messageList.isEmpty()) {
            return true;
        }

        final BulkRequestBuilder requestBuilder = c.prepareBulk().setConsistencyLevel(WriteConsistencyLevel.ONE);
        for (Map.Entry<IndexSet, Message> entry : messageList) {
            requestBuilder.add(buildIndexRequest(entry.getKey().getWriteIndexAlias(), entry.getValue().toElasticSearchObject(invalidTimestampMeter), entry.getValue().getId()));
        }

        final BulkResponse response = runBulkRequest(requestBuilder.request());

        LOG.debug("Index: Bulk indexed {} messages, took {} ms, failures: {}",
                response.getItems().length, response.getTookInMillis(), response.hasFailures());
        if (response.hasFailures()) {
            propagateFailure(response.getItems(), messageList, response.buildFailureMessage());
        }

        return !response.hasFailures();
    }

    private BulkResponse runBulkRequest(final BulkRequest request) {
        try {
            return c.bulk(request).actionGet();
        } catch (ElasticsearchTimeoutException timeoutException) {
            LOG.debug("Bulk indexing request timed out. Retrying.", timeoutException);
            try {
                return BULK_REQUEST_RETRYER.call(new BulkRequestCallable(c, request));
            } catch (ExecutionException | RetryException e) {
                LOG.error("Couldn't bulk index " + request.numberOfActions() + " messages.", e);
                throw new RuntimeException(e);
            }
        }
    }

    private void propagateFailure(BulkItemResponse[] items, List<Map.Entry<IndexSet, Message>> messageList, String errorMessage) {
        final List<IndexFailure> indexFailures = new LinkedList<>();
        for (BulkItemResponse item : items) {
            if (item.isFailed()) {
                LOG.trace("Failed to index message: {}", item.getFailureMessage());

                // Write failure to index_failures.
                final BulkItemResponse.Failure f = item.getFailure();
                final Map.Entry<IndexSet, Message> messageEntry = messageList.get(item.getItemId());
                final Map<String, Object> doc = ImmutableMap.<String, Object>builder()
                        .put("letter_id", item.getId())
                        .put("index", f.getIndex())
                        .put("type", f.getType())
                        .put("message", f.getMessage())
                        .put("timestamp", messageEntry.getValue().getTimestamp())
                        .build();

                indexFailures.add(new IndexFailureImpl(doc));
            }
        }

        LOG.error("Failed to index [{}] messages. Please check the index error log in your web interface for the reason. Error: {}",
                indexFailures.size(), errorMessage);

        try {
            // TODO: Magic number
            indexFailureQueue.offer(indexFailures, 25, TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
            LOG.warn("Couldn't save index failures.", e);
        }
    }

    public IndexRequest buildIndexRequest(String index, Map<String, Object> source, String id) {
        source.remove(Message.FIELD_ID);

        return c.prepareIndex(index, IndexMapping.TYPE_MESSAGE, id)
                .setSource(source)
                .setConsistencyLevel(WriteConsistencyLevel.ONE)
                .request();
    }

    public LinkedBlockingQueue<List<IndexFailure>> getIndexFailureQueue() {
        return indexFailureQueue;
    }

    private static class BulkRequestCallable implements Callable<BulkResponse> {
        private final Client client;
        private final BulkRequest request;

        public BulkRequestCallable(Client client, BulkRequest request) {
            this.client = checkNotNull(client);
            this.request = checkNotNull(request);
        }

        @Override
        public BulkResponse call() throws Exception {
            return client.bulk(request).actionGet();
        }
    }
}
