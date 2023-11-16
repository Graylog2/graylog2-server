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
package org.graylog.datanode.bootstrap.preflight;

import org.assertj.core.api.Assertions;
import org.graylog.datanode.configuration.DatanodeDirectories;
import org.graylog2.bootstrap.preflight.PreflightCheck;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

class DatanodeDirectoriesLockfileCheckTest {

    @Test
    void testLockCreation(@TempDir Path dataDir,
                          @TempDir Path logsDir,
                          @TempDir Path configDir) throws IOException {

        final Path dataDirLock = dataDir.resolve(DatanodeDirectoriesLockfileCheck.DATANODE_LOCKFILE);
        final Path logsDirLock = logsDir.resolve(DatanodeDirectoriesLockfileCheck.DATANODE_LOCKFILE);
        final Path configDirLock = configDir.resolve(DatanodeDirectoriesLockfileCheck.DATANODE_LOCKFILE);

        final PreflightCheck check = new DatanodeDirectoriesLockfileCheck("5ca1ab1e-0000-4000-a000-000000000000", new DatanodeDirectories(dataDir, logsDir, null, configDir));
        check.runCheck();

        Assertions.assertThat(Files.readString(dataDirLock)).isEqualTo("5ca1ab1e-0000-4000-a000-000000000000");
        Assertions.assertThat(Files.readString(logsDirLock)).isEqualTo("5ca1ab1e-0000-4000-a000-000000000000");
        Assertions.assertThat(Files.readString(configDirLock)).isEqualTo("5ca1ab1e-0000-4000-a000-000000000000");
    }


    @Test
    void testValidExistingLock(@TempDir Path dataDir,
                          @TempDir Path logsDir,
                          @TempDir Path configDir) throws IOException {

        final Path dataDirLock = dataDir.resolve(DatanodeDirectoriesLockfileCheck.DATANODE_LOCKFILE);
        final Path logsDirLock = logsDir.resolve(DatanodeDirectoriesLockfileCheck.DATANODE_LOCKFILE);
        final Path configDirLock = configDir.resolve(DatanodeDirectoriesLockfileCheck.DATANODE_LOCKFILE);

        Files.writeString(dataDirLock, "5ca1ab1e-0000-4000-a000-000000000000");
        Files.writeString(logsDirLock, "5ca1ab1e-0000-4000-a000-000000000000");
        Files.writeString(configDirLock, "5ca1ab1e-0000-4000-a000-000000000000");

        final PreflightCheck check = new DatanodeDirectoriesLockfileCheck("5ca1ab1e-0000-4000-a000-000000000000", new DatanodeDirectories(dataDir, logsDir, null, configDir));
        check.runCheck();

        Assertions.assertThat(Files.readString(dataDirLock)).isEqualTo("5ca1ab1e-0000-4000-a000-000000000000");
        Assertions.assertThat(Files.readString(logsDirLock)).isEqualTo("5ca1ab1e-0000-4000-a000-000000000000");
        Assertions.assertThat(Files.readString(configDirLock)).isEqualTo("5ca1ab1e-0000-4000-a000-000000000000");
    }

    @Test
    void testInvalidExistingLock(@TempDir Path dataDir,
                               @TempDir Path logsDir,
                               @TempDir Path configDir) throws IOException {

        final Path dataDirLock = dataDir.resolve(DatanodeDirectoriesLockfileCheck.DATANODE_LOCKFILE);
        final Path logsDirLock = logsDir.resolve(DatanodeDirectoriesLockfileCheck.DATANODE_LOCKFILE);
        final Path configDirLock = configDir.resolve(DatanodeDirectoriesLockfileCheck.DATANODE_LOCKFILE);

        Files.writeString(dataDirLock, "5ca1ab1e-0000-4000-a000-000000000001");
        Files.writeString(logsDirLock, "5ca1ab1e-0000-4000-a000-000000000001");
        Files.writeString(configDirLock, "5ca1ab1e-0000-4000-a000-000000000001");

        final PreflightCheck check = new DatanodeDirectoriesLockfileCheck("5ca1ab1e-0000-4000-a000-000000000000", new DatanodeDirectories(dataDir, logsDir, null, configDir));

        Assertions.assertThatThrownBy(check::runCheck)
                .isInstanceOf(DatanodeLockFileException.class)
                .hasMessageContaining("locked for datanode 5ca1ab1e-0000-4000-a000-000000000001, access with datanode 5ca1ab1e-0000-4000-a000-000000000000 rejected");
    }
}
