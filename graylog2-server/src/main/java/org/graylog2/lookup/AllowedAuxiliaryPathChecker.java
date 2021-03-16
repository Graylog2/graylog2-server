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
package org.graylog2.lookup;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.SortedSet;

@Singleton
public class AllowedAuxiliaryPathChecker {

    private final SortedSet<Path> allowedPaths;

    @Inject
    public AllowedAuxiliaryPathChecker(@Named("allowed_auxiliary_paths") SortedSet<Path> allowedPaths) {
        this.allowedPaths = allowedPaths;
    }

    public boolean isInAllowedPath(String filePath) {
        if (allowedPaths.isEmpty()) {
            return true;
        }

        // Get the absolute file path (resolve all relative paths and symbolic links).
        // The path.toFile().getCanonicalFile() calls accomplishes this.
        final Path absoluteFilePath = Paths.get(filePath).toAbsolutePath().normalize();
        for (Path allowedPath : allowedPaths) {
            final Path absoluteAllowedPath = allowedPath.toAbsolutePath().normalize();
            if (absoluteFilePath.startsWith(absoluteAllowedPath)) {
                return true;
            }
        }
        return false;
    }
}
