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
package org.graylog.datanode.management.opensearch.cli;

import org.graylog.datanode.configuration.DatanodeDirectories;
import org.graylog.datanode.process.OpensearchConfiguration;
import org.graylog2.shared.utilities.StringUtils;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;

public class OpensearchPluginCli extends AbstractOpensearchCli {

    private final Path pluginsPath;

    OpensearchPluginCli(OpensearchConfiguration config) {
        super(config, "opensearch-plugin");
        this.pluginsPath = config.datanodeDirectories().getOpensearchPluginsDir()
                .orElseThrow(() -> new IllegalStateException("Configuration opensearch_plugins_location not set. Please point this configuration to a directory holding plugins ZIP files."));
    }

    private Path resolveOpensearchPlugin(String pluginName, String version) {
        final String pluginFileName = StringUtils.f("%s-%s.zip", pluginName, version);
        final Path pluginFilePath = pluginsPath.resolve(pluginFileName);
        if (!Files.exists(pluginFilePath)) {
            throw new IllegalStateException("Failed to find " + pluginFileName + " plugin, tried path " + pluginFilePath);
        }

        return pluginFilePath;
    }

    /**
     * Installs plugin by its name, will be downloaded from the internet. This is not a preferred way for distributed
     * releases, as it requires access to the internet. We should rather predownload plugins and use {@link #installFromZip(String, String)}
     */
    public List<String> list() {
        final String output = runBatch("list");
        return Arrays.stream(output.split("\n")).toList();
    }

    /**
     * Install plugin from predownloaded zip file. The exact location of the file is determined by {@link DatanodeDirectories#getOpensearchPluginsDir()}
     * @param pluginName Name without version, e.g. "repository-s3"
     * @param version Semver version of the plugin. Usually it should correspond to the opensearch version, but it may
     *                differ for some plugins.
     */
    public void installFromZip(String pluginName, String version) {
        runBatch("install", "--batch", resolveOpensearchPlugin(pluginName, version).toAbsolutePath().toUri().toString());
    }
}
