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
package org.graylog2.plugin.system;

import org.apache.commons.io.FileUtils;
import org.graylog2.plugin.Tools;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Provider;
import jakarta.inject.Singleton;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Paths;
import java.util.List;

@Singleton
public class FilePersistedNodeIdProvider implements Provider<NodeId> {
    private static final Logger LOG = LoggerFactory.getLogger(FilePersistedNodeIdProvider.class);
    private final String filename;

    @Inject
    public FilePersistedNodeIdProvider(@Named("node_id_file") final String filename) {
        this.filename = filename;
    }

    @Override
    public NodeId get() {
        return new SimpleNodeId(readOrGenerate(filename));
    }

    private String readOrGenerate(String filename) {
        try {
            String read = read(filename);

            if (read == null || read.isEmpty()) {
                return generate(filename);
            }

            LOG.info("Node ID: {}", read);
            return read;
        } catch (FileNotFoundException | NoSuchFileException e) {
            return generate(filename);
        } catch (Exception e) {
            final String msg = "Could not read or generate node ID!";
            LOG.debug(msg, e);
            throw new NodeIdPersistenceException(msg, e);
        }
    }

    private String read(String filename) throws IOException {
        final List<String> lines = Files.readAllLines(Paths.get(filename), StandardCharsets.UTF_8);

        return lines.size() > 0 ? lines.get(0) : "";
    }

    private String generate(String filename) throws NodeIdPersistenceException {
        String generated = Tools.generateServerId();
        LOG.info("No node ID file found. Generated: {}", generated);

        try {
            persist(generated, filename);
        } catch (IOException e) {
            LOG.debug("Could not persist node ID: ", e);
            throw new NodeIdPersistenceException("Unable to persist node ID", e);
        }
        return generated;
    }

    private void persist(String nodeId, String filename) throws IOException {
        FileUtils.writeStringToFile(new File(filename), nodeId, StandardCharsets.UTF_8);
    }
}
