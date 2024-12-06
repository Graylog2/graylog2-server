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
package org.graylog.datanode.configuration;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public record OpensearchConfigurationDir(java.nio.file.Path configurationRoot) {
    public Path createOpensearchProcessConfigurationFile(Path relativePath) throws IOException {
        if (relativePath.isAbsolute()) {
            throw new IllegalArgumentException("Only relative paths supported here!" + relativePath);
        }

        final Path resolvedPath = configurationRoot.resolve(relativePath);

        // recursively create all parent directories
        Files.createDirectories(resolvedPath.getParent(), DatanodeDirectories.DIRECTORY_PERMISSIONS);
        return DatanodeDirectories.createRestrictedAccessFile(resolvedPath);
    }
}
