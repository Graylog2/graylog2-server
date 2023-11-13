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

import org.graylog.datanode.configuration.DatanodeConfiguration;
import org.graylog.datanode.configuration.DatanodeDirectories;
import org.graylog2.bootstrap.preflight.PreflightCheck;
import org.graylog2.bootstrap.preflight.PreflightCheckException;
import org.graylog2.plugin.system.NodeId;

import javax.inject.Inject;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;

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
        if (Files.exists(lockfile)) {
            checkExistingLock(dir, lockfile);
        } else {
            writeLockFile(lockfile);
        }
    }

    private void checkExistingLock(Path dir, Path lockfile) {
        try {
            final String dirLockedFor = Files.readString(lockfile);
            if(!Objects.equals(nodeId, dirLockedFor)) {
                throw new DatanodeLockFileException("Directory " + dir + " locked for datanode " + dirLockedFor + ", access with datanode " + nodeId + " rejected. Please check your configuration and make sure that there is only one datanode instance using this directory.");
            }
        } catch (IOException e) {
            throw new DatanodeLockFileException("Failed to read lockfile " + lockfile, e);
        }
    }

    private void writeLockFile(Path lockfile) {
        try {
            Files.writeString(lockfile, nodeId);
        } catch (IOException e) {
            throw new DatanodeLockFileException("Failed to create datanode lockfile " + lockfile, e);
        }
    }
}
