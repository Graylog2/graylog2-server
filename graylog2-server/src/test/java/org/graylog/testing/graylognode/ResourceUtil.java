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
package org.graylog.testing.graylognode;

import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Paths;
import java.util.Locale;

public class ResourceUtil {
    static File resourceToTmpFile(@SuppressWarnings("SameParameterValue") String resourceName) {

        InputStream resource = ResourceUtil.class.getClassLoader().getResourceAsStream(resourceName);

        if (resource == null) {
            throw new RuntimeException("Couldn't load resource " + resourceName);
        }

        File f = createTempFile(resourceName);

        try {
            FileUtils.copyInputStreamToFile(resource, f);
        } catch (IOException e) {
            throw new RuntimeException("Error copying resource to file: " + resourceName);
        }

        return f;
    }

    private static File createTempFile(String resourceName) {
        String filename = String.format(Locale.US, "graylog-test-resource-file_%s", Paths.get(resourceName).getFileName());

        try {
            File f = File.createTempFile(filename, null);
            f.deleteOnExit();
            return f;
        } catch (IOException e) {
            throw new RuntimeException("Failed to create temp resource file " + filename, e);
        }
    }
}
