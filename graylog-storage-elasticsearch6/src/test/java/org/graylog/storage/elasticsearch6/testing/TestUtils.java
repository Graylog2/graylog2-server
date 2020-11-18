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

import io.searchbox.client.JestClient;
import org.graylog.testing.elasticsearch.ElasticsearchInstance;

public class TestUtils {
    public static JestClient jestClient(ElasticsearchInstance elasticsearchInstance) {
        if (elasticsearchInstance instanceof ElasticsearchInstanceES6) {
            return ((ElasticsearchInstanceES6) elasticsearchInstance).jestClient();
        }

        throw new RuntimeException("Unable to return Jest client, Elasticsearch instance is of wrong type: " + elasticsearchInstance);
    }
}
