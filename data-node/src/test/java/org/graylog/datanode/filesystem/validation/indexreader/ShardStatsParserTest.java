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
package org.graylog.datanode.filesystem.validation.indexreader;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;

import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Path;

class ShardStatsParserTest {
    @Test
    void testOpensearch2() throws URISyntaxException {
        final ShardStatsParser shardStatsParser = new ShardStatsParserImpl();
        final URI shard = getClass().getResource("/indices/opensearch/nodes/0/indices/7z16oEKPTjivI0qd4tv36Q/0").toURI();
        final ShardStats stats = shardStatsParser.read(Path.of(shard));
        Assertions.assertThat(stats.documentsCount()).isEqualTo(1);
        Assertions.assertThat(stats.minSegmentLuceneVersion().toString()).isEqualTo("9.7.0");
    }

    @Test
    void testElasticsearch7() throws URISyntaxException {
        final ShardStatsParser shardStatsParser = new ShardStatsParserImpl();
        final URI shard = getClass().getResource("/indices/elasticsearch/nodes/0/indices/JwZYQzvUQG6JxLBgMsFfTA/0").toURI();
        final ShardStats stats = shardStatsParser.read(Path.of(shard));
        Assertions.assertThat(stats.documentsCount()).isEqualTo(1);
        Assertions.assertThat(stats.minSegmentLuceneVersion().toString()).isEqualTo("8.7.0");
    }
}
