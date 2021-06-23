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
package org.graylog.testing.completebackend;

import com.google.common.collect.ImmutableList;
import org.graylog.testing.PropertyLoader;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

public class DefaultPluginJarsProvider implements PluginJarsProvider {
    private static final String PROPERTIES_FILE = "api-it-tests.properties";

    @Override
    public List<Path> getJars() {
        String reposDir = getProjectReposPath().toString();
        String projectVersion = getProjectVersion();

        return ImmutableList.of(
                Paths.get(reposDir, "graylog2-server/graylog-storage-elasticsearch6/target",
                        "graylog-storage-elasticsearch6-" + projectVersion + ".jar"),
                Paths.get(reposDir, "graylog2-server/graylog-storage-elasticsearch7/target",
                        "graylog-storage-elasticsearch7-" + projectVersion + ".jar"),
                Paths.get(reposDir, "graylog-plugin-aws/target",
                        "graylog-plugin-aws-" + projectVersion + ".jar"),
                Paths.get(reposDir, "graylog-plugin-threatintel/target",
                        "graylog-plugin-threatintel-" + projectVersion + ".jar"),
                Paths.get(reposDir, "graylog-plugin-collector/target",
                        "graylog-plugin-collector-" + projectVersion + ".jar")
        );
    }

    protected Path getProjectReposPath() {
        return new File(PropertyLoader.get(PROPERTIES_FILE, "project_repos_dir")).toPath();
    }

    protected String getProjectVersion() {
        return PropertyLoader.get(PROPERTIES_FILE, "project_version");
    }
}
