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
import org.graylog2.configuration.converters.PathSetConverter;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.Set;

public class PathConfiguration {
    public static final String TRUSTED_DATA_FILE_PATHS = "trusted_data_file_paths";

    protected static final Path DEFAULT_BIN_DIR = Paths.get("bin");
    protected static final Path DEFAULT_DATA_DIR = Paths.get("data");
    protected static final Path DEFAULT_PLUGIN_DIR = Paths.get("plugin");

    @Parameter(value = "bin_dir", required = true)
    private Path binDir = DEFAULT_BIN_DIR;

    @Parameter(value = "data_dir", required = true)
    private Path dataDir = DEFAULT_DATA_DIR;

    @Parameter(value = "plugin_dir", required = true)
    private Path pluginDir = DEFAULT_PLUGIN_DIR;

    /**
     * Optional trusted paths for Graylog data files.
     *
     * If provided, certain operations in Graylog will only be permitted if the data file(s) are located in the
     * specified paths. All subdirectories of indicated paths are trusted by default.
     *
     * This provides an additional layer of security, and allows administrators to control where in the file system
     * Graylog users can select files from. It protects against the potential inspection of arbitrary files in the
     * file system from the Graylog user interface.
     */
    @Parameter(value = TRUSTED_DATA_FILE_PATHS, converter = PathSetConverter.class)
    private Set<Path> trustedFilePaths = Collections.emptySet();

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

    public Set<Path> getTrustedFilePaths() {
        return trustedFilePaths;
    }
}
