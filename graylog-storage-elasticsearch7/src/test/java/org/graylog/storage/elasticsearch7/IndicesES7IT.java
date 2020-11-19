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
package org.graylog.storage.elasticsearch7;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ImmutableMap;
import org.graylog.storage.elasticsearch7.cat.CatApi;
import org.graylog.storage.elasticsearch7.cluster.ClusterStateApi;
import org.graylog.storage.elasticsearch7.stats.StatsApi;
import org.graylog.storage.elasticsearch7.testing.ElasticsearchInstanceES7;
import org.graylog.testing.elasticsearch.ElasticsearchInstance;
import org.graylog2.indexer.cluster.NodeAdapter;
import org.graylog2.indexer.indices.IndicesAdapter;
import org.graylog2.indexer.indices.IndicesIT;
import org.graylog2.shared.bindings.providers.ObjectMapperProvider;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

public class IndicesES7IT extends IndicesIT {
    @Rule
    public final ElasticsearchInstanceES7 elasticsearch = ElasticsearchInstanceES7.create();

    @Override
    @Before
    public void setUp() {
        super.setUp();
    }

    @Override
    protected ElasticsearchInstance elasticsearch() {
        return elasticsearch;
    }

    @Override
    protected IndicesAdapter indicesAdapter() {
        final ObjectMapper objectMapper = new ObjectMapperProvider().get();
        final ElasticsearchClient client = elasticsearch.elasticsearchClient();
        return new IndicesAdapterES7(
                client,
                new StatsApi(objectMapper, client),
                new CatApi(objectMapper, client),
                new ClusterStateApi(objectMapper, client)
        );
    }

    @Override
    protected NodeAdapter createNodeAdapter() {
        return new NodeAdapterES7(elasticsearch.elasticsearchClient());
    }

    @Override
    protected Map<String, Object> createTemplateFor(String indexWildcard, Map<String, Object> mapping) {
        return ImmutableMap.of(
                "template", indexWildcard,
                "mappings", mapping
        );
    }

    // Prevent accidental use of AliasActions.Type.REMOVE_INDEX,
    // as despite being an *Alias* Action, it actually deletes an index!
    @Test
    public void cyclingAliasLeavesOldIndexInPlace() {
        final String deflector = "indices_it_deflector";

        final String index1 = client().createRandomIndex("indices_it_");
        final String index2 = client().createRandomIndex("indices_it_");

        client().addAliasMapping(index1, deflector);

        indices.cycleAlias(deflector, index2, index1);

        assertThat(indices.exists(index1)).isTrue();
    }
}
