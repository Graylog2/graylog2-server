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
import com.github.joschi.jadconfig.RepositoryException;
import com.github.joschi.jadconfig.ValidationException;
import com.github.joschi.jadconfig.repositories.InMemoryRepository;
import jakarta.annotation.Nonnull;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;

public class DatanodeTestUtils {

    public final static Path TMP_DIR = Paths.get(System.getProperty("java.io.tmpdir"));

    public static Configuration datanodeConfiguration(Map<String, String> properties) throws RepositoryException, ValidationException, IOException {
        final Configuration configuration = new Configuration();
        final InMemoryRepository mandatoryProps = new InMemoryRepository(Map.of(
                "password_secret", "thisisverysecretpassword",
                "node_id_file", TMP_DIR.resolve("node_id").toAbsolutePath().toString(),
                "opensearch_logs_location", createTmpDir("opensearch", "logs"),
                "opensearch_config_location", createTmpDir("opensearch", "config")
        ));
        new JadConfig(List.of(mandatoryProps, new InMemoryRepository(properties)), configuration).process();
        return configuration;
    }

    @Nonnull
    private static String createTmpDir(String... other) throws IOException {
        final Path path = Path.of(TMP_DIR.toAbsolutePath().toString(), other);
        Files.createDirectories(path);
        return path.toAbsolutePath().toString();
    }
}
