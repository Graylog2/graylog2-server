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

package org.graylog.integrations;

import java.io.BufferedReader;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * A base class to provide helper methods needed by tests that need to access resources (i.e. files).
 */
public abstract class TestWithResources {


    /**
     * Given a resource name (located at this class's hierarchy) return its content as text.
     *
     * <p>
     * It is assumed the resource's content can be faithfully represented as a string with <b>UTF-8</b> encoding.
     * </p>
     *
     * @param name resource name
     * @return file content
     */
    protected String getFileText(String name) {

        StringBuilder text = new StringBuilder();
        try (BufferedReader is = Files.newBufferedReader(getResourcePath(name), StandardCharsets.UTF_8)) {
            while (true) {
                char[] buff = new char[1024];
                int count = is.read(buff);
                if (count < 1) {
                    break;
                }

                String string = String.valueOf(buff, 0, count);
                text.append(string);
            }
        } catch (IOException e) {

            String error = String.format("Unable to read resource file '%s'. %s", name, e.getMessage());
            throw new IllegalArgumentException(error, e);
        }

        return text.toString();
    }

    /**
     * Get the {@link Path} for a resource located in the same hierarchy as this class.
     *
     * @param name resource name
     * @return full resource path
     */
    protected Path getResourcePath(String name) {
        try {
            URL resource = getClass().getResource(name);
            if (resource == null) {
                URL path = getClass().getResource("");
                String error = String.format("Error.  Resource '%s' does not exist at '%s'", name, path);
                throw new IllegalArgumentException(error);
            }

            return Paths.get(resource.toURI());

        } catch (URISyntaxException e) {
            String error = String.format("Error getting resource '%s'. %s", name, e.getMessage());
            throw new IllegalArgumentException(error, e);
        }
    }
}