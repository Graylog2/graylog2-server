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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * This check will verify that all pointers to all directories that the datanode and its managed opensearch process
 * are available and configured with correct permissions.
 *
 * Checking this early will prevent some later hard to understand exceptions, for example errors when starting the opensearch
 * process with non-writeable configuration directory.
 */
public class DatanodeDirectoriesCheck implements PreflightCheck {

    private static final Logger LOG = LoggerFactory.getLogger(DatanodeDirectoriesCheck.class);

    private final DatanodeConfiguration datanodeConfiguration;

    @Inject
    public DatanodeDirectoriesCheck(DatanodeConfiguration datanodeConfiguration) {
        this.datanodeConfiguration = datanodeConfiguration;
    }

    @Override
    public void runCheck() throws PreflightCheckException {
        try {
            checkDirectories(datanodeConfiguration.datanodeDirectories());
        } catch (DatanodeDirectoryException e) {
            throw new PreflightCheckException("Failed to check required datanode directories", e);
        }
    }

    private void checkDirectories(DatanodeDirectories directories) throws DatanodeDirectoryException {
        checkReadWriteDir(directories.getDataTargetDir(), "opensearch_data_location");
        checkReadWriteDir(directories.getLogsTargetDir(), "opensearch_logs_location");
        checkReadWriteDir(directories.getOpensearchProcessConfigurationDir(), "opensearch_config_location");
        checkReadDir(directories.getConfigurationSourceDir(), "config_location");
    }

    static void checkReadDir(Path dir, String configPropertyName) throws DatanodeDirectoryException {
        checkOrCreate(dir, configPropertyName);
        checkReadable(dir, configPropertyName);
    }

    static void checkReadWriteDir(Path dir, String configPropertyName) throws DatanodeDirectoryException {
        checkOrCreate(dir, configPropertyName);
        checkReadable(dir, configPropertyName);
        checkWriteable(dir, configPropertyName);
    }

    private static void checkOrCreate(Path dir, String configPropertyName) throws DatanodeDirectoryException {
        if (!Files.exists(dir)) {
            createDirectory(dir, configPropertyName);
        } else if (!Files.isDirectory(dir)) {
            throw new DatanodeDirectoryException("Datanode expects " + dir + " to be a directory, please make sure the configuration property " + configPropertyName + " points to a directory.");
        }
    }

    private static void createDirectory(Path dir, String configPropertyName) throws DatanodeDirectoryException {
        try {
            LOG.info("Datanode directory " + dir + " doesn't existing, creating now.");
            Files.createDirectories(dir);
        } catch (IOException e) {
            throw new DatanodeDirectoryException("Failed to create directory " + dir + ". Please make sure that user running datanode has permissions to create directory there or adapt configuration property " + configPropertyName, e);
        }
    }

    private static void checkReadable(Path dir, String configPropertyName) throws DatanodeDirectoryException {
        if (!Files.isReadable(dir)) {
            throw new DatanodeDirectoryException("Datanode needs READ permissions to the " + dir + " directory. Please configure READ permissions for the user that runs the datanode or change the " + configPropertyName + " configuration");
        }
    }

    private static void checkWriteable(Path dir, String configPropertyName) throws DatanodeDirectoryException {
        if (!Files.isWritable(dir)) {
            throw new DatanodeDirectoryException("Datanode needs WRITE permissions to the " + dir + " directory. Please configure WRITE permissions for the user that runs the datanode or change the " + configPropertyName + " configuration.");
        }
    }
}
