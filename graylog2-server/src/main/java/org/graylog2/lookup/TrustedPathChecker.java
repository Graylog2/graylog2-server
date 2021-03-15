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
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Set;

@Singleton
public class TrustedPathChecker {

    private final Set<Path> trustedPaths;

    @Inject
    public TrustedPathChecker(@Named("trusted_data_file_paths") Set<Path> trustedPaths) {
        this.trustedPaths = trustedPaths;
    }

    public boolean fileIsInTrustedPath(String filePath) throws IOException {
        if (trustedPaths.isEmpty()) {
            return true;
        }

        // Get the absolute file path (resolve all relative paths and symbolic links).
        // The path.toFile().getCanonicalFile() calls accomplishes this.
        final Path absoluteCsvFilePath = Paths.get(filePath).toAbsolutePath().normalize();
        for (Path trustedPath : trustedPaths) {
            final Path absoluteTrustedPath = trustedPath.toAbsolutePath().normalize();
            if (absoluteCsvFilePath.startsWith(absoluteTrustedPath)) {
                return true;
            }
        }
        return false;
    }
}
