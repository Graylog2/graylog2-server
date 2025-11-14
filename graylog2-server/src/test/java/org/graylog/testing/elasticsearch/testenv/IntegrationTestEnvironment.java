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

import org.apache.commons.lang3.RandomStringUtils;
import org.graylog.testing.elasticsearch.TestableSearchServerInstance;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Closeable;
import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

public class IntegrationTestEnvironment implements Closeable {

    private static final Logger LOG = LoggerFactory.getLogger(IntegrationTestEnvironment.class);

    private final Set<Closeable> cleanupObjects = new HashSet<>();
    private final TestableSearchServerInstance searchServerInstance;

    public IntegrationTestEnvironment(TestableSearchServerInstance searchServerInstance) {
        this.searchServerInstance = searchServerInstance;
    }

    public TestableIndex createIndex() {
        final var randomId = RandomStringUtils.secure().next(10, false, true);
        final String indexName = "test_index_" + randomId;
        searchServerInstance.client().createIndex(indexName);
        final TestableIndex index = new TestableIndex(indexName, searchServerInstance.client());
        cleanupObjects.add(index);
        return index;
    }

    public void refreshNode() {
        searchServerInstance.client().refreshNode();
    }

    @Override
    public void close() throws IOException {
        LOG.debug("Closing test environment, {} object will be automatically cleaned", cleanupObjects.size());
        for(Closeable closeable : cleanupObjects) {
            try {
                closeable.close();
            } catch (IOException e) {
                throw new RuntimeException("Failed to run cleanup for " + closeable, e);
            }
        }

    }
}
