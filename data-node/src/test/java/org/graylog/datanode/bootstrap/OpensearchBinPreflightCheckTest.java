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
package org.graylog.datanode.bootstrap;

import org.assertj.core.api.Assertions;
import org.graylog.datanode.bootstrap.preflight.OpensearchBinPreflightCheck;
import org.graylog2.bootstrap.preflight.PreflightCheckException;
import org.graylog2.shared.utilities.StringUtils;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.PosixFilePermission;
import java.util.Collections;

public class OpensearchBinPreflightCheckTest {

    @TempDir
    private Path tempDir;

    @Test
    void testNonexistentDirectory() {
        final Path baseDirectory = tempDir.resolve("nonexistent");
        final OpensearchBinPreflightCheck check = new OpensearchBinPreflightCheck(baseDirectory);
        Assertions.assertThatThrownBy(check::runCheck)
                .isInstanceOf(PreflightCheckException.class)
                .hasMessage("Opensearch base directory %s doesn't exist!", baseDirectory.toAbsolutePath());
    }


    @Test
    void testNonexistentBinary() throws IOException {
        final Path baseDir = tempDir.resolve("opensearch");
        final Path binDir = baseDir.resolve("bin");
        Files.createDirectories(binDir);

        // nonexistent!
        final Path executable = binDir.resolve("opensearch");

        final OpensearchBinPreflightCheck check = new OpensearchBinPreflightCheck(baseDir);

        Assertions.assertThatThrownBy(check::runCheck)
                .isInstanceOf(PreflightCheckException.class)
                .hasMessage("Opensearch binary %s doesn't exist!", executable.toAbsolutePath());
    }

    @Test
    void testBinaryWithoutExecPermission() throws IOException {
        final Path baseDir = tempDir.resolve("opensearch");
        final Path binDir = baseDir.resolve("bin");
        Files.createDirectories(binDir);
        final Path executable = binDir.resolve("opensearch");
        Files.createFile(executable);

        final OpensearchBinPreflightCheck check = new OpensearchBinPreflightCheck(baseDir);
        Assertions.assertThatThrownBy(check::runCheck)
                .isInstanceOf(PreflightCheckException.class)
                .hasMessageStartingWith(StringUtils.f("Opensearch binary %s is not executable!", executable.toAbsolutePath()));
    }


    @Test
    void testBinaryOk() throws IOException {
        final Path baseDir = tempDir.resolve("opensearch");
        final Path binDir = baseDir.resolve("bin");
        Files.createDirectories(binDir);
        final Path executable = binDir.resolve("opensearch");
        Files.createFile(executable);
       Files.setPosixFilePermissions(executable, Collections.singleton(PosixFilePermission.OWNER_EXECUTE));


        final OpensearchBinPreflightCheck check = new OpensearchBinPreflightCheck(baseDir);
        Assertions.assertThatCode(check::runCheck)
                .doesNotThrowAnyException();
    }
}

