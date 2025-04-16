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
package org.graylog.datanode.build;

import org.graylog.datanode.opensearch.cli.CliEnv;
import org.graylog.datanode.opensearch.cli.OpensearchCli;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Arrays;

/**
 * Utility for correct removal of opensearch plugins from a maven build. This makes sure that no dependencies are
 * left behind and also removes all configuration directories and files that are not needed anymore.
 */
public class RemoveOpensearchPlugins {

    public static void main(String[] args) throws IOException {

        if (args.length < 2) {
            System.err.println("Syntax: " + RemoveOpensearchPlugins.class.getSimpleName() + " <opendsearchDistDir> <pluginFile>");
            System.exit(1);
        }

        final Path opensearchDist = Path.of(args[0]);
        final Path binDir = opensearchDist.resolve("bin");
        final Path configDir = opensearchDist.resolve("config");

        final CliEnv env = new CliEnv(configDir).javaHome(System.getProperty("java.home"));
        final OpensearchCli cli = new OpensearchCli(env, binDir);

        // TODO: implement batch removal of multiple plugins, if opensearch ever supports that (like ES does)
        Arrays.stream(args)
                .skip(1)
                .forEach(pluginName -> removePlugin(pluginName, cli));
    }

    private static void removePlugin(String plugin, OpensearchCli cli) {
        System.out.println("Removing unused opensearch plugin " + plugin);
        cli.plugin().removePlugin(plugin, true);
    }
}
