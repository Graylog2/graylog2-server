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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;
import java.io.IOException;
import java.nio.file.Path;
import java.util.SortedSet;

@Singleton
public class AllowedAuxiliaryPathChecker {

    private static final Logger LOG = LoggerFactory.getLogger(AllowedAuxiliaryPathChecker.class);

    private final SortedSet<Path> allowedPaths;

    @Inject
    public AllowedAuxiliaryPathChecker(@Named("allowed_auxiliary_paths") SortedSet<Path> allowedPaths) {
        this.allowedPaths = allowedPaths;
    }

    public boolean fileIsInAllowedPath(Path path) {
        if (allowedPaths.isEmpty()) {
            return true;
        }

        final Path realFilePath = resolveRealPath(path);
        if (realFilePath == null) {
            return false;
        }
        for (Path allowedPath : allowedPaths) {
            final Path realAllowedPath = resolveRealPath(allowedPath);
            if (realAllowedPath != null && realFilePath.startsWith(realAllowedPath)) {
                return true;
            }
        }
        return false;
    }

    public static Path resolveRealPath(Path path) {
        try {
            // Get the real path by resolving all relative paths and symbolic links.
            return path.toRealPath();
        } catch (IOException e) {
            LOG.error("Could not resolve real location of path [{}].", path, e);
        }
        return null;
    }
}
