package org.graylog.storage.elasticsearch7;

import org.graylog.shaded.elasticsearch7.org.elasticsearch.client.core.CountRequest;
import org.graylog.shaded.elasticsearch7.org.elasticsearch.client.core.CountResponse;
import org.graylog2.indexer.counts.CountsAdapter;

import javax.inject.Inject;
import java.util.List;

public class CountsAdapterES7 implements CountsAdapter {
    private final ElasticsearchClient client;

    @Inject
    public CountsAdapterES7(ElasticsearchClient client) {
        this.client = client;
    }

    @Override
    public long totalCount(List<String> indices) {
        final CountRequest request = new CountRequest(indices.toArray(new String[0]));

        final CountResponse result = this.client.execute((c, requestOptions) -> c.count(request, requestOptions));

        return result.getCount();
    }
}
