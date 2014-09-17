/**
 * The MIT License
 * Copyright (c) 2012 TORCH GmbH
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

    public NodeId(String filename) {
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
        } catch(FileNotFoundException | NoSuchFileException e) {
            return generate();
        } catch (Exception e2) {
            LOG.error("Could not read or generate node ID: ", e2);
            throw new RuntimeException(e2);
        }
    }

    private String read() throws IOException {
        final List<String> lines = Files.readAllLines(Paths.get(filename), StandardCharsets.UTF_8);

        return lines.size() > 0 ? lines.get(0) : "";
    }

    private String generate() {
        String generated = Tools.generateServerId();
        LOG.info("No node ID file found. Generated: {}", generated);

        try {
            persist(generated);
        } catch (IOException e1) {
            LOG.error("Could not persist node ID: ", e1);
            throw new RuntimeException(e1);
        }

        return generated;
    }

    private void persist(String nodeId) throws IOException {
        FileUtils.writeStringToFile(new File(filename), nodeId);
    }

    public String toString() {
        return id;
    }

}
