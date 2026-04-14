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
package org.graylog.datanode.filesystem.index.indexreader;

import org.assertj.core.api.Assertions;
import org.graylog.datanode.filesystem.index.IncompatibleIndexVersionException;
import org.graylog.datanode.filesystem.index.IndexerInformationParserException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Path;

class ShardStatsParserTest {

    private ShardStatsParserImpl shardStatsParser;

    @BeforeEach
    void setUp() {
        shardStatsParser = new ShardStatsParserImpl();
    }

    @Test
    void testOpensearch2() throws URISyntaxException, IncompatibleIndexVersionException {
        final URI shard = getClass().getResource("/indices/opensearch2/nodes/0/indices/7z16oEKPTjivI0qd4tv36Q/0").toURI();
        final ShardStats stats = shardStatsParser.read(Path.of(shard));
        Assertions.assertThat(stats.documentsCount()).isEqualTo(1);
        Assertions.assertThat(stats.minSegmentLuceneVersion().toString()).isEqualTo("9.7.0");
    }

    @Test
    void testElasticsearch7() throws URISyntaxException, IncompatibleIndexVersionException {
        final URI shard = getClass().getResource("/indices/elasticsearch7/nodes/0/indices/JwZYQzvUQG6JxLBgMsFfTA/0").toURI();
        final ShardStats stats = shardStatsParser.read(Path.of(shard));
        Assertions.assertThat(stats.documentsCount()).isEqualTo(1);
        Assertions.assertThat(stats.minSegmentLuceneVersion().toString()).isEqualTo("8.7.0");
    }
    @Test
    void testElasticsearch6() throws URISyntaxException  {
        final URI shard = getClass().getResource("/indices/elasticsearch6/nodes/0/indices/MHXMFgomR8iU-44k_aLZtw/0/").toURI();
        Assertions.assertThatThrownBy(() -> shardStatsParser.read(Path.of(shard)))
                .isInstanceOf(IncompatibleIndexVersionException.class);
    }

    @Test
    void testOpensearch2MultipleSegments() throws URISyntaxException, IncompatibleIndexVersionException {
        // lmXxSRU5RkGiY4rPrDBAjQ/0 has four segments (_5/_6/_7/_8) — tests the min-across-segments logic
        final URI shardUri = getClass().getResource("/indices/opensearch2/nodes/0/indices/lmXxSRU5RkGiY4rPrDBAjQ/0").toURI();
        final Path shardPath = Path.of(shardUri);
        final ShardStats stats = shardStatsParser.read(shardPath);
        Assertions.assertThat(stats.documentsCount()).isGreaterThan(1);
        Assertions.assertThat(stats.minSegmentLuceneVersion().toString()).isEqualTo("9.7.0");
        Assertions.assertThat(stats.path()).isEqualTo(shardPath);
    }

    @Test
    void testOpensearch2EmptyShard() throws URISyntaxException, IncompatibleIndexVersionException {
        // dIdY0mrJQWOU5Xd1z7vLOw/0 contains only segments_2 + write.lock — no actual segments
        final URI shard = getClass().getResource("/indices/opensearch2/nodes/0/indices/dIdY0mrJQWOU5Xd1z7vLOw/0").toURI();
        final ShardStats stats = shardStatsParser.read(Path.of(shard));
        Assertions.assertThat(stats.documentsCount()).isEqualTo(0);
        // SegmentInfos.getMinSegmentLuceneVersion() returns null when there are no segments
        Assertions.assertThat(stats.minSegmentLuceneVersion()).isNull();
    }

    @Test
    void testNonExistentPath() {
        Assertions.assertThatThrownBy(() -> shardStatsParser.read(Path.of("/nonexistent/shard")))
                .isInstanceOf(IndexerInformationParserException.class);
    }
}
