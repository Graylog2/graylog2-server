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

import org.graylog.datanode.Configuration;
import org.graylog2.plugin.system.NodeId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Path;

/**
 * This is a collection of pointers to directories used to store data, logs and configuration of the managed opensearch.
 * Each data type is additionally stored in a subdirectory named after the nodeId, to avoid unexpected collisions when
 * running more datanode instances in the same machine.
 */
public class DatanodeDirectories {

    private static final Logger LOG = LoggerFactory.getLogger(DatanodeDirectories.class);

    private final String nodeId;
    private final String dataTargetDir;
    private final String logsTargetDir;
    private final String configurationSourceDir;
    private final String configurationTargetDir;

    public DatanodeDirectories(String nodeName, String dataTargetDir, String logsTargetDir, String configurationSourceDir, String configurationTargetDir) {
        this.nodeId = nodeName;
        this.dataTargetDir = dataTargetDir;
        this.logsTargetDir = logsTargetDir;
        this.configurationSourceDir = configurationSourceDir;
        this.configurationTargetDir = configurationTargetDir;
    }

    public static DatanodeDirectories fromConfiguration(Configuration configuration, NodeId nodeId) {
        final DatanodeDirectories directories = new DatanodeDirectories(
                nodeId.getNodeId(),
                configuration.getOpensearchDataLocation(),
                configuration.getOpensearchLogsLocation(),
                configuration.getDatanodeConfigurationLocation(),
                configuration.getOpensearchConfigLocation()
        );

        LOG.info("Opensearch of the node {} uses following directories as its storage: {}", nodeId.getNodeId(), directories);
        return directories;
    }

    /**
     * This directory is used by the managed opensearch to store its data in it.
     * Read-write permissions required.
     */
    public Path getDataTargetDir() {
        return resolvePath(dataTargetDir);
    }

    /**
     * This directory is used by the managed opensearch to store its logs in it.
     * Read-write permissions required.
     */
    public Path getLogsTargetDir() {
        return resolvePath(logsTargetDir);
    }


    /**
     * This directory is provided by system admin to the datanode. We read our configuration from this location,
     * we read certificates from here. We'll never write anything to it.
     * Read-only permissions required.
     */
    public Path getConfigurationSourceDir() {
        return Path.of(configurationSourceDir).toAbsolutePath();
    }

    /**
     * This directory is used by us to store all runtime-generated configuration of datanode. This
     * could be truststores, private keys, certificates and other generated config files.
     *
     * We also synchronize and generate opensearch configuration into a subdir of this dir, see {@link #getOpensearchProcessConfigurationLocation()}
     * Read-write permissions required.
     */
    public Path getConfigurationTargetDir() {
        return resolvePath(configurationTargetDir);
    }

    /**
     * This is a subdirectory of {@link #getConfigurationTargetDir()}. It's used by us to synchronize and generate opensearch
     * configuration. Opensearch is then instructed to accept this dir as its base configuration dir (OPENSEARCH_PATH_CONF env property).
     * @see org.graylog.datanode.bootstrap.preflight.OpensearchConfigSync
     */
    public Path getOpensearchProcessConfigurationLocation() {
        return resolvePath(configurationTargetDir).resolve("opensearch");
    }

    private Path resolvePath(String path) {
        return Path.of(path).resolve(nodeId).toAbsolutePath();
    }

    @Override
    public String toString() {
        return "DatanodeDirectories{" +
                "dataTargetDir='" + getDataTargetDir() + '\'' +
                ", logsTargetDir='" + getLogsTargetDir() + '\'' +
                ", configurationSourceDir='" + getConfigurationSourceDir() + '\'' +
                ", configurationTargetDir='" + configurationTargetDir + '\'' +
                '}';
    }
}
