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
package org.graylog.storage.elasticsearch7.testing;

import org.graylog2.storage.SearchVersion;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.Network;

public class ElasticsearchInstanceSmallES7 extends ElasticsearchInstanceES7 {

    public ElasticsearchInstanceSmallES7(String image, SearchVersion version, Network network) {
        super(image, version, network);
    }

    public static ElasticsearchInstanceSmallES7 create() {
        return create(Network.newNetwork());
    }
    public static ElasticsearchInstanceSmallES7 create(Network network) {
        return create(SearchVersion.elasticsearch(ES_VERSION), network);
    }
    public static ElasticsearchInstanceSmallES7 create(SearchVersion searchVersion, Network network) {
        final String image = imageNameFrom(searchVersion.version());
        return new ElasticsearchInstanceSmallES7(image, searchVersion, network);
    }

    @Override
    public GenericContainer<?> buildContainer(String image, Network network) {
        // Reduce ES memory to 256MB
        return super.buildContainer(image, network)
                .withEnv("ES_JAVA_OPTS", "-Xms256m -Xmx256m -Dlog4j2.formatMsgNoLookups=true");
    }
}
