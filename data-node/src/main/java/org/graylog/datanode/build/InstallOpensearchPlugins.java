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
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Utility for batch installation of opensearch plugins from maven builds.
 */
public class InstallOpensearchPlugins {

    public static final Pattern PLUGIN_NAME_PATTERN = Pattern.compile("^(.*)-\\d+\\.\\d+\\.\\d+\\.zip$");

    public static void main(String[] args) throws IOException {

        if (args.length < 2) {
            System.err.println("Syntax: " + InstallOpensearchPlugins.class.getSimpleName() + " <opensearchDistDir> <pluginFile1> ... <pluginFileN>");
            System.exit(1);
        }

        final Path opensearchDist = Path.of(args[0]);
        final Path binDir = opensearchDist.resolve("bin");
        final Path configDir = opensearchDist.resolve("config");

        final CliEnv env = new CliEnv(configDir).javaHome(System.getProperty("java.home"));
        final OpensearchCli cli = new OpensearchCli(env, binDir);

        final List<String> installedPlugins = cli.plugin().listPlugins();

        Arrays.stream(args).skip(1)
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

    private static void installPlugin(OpensearchCli cli, Plugin plugin) {
        System.out.println("Installing opensearch plugin " + plugin.path());
        cli.plugin().installPlugin(plugin.path());
    }

    private static Plugin parsePlugin(String pluginPath) {

        final Path path = Path.of(pluginPath);

        if (!Files.exists(path) || !Files.isReadable(path)) {
            throw new RuntimeException("Plugin file not found: " + pluginPath);
        }

        Matcher m = PLUGIN_NAME_PATTERN.matcher(path.getFileName().toString());

        if (m.find()) {
            String pluginName = m.group(1);
            return new Plugin(path, pluginName);
        } else {
            throw new RuntimeException("Plugin file name can't be parsed: " + pluginPath);
        }
    }

    private record Plugin(Path path, String name) {}
}
