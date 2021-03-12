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
package org.graylog2.lookup.adapters;

import com.google.common.base.Preconditions;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;

@Singleton
public class CSVPermittedPathChecker {

    private final Path permittedDir;

    @Inject
    public CSVPermittedPathChecker(@Named("csv_file_lookup_dir") Path permittedDir) {
        this.permittedDir = permittedDir;
    }

    /**
     * Checks if CSV file is in permitted location is in permitted path.
     *
     * @param csvFilePath the absolute path of the CSV file. No relative paths allowed.
     * @return true if the script is in the permitted location, false if it is not
     */
    boolean checkPath(String csvFilePath) throws IOException {
        Preconditions.checkNotNull(csvFilePath);

        // csvLookupDir is optional. If not provided, then true is a correct response.
        if (permittedDir == null) {
            return true;
        }

        final Path filePath = Paths.get(csvFilePath).toFile().getCanonicalFile().toPath();

        return filePath.startsWith(permittedDir);
    }
}
