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
import com.github.rholder.retry.RetryException;
import com.github.rholder.retry.RetryerBuilder;
import com.github.rholder.retry.WaitStrategies;
import com.google.common.base.Predicate;
import com.google.common.collect.Lists;
import org.elasticsearch.ElasticsearchTimeoutException;
import org.elasticsearch.action.WriteConsistencyLevel;
import org.elasticsearch.action.admin.indices.analyze.AnalyzeRequestBuilder;
import org.elasticsearch.action.admin.indices.analyze.AnalyzeResponse;
import org.elasticsearch.action.admin.indices.analyze.AnalyzeResponse.AnalyzeToken;
import org.elasticsearch.action.bulk.BulkItemResponse;
import org.elasticsearch.action.bulk.BulkRequestBuilder;
import org.elasticsearch.action.bulk.BulkResponse;
import org.elasticsearch.action.get.GetRequestBuilder;
import org.elasticsearch.action.get.GetResponse;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.action.index.IndexRequestBuilder;
import org.elasticsearch.client.Client;
import org.elasticsearch.common.xcontent.XContentType;
import org.elasticsearch.indices.IndexMissingException;
import org.graylog2.configuration.ElasticsearchConfiguration;
import org.graylog2.indexer.DeadLetter;
import org.graylog2.indexer.Deflector;
import org.graylog2.indexer.IndexMapping;
import org.graylog2.indexer.results.ResultMessage;
import org.graylog2.plugin.Message;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.LinkedBlockingQueue;

@Singleton
public class Messages {
    private static final Logger LOG = LoggerFactory.getLogger(Messages.class);
    private static final Predicate<Throwable> ES_TIMEOUT_EXCEPTION_PREDICATE = new Predicate<Throwable>() {
        @Override
        public boolean apply(Throwable t) {
            return t instanceof ElasticsearchTimeoutException;
        }
    };
    private static final Duration MAX_WAIT_TIME = Duration.seconds(30L);

    private final Client c;
    private final String deflectorName;
    private LinkedBlockingQueue<List<DeadLetter>> deadLetterQueue;

    @Inject
    public Messages(Client client, ElasticsearchConfiguration configuration) {
        this.c = client;
        this.deadLetterQueue = new LinkedBlockingQueue<>(1000);
        this.deflectorName = Deflector.buildName(configuration.getIndexPrefix());
    }

    public LinkedBlockingQueue<List<DeadLetter>> getDeadLetterQueue() {
        return deadLetterQueue;
    }

    public ResultMessage get(String messageId, String index) throws IndexMissingException, DocumentNotFoundException {
        GetRequestBuilder grb = new GetRequestBuilder(c, index);
        grb.setId(messageId);

        GetResponse r = c.get(grb.request()).actionGet();

        if (!r.isExists()) {
            throw new DocumentNotFoundException();
        }

        return ResultMessage.parseFromSource(r);
    }

    public List<String> analyze(String string, String index) throws IndexMissingException {
        List<String> tokens = Lists.newArrayList();
        AnalyzeRequestBuilder arb = new AnalyzeRequestBuilder(c.admin().indices(), index, string);
        AnalyzeResponse r = c.admin().indices().analyze(arb.request()).actionGet();

        for (AnalyzeToken token : r.getTokens()) {
            tokens.add(token.getTerm());
        }

        return tokens;
    }

    public boolean bulkIndex(final List<Message> messages) {
        if (messages.isEmpty()) {
            return true;
        }

        final BulkRequestBuilder requestBuilder = c.prepareBulk().setConsistencyLevel(WriteConsistencyLevel.ONE);
        for (Message msg : messages) {
            requestBuilder.add(buildIndexRequest(deflectorName, msg.toElasticSearchObject(), msg.getId()));
        }

        final BulkResponse response;
        try {
            response = RetryerBuilder.<BulkResponse>newBuilder()
                    .retryIfException(ES_TIMEOUT_EXCEPTION_PREDICATE)
                    .withWaitStrategy(WaitStrategies.exponentialWait(MAX_WAIT_TIME.getQuantity(), MAX_WAIT_TIME.getUnit()))
                    .build()
                    .call(new Callable<BulkResponse>() {
                        @Override
                        public BulkResponse call() throws Exception {
                            return c.bulk(requestBuilder.request()).actionGet();
                        }
                    });
        } catch (ExecutionException | RetryException e) {
            LOG.error("Couldn't bulk index " + messages.size() + " messages.", e);
            return false;
        }

        LOG.debug("Deflector index: Bulk indexed {} messages, took {} ms, failures: {}",
                response.getItems().length, response.getTookInMillis(), response.hasFailures());

        if (response.hasFailures()) {
            propagateFailure(response.getItems(), messages, response.buildFailureMessage());
        }

        return !response.hasFailures();
    }

    private void propagateFailure(BulkItemResponse[] items, List<Message> messages, String errorMessage) {
        LOG.error(
                "Failed to index [{}] messages. Please check the index error log in your web interface for the reason. Error: {}",
                items.length,
                errorMessage);

        // Get all failed messages.
        List<DeadLetter> deadLetters = Lists.newArrayList();
        for (BulkItemResponse item : items) {
            if (item.isFailed()) {
                deadLetters.add(new DeadLetter(item, messages.get(item.getItemId())));
            }
        }

        boolean r = deadLetterQueue.offer(deadLetters);

        if (!r) {
            LOG.debug("Could not propagate failure to failure queue. Queue is full.");
        }
    }

    private IndexRequestBuilder buildIndexRequest(String index, Map<String, Object> source, String id) {
        return new IndexRequestBuilder(c)
                .setId(id)
                .setSource(source)
                .setIndex(index)
                .setContentType(XContentType.JSON)
                .setOpType(IndexRequest.OpType.INDEX)
                .setType(IndexMapping.TYPE_MESSAGE)
                .setConsistencyLevel(WriteConsistencyLevel.ONE);
    }
}
