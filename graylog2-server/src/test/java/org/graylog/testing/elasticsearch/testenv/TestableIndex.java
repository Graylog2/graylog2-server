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
package org.graylog.testing.elasticsearch.testenv;

import org.graylog.testing.elasticsearch.BulkIndexRequest;
import org.graylog.testing.elasticsearch.Client;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Closeable;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.IntStream;

public class TestableIndex implements Closeable {

    private static final Logger LOG = LoggerFactory.getLogger(TestableIndex.class);

    private final String indexName;
    private final Client client;

    public TestableIndex(String indexName, Client client) {
        this.indexName = indexName;
        this.client = client;
    }

    public String getIndexName() {
        return indexName;
    }

    public TestableIndex indexDocuments(int documentsCount, Function<Integer, Map<String, Object>> creator) {
        return indexDocuments(
                IntStream.range(0, documentsCount)
                        .mapToObj(creator::apply)
                        .toList()
        );
    }

    public TestableIndex indexDocuments(List<Map<String, Object>> docs) {
        final BulkIndexRequest req = new BulkIndexRequest();
        docs.forEach(d -> req.addRequest(indexName, d));
        client.bulkIndex(req);
        return this;
    }

    /**
     * Delete index when closing this object
     */
    @Override
    public void close() throws IOException {
        LOG.debug("Deleting index {}", indexName);
        client.deleteIndices(indexName);
    }

    @Override
    public String toString() {
        final StringBuffer sb = new StringBuffer("TestableIndex{");
        sb.append("indexName='").append(indexName).append('\'');
        sb.append('}');
        return sb.toString();
    }
}
