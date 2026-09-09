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

class Lucene9ShardStatsParserTest {

    private Lucene9ShardStatsParser shardStatsParser;

    @BeforeEach
    void setUp() {
        shardStatsParser = new Lucene9ShardStatsParser();
    }

    @Test
    void testOpensearch1() throws URISyntaxException {
        final URI shard = getClass().getResource("/indices/opensearch1/nodes/0/indices/ccMlzvWBQT2YkL8LawXC1Q/0").toURI();
        final ShardStats stats = shardStatsParser.read(Path.of(shard));
        Assertions.assertThat(stats.documentsCount()).isEqualTo(1);
        Assertions.assertThat(stats.minSegmentLuceneVersion()).isEqualTo("8.10.1");
    }

    @Test
    void testElasticsearch7() throws URISyntaxException {
        final URI shard = getClass().getResource("/indices/elasticsearch7/nodes/0/indices/JwZYQzvUQG6JxLBgMsFfTA/0").toURI();
        final ShardStats stats = shardStatsParser.read(Path.of(shard));
        Assertions.assertThat(stats.documentsCount()).isEqualTo(1);
        Assertions.assertThat(stats.minSegmentLuceneVersion()).isEqualTo("8.7.0");
    }

    @Test
    void testOpensearch2IsStillReadable() throws URISyntaxException {
        // Lucene 9 reads its own format too, so the fallback doesn't narrow the supported range
        final URI shard = getClass().getResource("/indices/opensearch2/nodes/0/indices/7z16oEKPTjivI0qd4tv36Q/0").toURI();
        final ShardStats stats = shardStatsParser.read(Path.of(shard));
        Assertions.assertThat(stats.documentsCount()).isEqualTo(1);
        Assertions.assertThat(stats.minSegmentLuceneVersion()).isEqualTo("9.7.0");
    }

    @Test
    void testElasticsearch6() throws URISyntaxException {
        // Lucene 7 segments are two majors away and out of reach for Lucene 9 as well
        final URI shard = getClass().getResource("/indices/elasticsearch6/nodes/0/indices/MHXMFgomR8iU-44k_aLZtw/0/").toURI();
        Assertions.assertThatThrownBy(() -> shardStatsParser.read(Path.of(shard)))
                .isInstanceOf(IncompatibleIndexVersionException.class);
    }

    @Test
    void testNonExistentPath() {
        Assertions.assertThatThrownBy(() -> shardStatsParser.read(Path.of("/nonexistent/shard")))
                .isInstanceOf(IndexerInformationParserException.class);
    }
}
