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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Path;

class StateFileParserTest {

    private StateFileParser parser;

    @BeforeEach
    void setUp() {
        this.parser = new StateFileParserImpl();
    }

    @Test
    void parseOpensearch2() throws URISyntaxException {
        final URI uri = getClass().getResource("/indices/opensearch2/nodes/0/_state/node-1.st").toURI();
        final StateFile stateFile = parser.parse(Path.of(uri));
        Assertions.assertThat(stateFile.document().get("node_id")).isEqualTo("yK5GvmLyRD2nbhAyyJL76w");
        Assertions.assertThat(stateFile.document().get("node_version")).isEqualTo(136317827);
    }

    @Test
    void parseElasticsearch7() throws URISyntaxException {
        final URI uri = getClass().getResource("/indices/elasticsearch7/nodes/0/_state/node-0.st").toURI();
        final StateFile stateFile = parser.parse(Path.of(uri));
        Assertions.assertThat(stateFile.document().get("node_id")).isEqualTo("jy62YbbVQdOyvBoKDQb7mg");
        Assertions.assertThat(stateFile.document().get("node_version")).isEqualTo(7100099);
    }
}
