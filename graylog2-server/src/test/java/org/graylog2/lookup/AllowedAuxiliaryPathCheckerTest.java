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
import java.util.TreeSet;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class AllowedAuxiliaryPathCheckerTest {

    AllowedAuxiliaryPathChecker pathChecker;

    @Test
    public void inAllowedPath() throws IOException {
        final Path filePath = validPath();
        final Path permittedPath = validPath();
        when(filePath.startsWith(eq(permittedPath))).thenReturn(true);

        pathChecker = new AllowedAuxiliaryPathChecker(new TreeSet<>(Collections.singleton(permittedPath)));
        assertTrue(pathChecker.fileIsInAllowedPath(filePath));
    }

    private Path validPath() throws IOException {
        final Path filePath = mock(Path.class);
        when(filePath.toRealPath()).thenReturn(filePath);
        return filePath;
    }

    @Test
    public void outsideOfAllowedPath() throws IOException {
        final Path filePath = validPath();
        final Path permittedPath = validPath();
        when(filePath.startsWith(eq(permittedPath))).thenReturn(false);

        pathChecker = new AllowedAuxiliaryPathChecker(new TreeSet<>(Collections.singleton(permittedPath)));
        assertFalse(pathChecker.fileIsInAllowedPath(filePath));
        verify(filePath, times(1)).startsWith(eq(permittedPath));
    }

    @Test
    public void noPathsFileLocationOk() {
        pathChecker = new AllowedAuxiliaryPathChecker(new TreeSet<Path>(Collections.EMPTY_SET));
        assertTrue(pathChecker.fileIsInAllowedPath(Paths.get("")));
    }

    @Test
    public void fileDoesNotExist() throws IOException {
        final Path filePath = mock(Path.class);
        when(filePath.toRealPath()).thenReturn(null);

        pathChecker = new AllowedAuxiliaryPathChecker(new TreeSet<>(Collections.singleton(Paths.get(""))));
        assertFalse(pathChecker.fileIsInAllowedPath(filePath));
    }

    @Test
    public void permittedPathDoesNotExist() throws IOException {
        final Path permittedPath = mock(Path.class);
        when(permittedPath.toRealPath()).thenReturn(null);

        pathChecker = new AllowedAuxiliaryPathChecker(new TreeSet<>(Collections.singleton(permittedPath)));
        assertFalse(pathChecker.fileIsInAllowedPath(validPath()));
    }

    @Test
    public void realPathNullWhenDoesNotExist() throws IOException {
        final Path path = mock(Path.class);
        when(path.toRealPath()).thenThrow(new IOException());
        assertNull(AllowedAuxiliaryPathChecker.resolveRealPath(path));
    }
}
