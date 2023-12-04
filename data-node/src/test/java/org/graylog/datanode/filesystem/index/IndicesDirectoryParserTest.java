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
package org.graylog.datanode.filesystem.index;

import org.assertj.core.api.Assertions;
import org.graylog.datanode.filesystem.index.indexreader.ShardStatsParserImpl;
import org.graylog.datanode.filesystem.index.statefile.StateFileParserImpl;
import org.graylog.datanode.filesystem.index.dto.IndexInformation;
import org.graylog.datanode.filesystem.index.dto.IndexerDirectoryInformation;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Path;

class IndicesDirectoryParserTest {


    private IndicesDirectoryParser parser;

    @BeforeEach
    void setUp() {
        parser = new IndicesDirectoryParser(new StateFileParserImpl(), new ShardStatsParserImpl());
    }

    @Test
    void testOpensearch2() throws URISyntaxException {
        final URI uri = getClass().getResource("/indices/opensearch2").toURI();
        final IndexerDirectoryInformation result = parser.parse(Path.of(uri));
        Assertions.assertThat(result.nodes())
                .hasSize(1)
                .allSatisfy(node -> {
                    Assertions.assertThat(node.nodeVersion()).isEqualTo("2.10.0");
                    Assertions.assertThat(node.indices())
                            .hasSize(6)
                            .extracting(IndexInformation::indexName)
                            .contains(".opensearch-sap-log-types-config", ".plugins-ml-config", "graylog_0", ".opensearch-observability", ".opendistro_security", "security-auditlog-2023.11.24");

                    final IndexInformation graylog_0 = node.indices().stream().filter(i -> i.indexName().equals("graylog_0")).findFirst().orElseThrow(() -> new RuntimeException("Failed to detect graylog_0 index"));

                    Assertions.assertThat(graylog_0.indexVersionCreated()).isEqualTo("2.10.0");

                    Assertions.assertThat(graylog_0.shards())
                            .hasSize(1)
                            .allSatisfy(shard -> {
                                Assertions.assertThat(shard.documentsCount()).isEqualTo(1);
                                Assertions.assertThat(shard.name()).isEqualTo("S0");
                                Assertions.assertThat(shard.primary()).isEqualTo(true);
                                Assertions.assertThat(shard.minLuceneVersion()).isEqualTo("9.7.0");
                            });
                });
    }

    @Test
    void testOpensearch1() throws URISyntaxException {
        final URI uri = getClass().getResource("/indices/opensearch1").toURI();
        final IndexerDirectoryInformation result = parser.parse(Path.of(uri));
        Assertions.assertThat(result.nodes())
                .hasSize(1)
                .allSatisfy(node -> {
                    Assertions.assertThat(node.nodeVersion()).isEqualTo("1.3.0");
                    Assertions.assertThat(node.indices())
                            .hasSize(1)
                            .extracting(IndexInformation::indexName)
                            .contains("graylog_0");

                    final IndexInformation graylog_0 = node.indices().stream().filter(i -> i.indexName().equals("graylog_0")).findFirst().orElseThrow(() -> new RuntimeException("Failed to detect graylog_0 index"));

                    Assertions.assertThat(graylog_0.indexVersionCreated()).isEqualTo("1.3.0");

                    Assertions.assertThat(graylog_0.shards())
                            .hasSize(1)
                            .allSatisfy(shard -> {
                                Assertions.assertThat(shard.documentsCount()).isEqualTo(1);
                                Assertions.assertThat(shard.name()).isEqualTo("S0");
                                Assertions.assertThat(shard.primary()).isEqualTo(true);
                                Assertions.assertThat(shard.minLuceneVersion()).isEqualTo("8.10.1");
                            });
                });
    }


    @Test
    void testElasticsearch7() throws URISyntaxException {
        final URI uri = getClass().getResource("/indices/elasticsearch7").toURI();
        final IndexerDirectoryInformation result = parser.parse(Path.of(uri));
        Assertions.assertThat(result.nodes())
                .hasSize(1)
                .allSatisfy(node -> {
                    Assertions.assertThat(node.nodeVersion()).isEqualTo("7.10.0");
                    Assertions.assertThat(node.indices())
                            .hasSize(1)
                            .extracting(IndexInformation::indexName)
                            .contains("graylog_0");

                    final IndexInformation graylog_0 = node.indices().stream().filter(i -> i.indexName().equals("graylog_0")).findFirst().orElseThrow(() -> new RuntimeException("Failed to detect graylog_0 index"));

                    Assertions.assertThat(graylog_0.indexVersionCreated()).isEqualTo("7.10.0");

                    Assertions.assertThat(graylog_0.shards())
                            .hasSize(1)
                            .allSatisfy(shard -> {
                                Assertions.assertThat(shard.documentsCount()).isEqualTo(1);
                                Assertions.assertThat(shard.name()).isEqualTo("S0");
                                Assertions.assertThat(shard.primary()).isEqualTo(true);
                                Assertions.assertThat(shard.minLuceneVersion()).isEqualTo("8.7.0");
                            });
                });
    }

    @Test
    void testElasticsearch6() throws URISyntaxException {
        final URI uri = getClass().getResource("/indices/elasticsearch6").toURI();
       Assertions.assertThatThrownBy(()->parser.parse(Path.of(uri)))
               .isInstanceOf(IncompatibleIndexVersionException.class);

    }

    @Test
    void testEmptyDataDir(@TempDir Path tempDir) {
        final IndexerDirectoryInformation result = parser.parse(tempDir);
        Assertions.assertThat(result).isNotNull();
        Assertions.assertThat(result.nodes()).isEmpty();

    }
}
