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
package org.graylog.datanode.configuration.snapshots;

import com.github.joschi.jadconfig.JadConfig;
import com.github.joschi.jadconfig.RepositoryException;
import com.github.joschi.jadconfig.ValidationException;
import com.github.joschi.jadconfig.repositories.InMemoryRepository;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.Map;

class FsRepositoryConfigurationTest {

    @Test
    void testPathRepoPrimaryName(@TempDir Path tempDir) throws ValidationException, RepositoryException {
        final String tmpDirPath = tempDir.toAbsolutePath().toString();
        // this is the supported and expected property
        Assertions.assertThat(initializeConfiguration(Map.of("path_repo", tmpDirPath)))
                .satisfies(conf -> {
                    Assertions.assertThat(conf.isRepositoryEnabled()).isTrue();
                    Assertions.assertThat(conf.opensearchProperties())
                            // the opensearch config option is using dot in the name
                            .containsEntry("path.repo", tmpDirPath);
                });
    }


    @Test
    void testPathRepoFallback(@TempDir Path tempDir) throws ValidationException, RepositoryException {
        final String tmpDirPath = tempDir.toAbsolutePath().toString();
        // The dot variant is supported as fallback, should work, but we'll output a warning
        Assertions.assertThat(initializeConfiguration(Map.of("path.repo", tmpDirPath)))
                .satisfies(conf -> {
                    Assertions.assertThat(conf.isRepositoryEnabled()).isTrue();
                    Assertions.assertThat(conf.opensearchProperties())
                            // the opensearch config option is using dot in the name
                            .containsEntry("path.repo", tmpDirPath);
                });
    }

    private FsRepositoryConfiguration initializeConfiguration(Map<String, String> properties) throws RepositoryException, ValidationException {
        final FsRepositoryConfiguration configuration = new FsRepositoryConfiguration();
        new JadConfig(new InMemoryRepository(properties), configuration).process();
        return configuration;
    }
}
