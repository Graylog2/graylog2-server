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
package org.graylog2.configuration;

import com.github.joschi.jadconfig.Parameter;

import java.nio.file.Path;
import java.nio.file.Paths;

public abstract class PathConfiguration {
    protected static final Path DEFAULT_BIN_DIR = Paths.get("bin");
    protected static final Path DEFAULT_DATA_DIR = Paths.get("data");
    protected static final Path DEFAULT_PLUGIN_DIR = Paths.get("plugin");

    @Parameter(value = "bin_dir", required = true)
    private Path binDir = DEFAULT_BIN_DIR;

    @Parameter(value = "data_dir", required = true)
    private Path dataDir = DEFAULT_DATA_DIR;

    @Parameter(value = "plugin_dir", required = true)
    private Path pluginDir = DEFAULT_PLUGIN_DIR;

    public Path getBinDir() {
        return binDir;
    }

    public Path getDataDir() {
        return dataDir;
    }
    public Path getNativeLibDir() {
        return dataDir.resolve("libnative");
    }

    public Path getPluginDir() {
        return pluginDir;
    }

}
