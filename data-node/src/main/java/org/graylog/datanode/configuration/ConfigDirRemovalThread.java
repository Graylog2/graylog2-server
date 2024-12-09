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
package org.graylog.datanode.configuration;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Stream;

public class ConfigDirRemovalThread extends Thread {
    private final Path opensearchConfigDir;

    public ConfigDirRemovalThread(Path opensearchConfigDir) {
        this.opensearchConfigDir = opensearchConfigDir;
    }

    @Override
    public void run() {
        deleteDirectory(opensearchConfigDir);
    }
    private void deleteDirectory(Path toBeDeleted) {
        try {
            if (Files.isDirectory(toBeDeleted)) {
                try (final Stream<Path> list = Files.list(toBeDeleted)) {
                    list.forEach(this::deleteDirectory);
                }
            }
            Files.delete(toBeDeleted);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
