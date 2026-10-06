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
package org.graylog.datanode.filesystem.index.statefile;

import org.assertj.core.api.Assertions;
import org.graylog.datanode.filesystem.index.IndexerInformationParserException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;

class Lucene9StateFileParserTest {

    private StateFileParser parser;

    @BeforeEach
    void setUp() {
        this.parser = new Lucene9StateFileParser();
    }

    @Test
    void parseOpensearch1NodeState() throws URISyntaxException {
        final URI uri = getClass().getResource("/indices/opensearch1/nodes/0/_state/node-0.st").toURI();
        final StateFile stateFile = parser.parse(Path.of(uri));
        Assertions.assertThat(stateFile.document().get("node_id")).isEqualTo("i9mNg4aTR9ytY2Dbn5bSMg");
        Assertions.assertThat(stateFile.document().get("node_version")).isEqualTo(135247827);
    }

    @Test
    void parseElasticsearch7NodeState() throws URISyntaxException {
        final URI uri = getClass().getResource("/indices/elasticsearch7/nodes/0/_state/node-0.st").toURI();
        final StateFile stateFile = parser.parse(Path.of(uri));
        Assertions.assertThat(stateFile.document().get("node_id")).isEqualTo("jy62YbbVQdOyvBoKDQb7mg");
        Assertions.assertThat(stateFile.document().get("node_version")).isEqualTo(7100099);
    }

    @Test
    void parseOpensearch2IndexState() throws URISyntaxException {
        final URI uri = getClass().getResource("/indices/opensearch2/nodes/0/indices/ViOS0qZKRb6LKkIos_3GwQ/_state/state-3.st").toURI();
        final StateFile stateFile = parser.parse(Path.of(uri));
        Assertions.assertThat(stateFile.document()).containsOnlyKeys(".plugins-ml-config");
    }

    @Test
    void parseNonExistentFile() {
        Assertions.assertThatThrownBy(() -> parser.parse(Path.of("/nonexistent/path/node-0.st")))
                .isInstanceOf(IndexerInformationParserException.class);
    }

    @Test
    void parseCorruptedFile(@TempDir Path tempDir) throws IOException {
        final Path corrupt = tempDir.resolve("corrupt.st");
        Files.write(corrupt, new byte[64]);
        Assertions.assertThatThrownBy(() -> parser.parse(corrupt))
                .isInstanceOf(IndexerInformationParserException.class);
    }
}
