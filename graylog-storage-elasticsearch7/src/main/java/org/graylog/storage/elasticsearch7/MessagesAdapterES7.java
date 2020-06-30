package org.graylog.storage.elasticsearch7;

import com.codahale.metrics.Meter;
import com.codahale.metrics.MetricRegistry;
import org.graylog.shaded.elasticsearch7.org.elasticsearch.action.bulk.BulkRequest;
import org.graylog.shaded.elasticsearch7.org.elasticsearch.action.bulk.BulkResponse;
import org.graylog.shaded.elasticsearch7.org.elasticsearch.action.get.GetRequest;
import org.graylog.shaded.elasticsearch7.org.elasticsearch.action.get.GetResponse;
import org.graylog.shaded.elasticsearch7.org.elasticsearch.action.index.IndexRequest;
import org.graylog2.indexer.IndexFailure;
import org.graylog2.indexer.messages.ChunkedBulkIndexer;
import org.graylog2.indexer.messages.DocumentNotFoundException;
import org.graylog2.indexer.messages.IndexingRequest;
import org.graylog2.indexer.messages.Messages;
import org.graylog2.indexer.messages.MessagesAdapter;
import org.graylog2.indexer.results.ResultMessage;

import javax.inject.Inject;
import java.io.IOException;
import java.util.Collections;
import java.util.List;

import static com.codahale.metrics.MetricRegistry.name;

public class MessagesAdapterES7 implements MessagesAdapter {
    private final ElasticsearchClient client;
    private final Meter invalidTimestampMeter;
    private final ChunkedBulkIndexer chunkedBulkIndexer;

    @Inject
    public MessagesAdapterES7(ElasticsearchClient elasticsearchClient, MetricRegistry metricRegistry, ChunkedBulkIndexer chunkedBulkIndexer) {
        this.client = elasticsearchClient;
        this.invalidTimestampMeter = metricRegistry.meter(name(Messages.class, "invalid-timestamps"));
        this.chunkedBulkIndexer = chunkedBulkIndexer;
    }

    @Override
    public ResultMessage get(String messageId, String index) throws IOException, DocumentNotFoundException {
        final GetRequest getRequest = new GetRequest(index, messageId);

        final GetResponse result = this.client.execute((c, requestOptions) -> c.get(getRequest, requestOptions));

        if (!result.isExists()) {
            throw new DocumentNotFoundException(index, messageId);
        }

        return ResultMessage.parseFromSource(messageId, index, result.getSource());
    }

    @Override
    public List<String> analyze(String toAnalyze, String index, String analyzer) throws IOException {
        return null;
    }

    @Override
    public List<IndexFailure> bulkIndex(List<IndexingRequest> messageList) {
        return chunkedBulkIndexer.index(messageList, this::bulkIndexChunked);
    }

    private List<IndexFailure> bulkIndexChunked(ChunkedBulkIndexer.Chunk command) throws ChunkedBulkIndexer.EntityTooLargeException {
        final List<IndexingRequest> messageList = command.requests;

        if (messageList.isEmpty()) {
            return Collections.emptyList();
        }

        final BulkRequest bulkRequest = new BulkRequest();
        messageList.forEach(request -> bulkRequest.add(
                indexRequestFrom(request)
        ));

        final BulkResponse result = this.client.execute((c, requestOptions) -> c.bulk(bulkRequest, requestOptions));

        return Collections.emptyList();
    }

    private IndexRequest indexRequestFrom(IndexingRequest request) {
        return new IndexRequest(request.indexSet().getWriteIndexAlias())
                .id(request.message().getId())
                .source(request.message().toElasticSearchObject(this.invalidTimestampMeter));
    }
}
