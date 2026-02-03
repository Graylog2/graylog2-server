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
package org.graylog.datanode.opensearch.configuration;

import jakarta.inject.Inject;
import jakarta.inject.Provider;
import jakarta.inject.Singleton;
import org.graylog.datanode.configuration.DatanodeConfiguration;

import java.io.IOException;
import java.nio.file.FileStore;
import java.nio.file.Files;
import java.nio.file.Path;

@Singleton
public class OpensearchUsableSpaceProvider implements Provider<OpensearchUsableSpace> {

    private final Path dataTargetDir;

    @Inject
    public OpensearchUsableSpaceProvider(DatanodeConfiguration datanodeConfiguration) {
        dataTargetDir = datanodeConfiguration.datanodeDirectories().getDataTargetDir();
    }

    @Override
    public OpensearchUsableSpace get() {
        return new OpensearchUsableSpace(dataTargetDir, getUsableSpace(dataTargetDir));
    }

    private static long getUsableSpace(Path opensearchDataLocation) {
        final FileStore fileStore;
        try {
            fileStore = Files.getFileStore(opensearchDataLocation);
            return fileStore.getUsableSpace();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
