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

import jakarta.annotation.Nonnull;
import org.graylog.datanode.opensearch.configuration.OpensearchConfiguration;

import java.nio.file.Path;

/**
 * Collection of opensearch CLI tools. All of them need to have OPENSEARCH_PATH_CONF preconfigured, so they operate
 * on the correct version of configuration.
 */
public class OpensearchCli {

    private final OpensearchKeystoreCli keystore;
    private final OpensearchPluginCli plugin;

    public OpensearchCli(OpensearchConfiguration config) {
        this(createEnv(config), getBinDir(config));
    }

    public OpensearchCli(CliEnv env, Path binDir) {
        this.keystore = new OpensearchKeystoreCli(binDir, env);
        this.plugin = new OpensearchPluginCli(binDir, env);
    }

    @Nonnull
    private static Path getBinDir(OpensearchConfiguration config) {
        return config.getOpensearchDistribution().getOpensearchBinDirPath();
    }

    @Nonnull
    private static CliEnv createEnv(OpensearchConfiguration config) {
        return new CliEnv(config.getOpensearchConfigurationDir().configurationRoot());
    }

    public OpensearchKeystoreCli keystore() {
        return keystore;
    }

    public OpensearchPluginCli plugin() {
        return plugin;
    }
}
