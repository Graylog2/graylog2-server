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
package org.graylog.testing;

import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class ResourceUtil {
    public static File resourceToTmpFile(String resourceName) {
        final URL resource = ResourceUtil.class.getClassLoader().getResource(resourceName);

        if (resource == null) {
            throw new RuntimeException("Couldn't load resource " + resourceName);
        }

        return resourceURLToTmpFile(resource).toFile();
    }

    public static Path resourceURLToTmpFile(URL resourceUrl) {
        final Path path = createTempFile(resourceUrl);

        try {
            FileUtils.copyInputStreamToFile(resourceUrl.openStream(), path.toFile());
        } catch (IOException e) {
            throw new UncheckedIOException("Error copying resource to file: " + resourceUrl, e);
        }

        return path;
    }

    private static Path createTempFile(URL resourceUrl) {
        try {
            final Path tmpDir = Paths.get(System.getProperty("java.io.tmpdir"));
            final Path path = Files.createTempFile(tmpDir, "graylog-test-resource-file-", null);

            // Temp files should automatically be deleted on exit of the JVM
            path.toFile().deleteOnExit();

            return path;
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to create temp resource file: " + resourceUrl.toString(), e);
        }
    }
}
