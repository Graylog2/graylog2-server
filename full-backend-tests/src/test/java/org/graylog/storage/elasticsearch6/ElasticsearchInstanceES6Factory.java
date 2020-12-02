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

import org.graylog.storage.elasticsearch6.testing.ElasticsearchInstanceES6;
import org.graylog.testing.completebackend.ElasticsearchInstanceFactory;
import org.graylog.testing.elasticsearch.ElasticsearchInstance;
import org.testcontainers.containers.Network;

public class ElasticsearchInstanceES6Factory implements ElasticsearchInstanceFactory {
    @Override
    public ElasticsearchInstance create(Network network) {
        return ElasticsearchInstanceES6.create(network);
    }

    @Override
    public String version() {
        return "6";
    }
}
