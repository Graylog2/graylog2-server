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
package org.graylog.datanode.bootstrap.preflight;

import org.apache.commons.io.FileUtils;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class DatanodeDirectoriesCheckTest {


    private Path nonreadableDir;
    private Path nonWriteableDir;
    private Path nonexistentDir;
    private Path fileInsteadOfDir;

    @BeforeEach
    void setUp(@TempDir Path tmpDir) throws IOException {

        nonreadableDir = tmpDir.resolve("nonreadable");
        Files.createDirectories(nonreadableDir);
        Assertions.assertThat(nonreadableDir.toFile().setReadable(false))
                .isTrue()
                .describedAs("Failed to disable read permissions on temp file");

        nonWriteableDir = tmpDir.resolve("nonwriteable");
        Files.createDirectories(nonWriteableDir);
        Assertions.assertThat(nonWriteableDir.toFile().setWritable(false))
                .isTrue()
                .describedAs("Failed to disable read permissions on temp file");

        fileInsteadOfDir = tmpDir.resolve("file.txt");
        FileUtils.writeStringToFile(fileInsteadOfDir.toFile(), "Hello File", StandardCharsets.UTF_8);

        nonexistentDir = tmpDir.resolve("someother");
    }

    @Test
    void checkReadDirFailure() {
        Assertions.assertThatThrownBy(() -> DatanodeDirectoriesCheck.checkReadDir(nonreadableDir, "my_config_value"))
                .isInstanceOf(DatanodeDirectoryException.class)
                .hasMessageStartingWith("Datanode needs READ permissions");
    }


    @Test
    void checkReadOfFileInsteadOfDir() throws IOException {
        Assertions.assertThatThrownBy(() -> {
                    DatanodeDirectoriesCheck.checkReadDir(fileInsteadOfDir, "my_config_value");
                })
                .isInstanceOf(DatanodeDirectoryException.class)
                .hasMessageStartingWith("Datanode expects " + fileInsteadOfDir + " to be a directory");
    }

    @Test
    void checkReadWriteDirFailure() {
        Assertions.assertThatThrownBy(() -> DatanodeDirectoriesCheck.checkReadWriteDir(nonWriteableDir, "my_config_value"))
                .isInstanceOf(DatanodeDirectoryException.class)
                .hasMessageStartingWith("Datanode needs WRITE permissions");
    }

    @Test
    void testCreateDir() throws DatanodeDirectoryException {
        // this should create a directory without any problems
        DatanodeDirectoriesCheck.checkReadWriteDir(nonexistentDir, "my_config_value");

        Assertions.assertThatThrownBy(() -> {
            DatanodeDirectoriesCheck.checkReadWriteDir(nonWriteableDir.resolve("subdir"), "my_config_value");
        })
                .isInstanceOf(DatanodeDirectoryException.class)
                .hasMessageStartingWith("Failed to create directory");

    }
}
