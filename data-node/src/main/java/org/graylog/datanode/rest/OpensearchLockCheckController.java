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

import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import org.graylog.datanode.configuration.DatanodeConfiguration;
import org.graylog.plugins.views.storage.migration.state.actions.OpensearchLockCheckResult;
import org.graylog.plugins.views.storage.migration.state.actions.OpensearchNodeLock;
import org.graylog2.bootstrap.preflight.PreflightCheckException;

import java.io.IOException;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.nio.channels.NonWritableChannelException;
import java.nio.channels.OverlappingFileLockException;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;
import java.util.Collections;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Path("/lock-check")
@Produces(MediaType.APPLICATION_JSON)
public class OpensearchLockCheckController {

    private final java.nio.file.Path dataTargetDir;

    @Inject
    public OpensearchLockCheckController(DatanodeConfiguration datanodeConfiguration) {
        this(datanodeConfiguration.datanodeDirectories().getDataTargetDir());
    }

    public OpensearchLockCheckController(java.nio.file.Path dataTargetDir) {
        this.dataTargetDir = dataTargetDir;
    }

    @GET
    public OpensearchLockCheckResult checkLockFiles() {
        final java.nio.file.Path nodesDir = dataTargetDir.resolve("nodes");
        if (Files.isDirectory(nodesDir)) {
            try (final Stream<java.nio.file.Path> nodes = Files.list(nodesDir)) {
                return nodes.map(n -> new OpensearchNodeLock(n, isDirLocked(n)))
                        .collect(Collectors.collectingAndThen(Collectors.toList(), OpensearchLockCheckResult::new));
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        } else {
            return new OpensearchLockCheckResult(Collections.emptyList());
        }
    }

    private static boolean isDirLocked(java.nio.file.Path nodeDir) {
        final java.nio.file.Path lockFile = nodeDir.resolve("node.lock");
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
