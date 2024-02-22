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

import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;

public class OpensearchPluginCli extends AbstractOpensearchCli {

    public OpensearchPluginCli(Path configPath, Path binPath) {
        super(configPath, binPath.resolve("opensearch-plugin"));
    }

    public List<String> list() {
        final String output = run("list");
        return Arrays.stream(output.split("\n")).toList();
    }

    public void install(String pluginName) {
        // the --batch argument will skip the permission warning confirmation if any occurs
        run("install", "--batch", pluginName);
    }
}
