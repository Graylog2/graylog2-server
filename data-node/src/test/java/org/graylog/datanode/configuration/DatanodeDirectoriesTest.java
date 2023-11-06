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
package org.graylog.datanode.configuration;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.PosixFilePermission;

class DatanodeDirectoriesTest {

    @Test
    void testConfigDirPermissions(@TempDir Path dataDir, @TempDir Path logsDir, @TempDir Path configSourceDir, @TempDir Path configTargetDir) throws IOException {
        final DatanodeDirectories datanodeDirectories = new DatanodeDirectories(dataDir, logsDir, configSourceDir, configTargetDir);
        final Path dir = datanodeDirectories.createOpensearchProcessConfigurationDir();
        Assertions.assertThat(Files.getPosixFilePermissions(dir)).
                contains(
                        PosixFilePermission.OWNER_EXECUTE,
                        PosixFilePermission.OWNER_WRITE,
                        PosixFilePermission.OWNER_READ
                );

        final Path keyFile = datanodeDirectories.createOpensearchProcessConfigurationFile(Path.of("my-secret-file.key"));
        Assertions.assertThat(Files.getPosixFilePermissions(keyFile)).
                contains(
                        PosixFilePermission.OWNER_WRITE,
                        PosixFilePermission.OWNER_READ
                );
    }
}
