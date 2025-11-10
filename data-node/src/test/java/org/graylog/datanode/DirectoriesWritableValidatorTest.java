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
package org.graylog.datanode;

import com.github.joschi.jadconfig.JadConfig;
import com.github.joschi.jadconfig.Parameter;
import com.github.joschi.jadconfig.RepositoryException;
import com.github.joschi.jadconfig.ValidationException;
import com.github.joschi.jadconfig.repositories.InMemoryRepository;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

class DirectoriesWritableValidatorTest {

    @Test
    void testValidDirList(@TempDir Path tempDir) throws IOException {

        final Path snapshotsPath = Files.createDirectory(tempDir.resolve("snapshots"));

        final MyConfiguration configuration = new MyConfiguration();
        final InMemoryRepository mandatoryProps = new InMemoryRepository(Map.of(
                "path_repo", snapshotsPath.toAbsolutePath().toString()
        ));

        try {
            new JadConfig(List.of(mandatoryProps), configuration).process();

            Assertions.assertThat(configuration.pathRepo)
                    .isNotNull()
                    .hasSize(1)
                    .contains(snapshotsPath);

        } catch (RepositoryException | ValidationException e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    void testInvalidDirList(@TempDir Path tempDir) throws IOException {
        final MyConfiguration configuration = new MyConfiguration();
        final InMemoryRepository mandatoryProps = new InMemoryRepository(Map.of(
                "path_repo", "[\"/one\", \"/two\"]"
        ));

        try {
            new JadConfig(List.of(mandatoryProps), configuration).process();
            Assertions.fail("Should have thrown an exception");
        } catch (RepositoryException | ValidationException e) {
            Assertions.assertThat(e)
                    .isInstanceOf(ValidationException.class)
                    .hasMessageContaining("Cannot write to directory path_repo at path [\"/one\". Directory doesn't exist. Please create the directory.");
        }
    }

    @Test
    void testPartiallyValidDirList(@TempDir Path tempDir) throws IOException {

        final Path one = Files.createDirectory(tempDir.resolve("one"));
        final Path two = tempDir.resolve("two"); // second directory is not created and doesn't exist, is subject of test

        final MyConfiguration configuration = new MyConfiguration();
        final InMemoryRepository mandatoryProps = new InMemoryRepository(Map.of(
                "path_repo", one.toAbsolutePath() + "," + two.toAbsolutePath()
        ));

        try {
            new JadConfig(List.of(mandatoryProps), configuration).process();
            Assertions.fail("Should have thrown an exception");
        } catch (RepositoryException | ValidationException e) {
            Assertions.assertThat(e)
                    .isInstanceOf(ValidationException.class)
                    .hasMessageStartingWith("Cannot write to directory path_repo at path")
                    .hasMessageContaining(two.toAbsolutePath().toString())
                    .hasMessageEndingWith("Directory doesn't exist. Please create the directory.");
        }
    }

    static class MyConfiguration {
        @Parameter(value = "path_repo", converter = PathListConverter.class, validators = DirectoriesWritableValidator.class)
        private List<Path> pathRepo;
    }
}
