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
package org.graylog.datanode.opensearch.cli;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

/**
 * Wrapper around opensearch-plugin command line tool.
 */
public class OpensearchPluginCli extends AbstractOpensearchCli {

    protected OpensearchPluginCli(Path configDir, Path binDir) {
        super(configDir, binDir, "opensearch-plugin");
    }

    /**
     * @return List of installed plugin names. The name is for example "repository-s3", not a full filename
     */
    public List<String> listPlugins() {
        final String output = runBatch("list");
        return output.lines().map(String::trim).toList();
    }

    /**
     * Install plugin from a downloaded zip file.
     *
     * @param file Path to the plugin file.
     * @return STDOUT of the installation process. In case of error, a RuntimeException will be thrown.
     */
    public String installPlugin(Path file) {
        if (!Files.exists(file) || !Files.isReadable(file)) {
            throw new IllegalArgumentException("File " + file + " does not exist or is not readable");
        }
        return runBatch("install", "--batch", file.toUri().toString());
    }

    /**
     * Remove plugin from existing installation.
     *
     * @param pluginName plain plugin name, e.g. repository-s3.
     * @param purge should also configuration of the plugin be removed?
     * @return STDOUT of the removal process. In case of error, a RuntimeException will be thrown.
     */
    public String removePlugin(String pluginName, boolean purge) {
        if (purge) {
            return runBatch("remove", pluginName, "--purge");
        } else {
            return runBatch("remove", pluginName);
        }
    }
}
