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

import org.graylog.datanode.opensearch.cli.OpensearchCli;
import org.graylog.datanode.process.Environment;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

/**
 * Utility for correct removal of opensearch plugins from a maven build. This makes sure that no dependencies are
 * left behind and also removes all configuration directories and files that are not needed anymore.
 */
public class RemoveOpensearchPlugins {

    public static void main(String[] args) throws IOException {

        if (args.length < 2) {
            System.err.println("Syntax: " + RemoveOpensearchPlugins.class.getSimpleName() + " <opensearchParentDir> <plugin1> ... <pluginN>");
            System.exit(1);
        }

        final Path opensearchParentDir = Path.of(args[0]);
        final List<String> pluginsToRemove = Arrays.stream(args).skip(1).toList();

        try (Stream<Path> dirs = Files.list(opensearchParentDir)) {
            dirs.filter(Files::isDirectory)
                    .sorted()
                    .forEach(distDir -> removePluginsFromDist(distDir, pluginsToRemove));
        }
    }

    private static void removePluginsFromDist(Path distDir, List<String> pluginsToRemove) {
        System.out.println("Removing unused plugins of  " + distDir.getFileName());
        final Environment env = new Environment(Map.of())
                .withOpensearchJavaHome(Path.of(System.getProperty("java.home")))
                .withOpensearchPathConf(distDir.resolve("config"));
        final OpensearchCli cli = new OpensearchCli(env, distDir.resolve("bin"));
        // Caution: plugin order is relevant — plugins may depend on other plugins and must be removed in order.
        pluginsToRemove.forEach(plugin -> removePlugin(plugin, cli));
    }

    private static void removePlugin(String plugin, OpensearchCli cli) {
        System.out.println("Removing unused opensearch plugin " + plugin);
        cli.plugin().removePlugin(plugin, true);
    }
}
