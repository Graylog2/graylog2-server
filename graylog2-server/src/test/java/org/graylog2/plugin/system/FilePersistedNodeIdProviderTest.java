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
package org.graylog2.plugin.system;

import org.apache.commons.io.FileUtils;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

class FilePersistedNodeIdProviderTest {

    public static final String NODE_ID_FILENAME = "node.id";

    @TempDir
    static Path tempDir;

    @Test
    void testNonexistentFile() throws IOException {

        final Path nodeIdPath = tempDir.resolve(NODE_ID_FILENAME);

        final String filename = nodeIdPath.toAbsolutePath().toString();
        final FilePersistedNodeIdProvider provider = new FilePersistedNodeIdProvider(filename);

        // first let the logic generate and persist a new ID
        final NodeId nodeId = provider.get();
        final String generatedNodeId = nodeId.getNodeId();
        Assertions.assertThat(generatedNodeId).isNotBlank();

        // verify that content of the file is the same as the returned ID
        Assertions.assertThat(Files.readString(Path.of(filename))).isEqualTo(generatedNodeId);

        // now let's start again, but with a file that already contains an ID
        final FilePersistedNodeIdProvider anotherProvider = new FilePersistedNodeIdProvider(filename);
        Assertions.assertThat(anotherProvider.get().getNodeId()).isEqualTo(generatedNodeId);
    }

    @Test
    void testEmptyFile() throws IOException {

        final Path nodeIdPath = tempDir.resolve(NODE_ID_FILENAME);

        // create the file, write empty string
        FileUtils.writeStringToFile(nodeIdPath.toFile(), "", StandardCharsets.UTF_8);

        final String filename = nodeIdPath.toAbsolutePath().toString();
        final FilePersistedNodeIdProvider provider = new FilePersistedNodeIdProvider(filename);

        // first let the logic generate and persist a new ID
        final NodeId nodeId = provider.get();
        final String generatedNodeId = nodeId.getNodeId();
        Assertions.assertThat(generatedNodeId).isNotBlank();
    }

    @Test
    void testReadOnlyFile() {
        final boolean readOnly = tempDir.toFile().setReadOnly();
        Assertions.assertThat(readOnly).isTrue();
        final String filename = tempDir.toAbsolutePath().toString();
        final FilePersistedNodeIdProvider anotherProvider = new FilePersistedNodeIdProvider(filename);
        Assertions.assertThatThrownBy(anotherProvider::get)
                .isInstanceOf(NodeIdPersistenceException.class)
                .hasMessageContaining("Could not read or generate node ID");
    }
}
