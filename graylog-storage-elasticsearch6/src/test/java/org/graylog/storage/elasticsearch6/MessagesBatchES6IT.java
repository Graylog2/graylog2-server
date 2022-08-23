/*
 * Copyright (C) 2020 Graylog, Inc.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the Server Side Public License, version 1,
 * as published by MongoDB, Inc.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * Server Side Public License for more details.
 *
 * You should have received a copy of the Server Side Public License
 * along with this program. If not, see
 * <http://www.mongodb.com/licensing/server-side-public-license>.
 */
package org.graylog.storage.elasticsearch6;

import com.codahale.metrics.MetricRegistry;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.searchbox.core.Count;
import io.searchbox.core.CountResult;
import org.graylog.storage.elasticsearch6.jest.JestUtils;
import org.graylog.storage.elasticsearch6.testing.ElasticsearchInstanceES6;
import org.graylog.testing.elasticsearch.SearchServerInstance;
import org.graylog.testing.elasticsearch.TestableSearchServerInstance;
import org.graylog2.indexer.messages.ChunkedBulkIndexer;
import org.graylog2.indexer.messages.MessagesAdapter;
import org.graylog2.indexer.messages.MessagesBatchIT;
import org.graylog2.shared.bindings.providers.ObjectMapperProvider;
import org.junit.Ignore;
import org.junit.Rule;

import static org.graylog.storage.elasticsearch6.testing.TestUtils.jestClient;

@Ignore("This does not run on ES6 yet. It will just die with OOM")
public class MessagesBatchES6IT extends MessagesBatchIT {
    @Rule
    public final TestableSearchServerInstance elasticsearch = ElasticsearchInstanceES6.create("256m");

    private final ObjectMapper objectMapper = new ObjectMapperProvider().get();

    @Override
    protected SearchServerInstance elasticsearch() {
        return this.elasticsearch;
    }

    @Override
    protected MessagesAdapter createMessagesAdapter(MetricRegistry metricRegistry) {
        return new MessagesAdapterES6(jestClient(elasticsearch), true, metricRegistry, new ChunkedBulkIndexer(), objectMapper);
    }

    @Override
    protected long messageCount(String indexName) {
        final Count count = new Count.Builder().addIndex(indexName).build();

        final CountResult result = JestUtils.execute(jestClient(elasticsearch), count, () -> "Unable to count documents");
        return result.getCount().longValue();
    }
}
