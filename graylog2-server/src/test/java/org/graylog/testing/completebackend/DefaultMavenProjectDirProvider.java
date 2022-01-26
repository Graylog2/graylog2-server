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

import org.graylog.testing.PropertyLoader;

import java.io.File;
import java.nio.file.Path;

public class DefaultMavenProjectDirProvider implements MavenProjectDirProvider {
    @Override
    public Path getProjectDir() {
        return getProjectReposDir().resolve("graylog2-server");
    }

    @Override
    public Path getBinDir() {
        return getProjectReposDir().resolve("graylog-plugin-enterprise/enterprise/bin");
    }

    @Override
    public String getUniqueId() {
        return "default";
    }

    protected Path getProjectReposDir() {
        return new File(PropertyLoader.get("api-it-tests.properties", "project_repos_dir")).toPath().normalize();
    }
}
