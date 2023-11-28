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
import org.graylog.shaded.opensearch2.org.apache.lucene.util.Version;
import org.junit.jupiter.api.Test;

import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Path;
import java.util.Optional;

class ShardStatsParserTest {
    @Test
    void testOpensearch2() throws URISyntaxException {
        final ShardStatsParser shardStatsParser = new ShardStatsParserImpl();
        final URI shard = getClass().getResource("/indices/opensearch/nodes/0/indices/7z16oEKPTjivI0qd4tv36Q/0").toURI();
        final Optional<ShardStats> stats = shardStatsParser.read(Path.of(shard));
        Assertions.assertThat(stats).map(ShardStats::documentsCount).hasValue(1);
        Assertions.assertThat(stats).map(ShardStats::minSegmentLuceneVersion).map(Version::toString).hasValue("9.7.0");
    }

    @Test
    void testElasticsearch7() throws URISyntaxException {
        final ShardStatsParser shardStatsParser = new ShardStatsParserImpl();
        final URI shard = getClass().getResource("/indices/elasticsearch/nodes/0/indices/JwZYQzvUQG6JxLBgMsFfTA/0").toURI();
        final Optional<ShardStats> stats = shardStatsParser.read(Path.of(shard));
        Assertions.assertThat(stats).map(ShardStats::documentsCount).hasValue(1);
        Assertions.assertThat(stats).map(ShardStats::minSegmentLuceneVersion).map(Version::toString).hasValue("8.7.0");
    }
}
