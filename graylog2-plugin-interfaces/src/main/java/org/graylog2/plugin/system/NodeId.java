/*
 * Copyright 2012-2014 TORCH GmbH
 *
 * This file is part of Graylog2.
 *
 * Graylog2 is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Graylog2 is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Graylog2.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.graylog2.plugin.system;

import org.apache.commons.io.FileUtils;
import org.graylog2.plugin.Tools;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;

/**
 * @author Lennart Koopmann <lennart@torch.sh>
 */
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
        } catch(FileNotFoundException e) {
            return generate();
        } catch (Exception e2) {
            LOG.error("Could not read or generate node ID: ", e2);
            throw new RuntimeException(e2);
        }
    }

    private String read() throws IOException {
        BufferedReader br = new BufferedReader(new FileReader(filename));
        try {
            return br.readLine();
        } finally {
            if (br != null) {
                br.close();
            }
        }
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
