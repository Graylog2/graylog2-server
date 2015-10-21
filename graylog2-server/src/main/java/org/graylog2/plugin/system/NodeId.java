/**
 * The MIT License
 * Copyright (c) 2012 Graylog, Inc.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
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
        FileUtils.writeStringToFile(new File(filename), nodeId);
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
}
