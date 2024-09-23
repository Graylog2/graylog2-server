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
package org.graylog.datanode.rest;

import jakarta.annotation.Nonnull;
import org.assertj.core.api.Assertions;
import org.graylog.plugins.views.storage.migration.state.actions.OpensearchLockCheckResult;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;

class OpensearchLockCheckControllerTest {
    @Test
    void testNotLockedDir(@TempDir Path tempDir) throws IOException {
        final OpensearchLockCheckController controller = new OpensearchLockCheckController(tempDir);
        createLockFile(tempDir);
        final OpensearchLockCheckResult result = controller.checkLockFiles();
        Assertions.assertThat(result.locks())
                .hasSize(1)
                .allSatisfy(l -> Assertions.assertThat(l.locked()).isFalse());
    }

    @Test
    void testLockedDir(@TempDir Path tempDir) throws IOException {
        final OpensearchLockCheckController controller = new OpensearchLockCheckController(tempDir);
        final Path lockFile = createLockFile(tempDir);
        lock(lockFile);
        final OpensearchLockCheckResult result = controller.checkLockFiles();
        Assertions.assertThat(result.locks())
                .hasSize(1)
                .allSatisfy(l -> Assertions.assertThat(l.locked()).isTrue());
    }

    @Test
    void testEmptyDir(@TempDir Path tempDir) {
        final OpensearchLockCheckController controller = new OpensearchLockCheckController(tempDir);
        final OpensearchLockCheckResult result = controller.checkLockFiles();
        Assertions.assertThat(result.locks())
                .isEmpty();
    }

    @Nonnull
    private static Path createLockFile(Path tempDir) throws IOException {
        final Path nodeDir = tempDir.resolve("nodes").resolve("0");
        Files.createDirectories(nodeDir);
        final Path lockFile = nodeDir.resolve("node.lock");
        Files.createFile(lockFile);
        return lockFile;
    }

    private FileLock lock(Path lockFile) throws IOException {
        FileChannel channel = FileChannel.open(lockFile, StandardOpenOption.WRITE);
        return channel.lock();
    }
}
