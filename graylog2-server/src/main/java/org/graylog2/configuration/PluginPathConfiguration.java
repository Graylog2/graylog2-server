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

public class PluginPathConfiguration {

    protected static final Path DEFAULT_PLUGIN_DIR = Paths.get("plugin");

    @Parameter(value = "plugin_dir", required = true)
    private final Path pluginDir = DEFAULT_PLUGIN_DIR;

    public Path getPluginDir() {
        return pluginDir;
    }

}
