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

import org.junit.Test;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.Set;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class AllowedAuxiliaryPathCheckerTest {

    public static final String ALLOWED_PATH = "/allowed-path";
    AllowedAuxiliaryPathChecker pathChecker;

    @Test
    public void success() throws IOException {
        final Set<Path> paths = Collections.singleton(Paths.get(ALLOWED_PATH));
        pathChecker = new AllowedAuxiliaryPathChecker(paths);
        assertTrue(pathChecker.isInAllowedPath(ALLOWED_PATH + "/file.csv"));
    }

    @Test
    public void successRelativePaths() throws IOException {
        final Set<Path> paths = Collections.singleton(Paths.get(ALLOWED_PATH + "/sub-dir/../"));
        pathChecker = new AllowedAuxiliaryPathChecker(paths);
        assertTrue(pathChecker.isInAllowedPath(ALLOWED_PATH + "/file.csv"));
    }

    @Test
    public void failureOutsideOfAllowedPath() throws IOException {
        final Set<Path> paths = Collections.singleton(Paths.get(ALLOWED_PATH));
        pathChecker = new AllowedAuxiliaryPathChecker(paths);
        assertFalse(pathChecker.isInAllowedPath("/disallowed-path/file.csv"));
    }
}
