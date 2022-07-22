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
package org.graylog.storage.elasticsearch6.testing;

import org.graylog.testing.containermatrix.SearchServer;
import org.graylog.testing.elasticsearch.TestableSearchServerInstance;
import org.graylog2.storage.SearchVersion;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.Network;

public class ElasticsearchInstanceSmallES6 extends ElasticsearchInstanceES6 {

    public ElasticsearchInstanceSmallES6(String image, SearchVersion version, Network network) {
        super(image, version, network);
    }

    public static TestableSearchServerInstance create() {
        return create(Network.newNetwork());
    }

    public static TestableSearchServerInstance create(Network network) {
        return create(SearchServer.ES6.getSearchVersion(), network);
    }

    public static TestableSearchServerInstance create(SearchVersion version, Network network) {
        final String image = imageNameFrom(version);

        return new ElasticsearchInstanceSmallES6(image, version, network);
    }

    @Override
    public GenericContainer<?> buildContainer(String image, Network network) {
        // Reduce ES memory to 256MB
        return super.buildContainer(image, network)
                .withEnv("ES_JAVA_OPTS", "-Xms256m -Xmx256m -Dlog4j2.formatMsgNoLookups=true");
    }
}
