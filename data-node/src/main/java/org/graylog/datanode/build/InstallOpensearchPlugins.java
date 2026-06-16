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
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

/**
 * Utility for batch installation of opensearch plugins from maven builds.
 */
public class InstallOpensearchPlugins {

    public static final Pattern PLUGIN_NAME_PATTERN = Pattern.compile("^(.*)-\\d+\\.\\d+\\.\\d+\\.zip$");
    private static final Pattern DIST_VERSION_PATTERN = Pattern.compile("^opensearch-(\\d+\\.\\d+\\.\\d+)-.*$");

    public static void main(String[] args) throws IOException {

        if (args.length != 2) {
            System.err.println("Syntax: " + InstallOpensearchPlugins.class.getSimpleName() + " <opensearchParentDir> <pluginsDir>");
            System.exit(1);
        }

        final Path opensearchParentDir = Path.of(args[0]);
        final Path pluginsDir = Path.of(args[1]);

        try (Stream<Path> dirs = Files.list(opensearchParentDir)) {
            dirs.filter(Files::isDirectory)
                    .sorted()
                    .forEach(distDir -> {
                        final Matcher m = DIST_VERSION_PATTERN.matcher(distDir.getFileName().toString());
                        if (!m.matches()) {
                            System.err.println("Skipping unexpected directory: " + distDir.getFileName());
                            return;
                        }
                        try {
                            installPluginsForDist(distDir, pluginsDir, m.group(1));
                        } catch (IOException e) {
                            throw new RuntimeException("Failed to install plugins for " + distDir, e);
                        }
                    });
        }
    }

    private static void installPluginsForDist(Path distDir, Path pluginsDir, String version) throws IOException {
        System.out.println("Installing plugins for " + distDir.getFileName() + " (version " + version + ")");

        final Environment env = new Environment(Map.of())
                .withOpensearchJavaHome(Path.of(System.getProperty("java.home")))
                .withOpensearchPathConf(distDir.resolve("config"));
        final OpensearchCli cli = new OpensearchCli(env, distDir.resolve("bin"));
        final List<String> installedPlugins = cli.plugin().listPlugins();

        try (Stream<Path> files = Files.list(pluginsDir)) {
            files.filter(p -> p.getFileName().toString().endsWith("-" + version + ".zip"))
                    .sorted()
                    .map(InstallOpensearchPlugins::parsePlugin)
                    .filter(plugin -> {
                        final boolean isInstalled = installedPlugins.contains(plugin.name());
                        if (isInstalled) {
                            System.out.println("Plugin " + plugin.name() + " is already installed, skipping installation");
                        }
                        return !isInstalled;
                    })
                    .forEach(plugin -> installPlugin(cli, plugin));
        }
    }

    private static void installPlugin(OpensearchCli cli, Plugin plugin) {
        System.out.println("Installing opensearch plugin " + plugin.path());
        cli.plugin().installPlugin(plugin.path());
    }

    private static Plugin parsePlugin(Path path) {
        if (!Files.exists(path) || !Files.isReadable(path)) {
            throw new RuntimeException("Plugin file not found: " + path);
        }

        final Matcher m = PLUGIN_NAME_PATTERN.matcher(path.getFileName().toString());

        if (m.find()) {
            return new Plugin(path, m.group(1));
        } else {
            throw new RuntimeException("Plugin file name can't be parsed: " + path);
        }
    }

    private record Plugin(Path path, String name) {}
}
