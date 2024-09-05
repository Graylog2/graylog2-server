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

import com.github.rholder.retry.Attempt;
import com.github.rholder.retry.RetryException;
import com.github.rholder.retry.RetryListener;
import com.github.rholder.retry.RetryerBuilder;
import com.github.rholder.retry.StopStrategies;
import com.github.rholder.retry.WaitStrategies;
import jakarta.annotation.Nonnull;
import jakarta.inject.Inject;
import org.graylog.datanode.configuration.DatanodeConfiguration;
import org.graylog2.bootstrap.preflight.PreflightCheck;
import org.graylog2.bootstrap.preflight.PreflightCheckException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.nio.channels.NonWritableChannelException;
import java.nio.channels.OverlappingFileLockException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.Duration;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

/**
 * When opensearch process operates on a data directory, it locks the node.lock file in top directory of the node,
 * e.g. /var/lib/opensearch/data/nodes/0/node.lock
 *
 * We can check if this file is not locked and only then start. This check itself will wait predefined time
 * and will try repeatedly to obtain (and immediately release) a lock. This should prevent any possible
 * later collisions between an existing opensearch managing the data dir and datanode starting a different
 * process managing the same data dir. This would fail anyway as the opensearch process inside the datanode
 * does this check as well, but for us, it's not easy to detect such situation and handle it nicely. With
 * explicit check, we can wait till the original opensearch process stops (for example during in-place migration)
 */
public class OpensearchNodeLockFileCheck implements PreflightCheck {

    private static final Logger LOG = LoggerFactory.getLogger(OpensearchDataDirCompatibilityCheck.class);

    private final Path dataTargetDir;
    private final Duration sleepInterval;
    private final Duration stopInterval;

    @Inject
    public OpensearchNodeLockFileCheck(DatanodeConfiguration datanodeConfiguration) {
        this(datanodeConfiguration.datanodeDirectories().getDataTargetDir(),
                Duration.ofSeconds(2),
                Duration.ofMinutes(30)
        );
    }

    public OpensearchNodeLockFileCheck(Path dataTargetDir, Duration sleepInterval, Duration lockObtainOverallTimeout) {
        this.dataTargetDir = dataTargetDir;
        this.sleepInterval = sleepInterval;
        this.stopInterval = lockObtainOverallTimeout;
    }

    @Override
    public void runCheck() throws PreflightCheckException {
        final Path nodesDir = dataTargetDir.resolve("nodes");
        if (Files.isDirectory(nodesDir)) {
            try (final Stream<Path> nodes = Files.list(nodesDir)) {
                nodes.forEach(this::waitForFreeLockFile);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }

    private void waitForFreeLockFile(Path nodeDir) {
        try {
            RetryerBuilder.newBuilder()
                    .retryIfResult(Boolean.TRUE::equals)
                    .withWaitStrategy(WaitStrategies.fixedWait(sleepInterval.toMillis(), TimeUnit.MILLISECONDS))
                    .withStopStrategy(StopStrategies.stopAfterDelay(stopInterval.toMillis(), TimeUnit.MILLISECONDS))
                    .withRetryListener(getRetryListener())
                    .build()
                    .call(() -> isDirLocked(nodeDir));
        } catch (ExecutionException | RetryException e) {
            throw new PreflightCheckException("Data directory still locked and can't be accessed, giving up.", e);
        }
    }

    @Nonnull
    private static RetryListener getRetryListener() {
        return new RetryListener() {
            @Override
            public <V> void onRetry(Attempt<V> attempt) {
                LOG.warn("Waiting for release of data directory locks. Another opensearch process is still using this directory. Retry #" + attempt.getAttemptNumber());
            }
        };
    }

    private static boolean isDirLocked(Path nodeDir) {
        final Path lockFile = nodeDir.resolve("node.lock");
        if (Files.exists(lockFile)) {
            try (FileChannel channel = FileChannel.open(lockFile, StandardOpenOption.WRITE)) {
                final FileLock fileLock = channel.tryLock();
                if (fileLock != null) { // file was not locked, we are good to go, let's release immediately
                    fileLock.release();
                    return false;
                } else {
                    return true;
                }
            } catch (OverlappingFileLockException e) {
                return true;
            } catch (NonWritableChannelException | IOException e) {
                throw new PreflightCheckException("Failed to verify free node.lock file", e);
            }
        }
        return false;
    }
}
