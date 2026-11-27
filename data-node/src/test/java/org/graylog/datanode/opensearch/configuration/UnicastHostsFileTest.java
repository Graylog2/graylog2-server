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
package org.graylog.datanode.opensearch.configuration;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.PosixFilePermission;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class UnicastHostsFileTest {

    @Test
    void testCreatesFile(@TempDir Path tempDir) throws IOException {
        assertThat(UnicastHostsFile.update(tempDir, Set.of("node2:9300", "node1:9300"))).isTrue();

        final Path file = tempDir.resolve(UnicastHostsFile.FILENAME);
        assertThat(file).hasContent("node1:9300\nnode2:9300");
        assertThat(Files.getPosixFilePermissions(file)).containsOnly(PosixFilePermission.OWNER_READ, PosixFilePermission.OWNER_WRITE);
        assertThat(tempDir).isDirectoryNotContaining("glob:**.tmp");
    }

    @Test
    void testSkipsUnchangedContent(@TempDir Path tempDir) throws IOException {
        final Path file = tempDir.resolve(UnicastHostsFile.FILENAME);
        Files.writeString(file, "node1:9300\nnode2:9300", StandardCharsets.UTF_8);
        final long modified = Files.getLastModifiedTime(file).toMillis();

        assertThat(UnicastHostsFile.update(tempDir, List.of("node2:9300", "node1:9300"))).isFalse();
        assertThat(Files.getLastModifiedTime(file).toMillis()).isEqualTo(modified);
    }

    @Test
    void testReplacesChangedContent(@TempDir Path tempDir) throws IOException {
        final Path file = tempDir.resolve(UnicastHostsFile.FILENAME);
        Files.writeString(file, "", StandardCharsets.UTF_8);

        assertThat(UnicastHostsFile.update(tempDir, Set.of("node1:9300", "node2:9300"))).isTrue();
        assertThat(file).hasContent("node1:9300\nnode2:9300");

        assertThat(UnicastHostsFile.update(tempDir, Set.of("node1:9300"))).isTrue();
        assertThat(file).hasContent("node1:9300");
        assertThat(tempDir).isDirectoryNotContaining("glob:**.tmp");
    }

    @Test
    void testConfigFileMatchesUpdatedContent() {
        final Set<String> seedHosts = Set.of("node2:9300", "node1:9300");
        assertThat(UnicastHostsFile.configFile(seedHosts).relativePath()).isEqualTo(UnicastHostsFile.FILENAME);
        assertThat(UnicastHostsFile.configFile(seedHosts).text()).isEqualTo("node1:9300\nnode2:9300");
    }
}
