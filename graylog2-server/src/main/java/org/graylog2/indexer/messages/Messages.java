/**
 * This file is part of Graylog2.
 *
 * Graylog2 is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Graylog2 is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Graylog2.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.graylog2.indexer.messages;

import com.google.common.collect.Lists;
import com.google.inject.Inject;
import com.google.inject.Singleton;
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
import org.elasticsearch.action.support.replication.ReplicationType;
import org.elasticsearch.client.Client;
import org.elasticsearch.common.xcontent.XContentType;
import org.elasticsearch.indices.IndexMissingException;
import org.elasticsearch.node.Node;
import org.graylog2.Configuration;
import org.graylog2.indexer.DeadLetter;
import org.graylog2.indexer.Deflector;
import org.graylog2.indexer.results.ResultMessage;
import org.graylog2.plugin.Message;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * @author Lennart Koopmann <lennart@socketfeed.com>
 */
@Singleton
public class Messages {
    public static final String TYPE = "message";
    private static final Logger log = LoggerFactory.getLogger(Messages.class);

    private final Client c;
    private final Configuration configuration;
    private LinkedBlockingQueue<List<DeadLetter>> deadLetterQueue;

    @Inject
	public Messages(Node node, Configuration configuration) {
        this.configuration = configuration;
        this.c = node.client();
        this.deadLetterQueue = new LinkedBlockingQueue<>(1000);
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

        final BulkRequestBuilder request = c.prepareBulk();
        for (Message msg : messages) {
            request.add(buildIndexRequest(configuration.getElasticSearchIndexPrefix() + "_" + Deflector.DEFLECTOR_SUFFIX,
                                          msg.toElasticSearchObject(),
                                          msg.getId())); // Main index.
        }

        request.setConsistencyLevel(WriteConsistencyLevel.ONE);
        request.setReplicationType(ReplicationType.ASYNC);

        final BulkResponse response = c.bulk(request.request()).actionGet();

        log.debug("Deflector index: Bulk indexed {} messages, took {} ms, failures: {}",
                  response.getItems().length, response.getTookInMillis(), response.hasFailures());

        if (response.hasFailures()) {
            propagateFailure(response.getItems(), messages, response.buildFailureMessage());
        }

        return !response.hasFailures();
    }

    private void propagateFailure(BulkItemResponse[] items, List<Message> messages, String errorMessage) {
        log.error(
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

        if(!r) {
            log.debug("Could not propagate failure to failure queue. Queue is full.");
        }
    }

    private IndexRequestBuilder buildIndexRequest(String index, Map<String, Object> source, String id) {
        final IndexRequestBuilder b = new IndexRequestBuilder(c);

        b.setId(id);
        b.setSource(source);
        b.setIndex(index);
        b.setContentType(XContentType.JSON);
        b.setOpType(IndexRequest.OpType.INDEX);
        b.setType(TYPE);
        b.setConsistencyLevel(WriteConsistencyLevel.ONE);

        return b;
    }
}
