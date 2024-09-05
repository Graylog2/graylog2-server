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

import jakarta.annotation.Nonnull;
import org.assertj.core.api.Assertions;
import org.graylog2.bootstrap.preflight.PreflightCheckException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.Duration;

class OpensearchNodeLockFileCheckTest {

    @Test
    void testWithLockedFile(@TempDir Path tempDir) throws IOException {
        final Path lockFile = createLockFile(tempDir);
        FileLock lock = null;
        try  {
            lock = lock(lockFile);
            final OpensearchNodeLockFileCheck check = new OpensearchNodeLockFileCheck(
                    tempDir,
                    Duration.ofMillis(100),
                    Duration.ofMillis(200)
            );
            Assertions.assertThatThrownBy(check::runCheck)
                    .isInstanceOf(PreflightCheckException.class)
                    .hasMessageContaining("Data directory still locked and can't be accessed");

        } finally {
            if(lock != null) {
                lock.release();
            }
        }
    }

    @Test
    void testUnlockedFile(@TempDir Path tempDir) throws IOException {
        createLockFile(tempDir);

        final OpensearchNodeLockFileCheck check = new OpensearchNodeLockFileCheck(
                tempDir,
                Duration.ofMillis(100),
                Duration.ofMillis(200)
        );
        Assertions.assertThatNoException().isThrownBy(check::runCheck);
    }

    @Test
    void testEmptyDataDir(@TempDir Path tempDir) {
        final OpensearchNodeLockFileCheck check = new OpensearchNodeLockFileCheck(
                tempDir,
                Duration.ofMillis(100),
                Duration.ofMillis(200)
        );
        Assertions.assertThatNoException().isThrownBy(check::runCheck);
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
