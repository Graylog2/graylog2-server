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
