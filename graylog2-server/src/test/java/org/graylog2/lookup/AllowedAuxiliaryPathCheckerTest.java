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

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.TreeSet;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class AllowedAuxiliaryPathCheckerTest {

    public static final String FILE = "file";

    @Rule
    public TemporaryFolder permittedTempDir = new TemporaryFolder();

    @Rule
    public TemporaryFolder forbiddenTempDir = new TemporaryFolder();

    AllowedAuxiliaryPathChecker pathChecker;

    @Test
    public void inAllowedPath() throws IOException {
        final Path permittedPath = permittedTempDir.getRoot().toPath();
        final Path filePath = permittedTempDir.newFile(FILE).toPath();
        pathChecker = new AllowedAuxiliaryPathChecker(new TreeSet<>(Collections.singleton(permittedPath)));
        assertTrue(pathChecker.fileIsInAllowedPath(filePath));
    }

    @Test
    public void outsideOfAllowedPath() throws IOException {
        final Path permittedPath = permittedTempDir.getRoot().toPath();
        final Path filePath = forbiddenTempDir.newFile(FILE).toPath();
        pathChecker = new AllowedAuxiliaryPathChecker(new TreeSet<>(Collections.singleton(permittedPath)));
        assertFalse(pathChecker.fileIsInAllowedPath(filePath));
    }

    @Test
    public void noPathsFileLocationOkNoChecksRequired() throws IOException {
        pathChecker = new AllowedAuxiliaryPathChecker(new TreeSet<>(Collections.emptySet()));
        assertTrue(pathChecker.fileIsInAllowedPath(permittedTempDir.newFile(FILE).toPath()));
    }

    @Test
    public void fileDoesNotExist() {
        final Path filePath = Paths.get("non-existent-file");
        pathChecker = new AllowedAuxiliaryPathChecker(
                new TreeSet<>(Collections.singleton(permittedTempDir.getRoot().toPath())));
        assertFalse(pathChecker.fileIsInAllowedPath(filePath));
    }

    @Test
    public void permittedPathDoesNotExist() throws IOException {
        final Path permittedPath = Paths.get("non-existent-file-path");
        final Path filePath = permittedTempDir.newFile(FILE).toPath();

        pathChecker = new AllowedAuxiliaryPathChecker(new TreeSet<>(Collections.singleton(permittedPath)));
        assertFalse(pathChecker.fileIsInAllowedPath(filePath));
    }

    @Test
    public void realPathNullWhenDoesNotExist() {
        final Path path = Paths.get("non-existent-file-path");
        assertNull(AllowedAuxiliaryPathChecker.resolveRealPath(path));
    }

    /**
     * Verifies that the {@literal path.toRealPath()} method is called in the process of checking the file path.
     * This is essential, because this is the operative part that resolves relative paths and symbolic links
     * before verifying that a file is in the appropriate path.
     */
    @Test
    public void verifyToRealPathCalled() throws IOException {
        final Path permittedPath = mock(Path.class);
        final Path filePath = mock(Path.class);

        pathChecker = new AllowedAuxiliaryPathChecker(new TreeSet<>(Collections.singleton(permittedPath)));
        when(filePath.toRealPath()).thenReturn(filePath);
        pathChecker.fileIsInAllowedPath(filePath);
        verify(permittedPath, times(1)).toRealPath();
        verify(filePath, times(1)).toRealPath();
    }
}
