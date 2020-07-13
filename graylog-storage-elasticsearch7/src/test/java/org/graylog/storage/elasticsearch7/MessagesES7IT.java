package org.graylog.storage.elasticsearch7;

import com.codahale.metrics.MetricRegistry;
import org.graylog.shaded.elasticsearch7.org.elasticsearch.action.admin.indices.refresh.RefreshRequest;
import org.graylog.shaded.elasticsearch7.org.elasticsearch.action.index.IndexRequest;
import org.graylog.shaded.elasticsearch7.org.elasticsearch.action.index.IndexResponse;
import org.graylog.shaded.elasticsearch7.org.elasticsearch.client.core.CountRequest;
import org.graylog.shaded.elasticsearch7.org.elasticsearch.client.core.CountResponse;
import org.graylog.shaded.elasticsearch7.org.elasticsearch.rest.RestStatus;
import org.graylog.storage.elasticsearch7.testing.ElasticsearchInstanceES7;
import org.graylog.testing.elasticsearch.ElasticsearchInstance;
import org.graylog2.indexer.messages.ChunkedBulkIndexer;
import org.graylog2.indexer.messages.MessagesAdapter;
import org.graylog2.indexer.messages.MessagesIT;
import org.junit.Rule;

import java.util.Map;

public class MessagesES7IT extends MessagesIT {
    @Rule
    public final ElasticsearchInstanceES7 elasticsearch = ElasticsearchInstanceES7.create();

    @Override
    protected ElasticsearchInstance elasticsearch() {
        return this.elasticsearch;
    }

    @Override
    protected MessagesAdapter createMessagesAdapter(MetricRegistry metricRegistry) {
        return new MessagesAdapterES7(this.elasticsearch.elasticsearchClient(), metricRegistry, new ChunkedBulkIndexer());
    }

    @Override
    protected long messageCount(String indexName) {
        this.elasticsearch.elasticsearchClient().execute((c, requestOptions) -> c.indices().refresh(new RefreshRequest(), requestOptions));

        final CountRequest countRequest = new CountRequest(indexName);
        final CountResponse result = this.elasticsearch.elasticsearchClient().execute((c, requestOptions) -> c.count(countRequest, requestOptions));

        return result.getCount();
    }

    @Override
    protected boolean indexMessage(String index, Map<String, Object> source, String id) {
        final IndexRequest indexRequest = new IndexRequest(index)
                .source(source)
                .id(id);

        final IndexResponse result = this.elasticsearch.elasticsearchClient().execute((c, requestOptions) -> c.index(indexRequest, requestOptions));

        return result.status().equals(RestStatus.CREATED);
    }
}
