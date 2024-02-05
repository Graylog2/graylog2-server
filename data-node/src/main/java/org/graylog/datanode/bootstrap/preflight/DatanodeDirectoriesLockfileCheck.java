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

import com.google.common.io.CharStreams;
import org.graylog.datanode.configuration.DatanodeConfiguration;
import org.graylog.datanode.configuration.DatanodeDirectories;
import org.graylog2.bootstrap.preflight.PreflightCheck;
import org.graylog2.bootstrap.preflight.PreflightCheckException;
import org.graylog2.plugin.system.NodeId;

import jakarta.inject.Inject;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.channels.Channels;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Objects;
import java.util.Optional;

/**
 * To prevent two or more datanodes using the same directories, we are writing a datanode.lock files into the dirs.
 * The content of the lock file is the nodeid of the datanode. During startup we are checking that the directory
 * has either no lock (and then we create one) or if it has a lock, then we verify its content against current nodeid.
 */
public class DatanodeDirectoriesLockfileCheck implements PreflightCheck {

    protected static final Path DATANODE_LOCKFILE = Path.of("datanode.lock");
    private final DatanodeDirectories directories;
    private final String nodeId;

    @Inject
    public DatanodeDirectoriesLockfileCheck(NodeId nodeId, DatanodeConfiguration datanodeConfiguration) {
        this(nodeId.getNodeId(), datanodeConfiguration.datanodeDirectories());
    }

    public DatanodeDirectoriesLockfileCheck(String nodeId, DatanodeDirectories directories) {
        this.directories = directories;
        this.nodeId = nodeId;
    }

    @Override
    public void runCheck() throws PreflightCheckException {
        checkDatanodeLock(directories.getConfigurationTargetDir());
        checkDatanodeLock(directories.getDataTargetDir());
        checkDatanodeLock(directories.getLogsTargetDir());
    }

    private void checkDatanodeLock(Path dir) {
        final Path lockfile = dir.resolve(DATANODE_LOCKFILE);
        try (FileChannel channel = FileChannel.open(lockfile, StandardOpenOption.READ, StandardOpenOption.CREATE, StandardOpenOption.WRITE)) {
            doCheckLockFile(channel, dir);
        } catch (IOException e) {
            throw new DatanodeLockFileException("Failed to open channel to lock file " + lockfile.toAbsolutePath(), e);
        }
    }

    private void doCheckLockFile(FileChannel channel, Path dir) {
        FileLock lock = null;
        try {
            lock = channel.lock();
            // now we are the only process that can access the lock file.
            verifyOrCreateLockFile(channel, dir);
        } catch (IOException e) {
            throw new DatanodeLockFileException("Failed to obtain lock", e);
        } finally {
            releaseLock(lock);
        }
    }

    private void verifyOrCreateLockFile(FileChannel channel, Path dir) {
        readChannel(channel).ifPresentOrElse(
                lockedForID -> verifyLockFileContent(lockedForID, dir),
                () -> writeLockFile(channel)
        );
    }

    private void releaseLock(FileLock lock) {
        try {
            if (lock != null) {
                lock.release();
            }
        } catch (IOException e) {
            throw new DatanodeLockFileException("Failed to release lock file", e);
        }
    }

    private void verifyLockFileContent(String lockedForID, Path dir) {
        if (!Objects.equals(lockedForID, nodeId)) {
            throw new DatanodeLockFileException("Directory " + dir + " locked for datanode " + lockedForID + ", access with datanode " + nodeId + " rejected. Please check your configuration and make sure that there is only one datanode instance using this directory.");
        }
    }

    private void writeLockFile(FileChannel channel) {
        try {
            // do not close the writer, otherwise it will close the underlying channel. We do that explicitly elsewhere.
            final Writer writer = Channels.newWriter(channel, StandardCharsets.UTF_8);
            writer.write(nodeId);
            writer.flush();
        } catch (IOException e) {
            throw new DatanodeLockFileException("Failed to write node ID to the lock file", e);
        }
    }

    private Optional<String> readChannel(FileChannel channel) {
        try {
            // do not close the reader, otherwise it will close the underlying channel. We do that explicitly elsewhere.
            final Reader reader = Channels.newReader(channel, StandardCharsets.UTF_8);
            return Optional.of(CharStreams.toString(reader)).filter(v -> !v.isBlank()).map(String::trim);
        } catch (IOException e) {
            throw new DatanodeLockFileException("Failed to read content of lock file", e);
        }
    }
}
