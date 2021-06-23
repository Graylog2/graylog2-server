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

import com.google.common.hash.Hashing;
import org.apache.commons.io.FileUtils;
import org.graylog2.plugin.Tools;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Paths;
import java.util.List;

public class NodeId {
    private static final Logger LOG = LoggerFactory.getLogger(NodeId.class);

    private final String filename;
    private final String id;

    public NodeId(final String filename) {
        this.filename = filename;
        this.id = readOrGenerate();
    }

    private String readOrGenerate() {
        try {
            String read = read();

            if (read == null || read.isEmpty()) {
                return generate();
            }

            LOG.info("Node ID: {}", read);
            return read;
        } catch (FileNotFoundException | NoSuchFileException e) {
            return generate();
        } catch (Exception e2) {
            final String msg = "Could not read or generate node ID!";
            LOG.debug(msg, e2);
            throw new NodeIdPersistenceException(msg, e2);
        }
    }

    private String read() throws IOException {
        final List<String> lines = Files.readAllLines(Paths.get(filename), StandardCharsets.UTF_8);

        return lines.size() > 0 ? lines.get(0) : "";
    }

    private String generate() throws NodeIdPersistenceException {
        String generated = Tools.generateServerId();
        LOG.info("No node ID file found. Generated: {}", generated);

        try {
            persist(generated);
        } catch (IOException e1) {
            LOG.debug("Could not persist node ID: ", e1);
            throw new NodeIdPersistenceException("Unable to persist node ID", e1);
        }

        return generated;
    }

    private void persist(String nodeId) throws IOException {
        FileUtils.writeStringToFile(new File(filename), nodeId, StandardCharsets.UTF_8);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString() {
        return id;
    }

    /**
     * Generate an "anonymized" node ID for use with external services. Currently it just hashes the actual node ID
     * using SHA-256.
     *
     * @return The anonymized ID derived from hashing the node ID.
     */
    public String anonymize() {
        return Hashing.sha256().hashString(id, StandardCharsets.UTF_8).toString();
    }

    public String getShortNodeId() {
        return id.split("-")[0];
    }
}
