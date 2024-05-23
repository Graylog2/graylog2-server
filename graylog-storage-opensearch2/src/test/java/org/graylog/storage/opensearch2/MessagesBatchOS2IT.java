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
package org.graylog.storage.opensearch2;

import org.graylog.storage.opensearch2.testing.OpenSearchInstance;
import org.graylog.storage.opensearch2.testing.OpenSearchInstanceBuilder;
import org.graylog.testing.elasticsearch.SearchServerInstance;
import org.graylog2.indexer.messages.MessagesBatchIT;
import org.junit.Rule;

public class MessagesBatchOS2IT extends MessagesBatchIT {
    @Rule
    public final OpenSearchInstance openSearchInstance = OpenSearchInstanceBuilder.builder()
            .heapSize("256m")
            // Disable the real memory circuit breaker because it has issues with JDK 21. Turning it off relaxes
            // the limits in the circuit breaker and makes our test work correctly.
            // See: https://github.com/opensearch-project/OpenSearch/issues/12694
            //      https://opensearch.org/docs/latest/install-and-configure/configuring-opensearch/circuit-breaker/#parent-circuit-breaker-settings
            .env("indices.breaker.total.use_real_memory", "false")
            .build();

    @Override
    protected SearchServerInstance searchServer() {
        return this.openSearchInstance;
    }
}
