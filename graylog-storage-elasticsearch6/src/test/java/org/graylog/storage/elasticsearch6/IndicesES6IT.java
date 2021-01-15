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

import com.google.common.collect.ImmutableMap;
import org.graylog.storage.elasticsearch6.testing.ElasticsearchInstanceES6;
import org.graylog.testing.elasticsearch.ElasticsearchInstance;
import org.graylog2.indexer.cluster.NodeAdapter;
import org.graylog2.indexer.IndexMapping;
import org.graylog2.indexer.indices.IndicesAdapter;
import org.graylog2.indexer.indices.IndicesIT;
import org.graylog2.shared.bindings.providers.ObjectMapperProvider;
import org.junit.Rule;

import java.util.Map;

import static org.graylog.storage.elasticsearch6.testing.TestUtils.jestClient;

public class IndicesES6IT extends IndicesIT {
    @Rule
    public final ElasticsearchInstance elasticsearch = ElasticsearchInstanceES6.create();

    @Override
    protected ElasticsearchInstance elasticsearch() {
        return this.elasticsearch;
    }

    @Override
    protected IndicesAdapter indicesAdapter() {
        return new IndicesAdapterES6(jestClient(elasticsearch),
                new ObjectMapperProvider().get(),
                new IndexingHelper());
    }

    @Override
    protected NodeAdapter createNodeAdapter() {
        return new NodeAdapterES6(jestClient(elasticsearch));
    }

    @Override
    protected Map<String, Object> createTemplateFor(String indexWildcard, Map<String, Object> mapping) {
        return ImmutableMap.of(
                "template", indexWildcard,
                "mappings", ImmutableMap.of(IndexMapping.TYPE_MESSAGE, mapping)
        );
    }
}
