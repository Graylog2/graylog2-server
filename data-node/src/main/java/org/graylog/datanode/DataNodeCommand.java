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

import com.github.rvesse.airline.annotations.Command;
import org.graylog2.bootstrap.CliCommand;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Properties;

@Command(name = "data-node", description = "Start the data-node server")
public class DataNodeCommand implements CliCommand {

    private static final Logger LOG = LoggerFactory.getLogger(Main.class);

    @Override
    public void run() {

        final Path opensearchLocation = Path.of(getOpensearchLocation());

        final LinkedHashMap<String, String> config = new LinkedHashMap<>();
        config.put("discovery.type", "single-node");
        config.put("plugins.security.ssl.http.enabled", "false");
        config.put("plugins.security.disabled", "true");

        final DataNodeRunner runner = new DataNodeRunner(opensearchLocation, config);
        final RunningProcess dataNode = runner.start();

        LOG.info("Data node up and running");

        try {
            dataNode.getProcess().waitFor();
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    private static String getOpensearchLocation() {
        try (InputStream resourceAsStream = Main.class.getResourceAsStream("/configuration.properties")) {
            Properties prop = new Properties();
            prop.load(resourceAsStream);
            return prop.getProperty("opensearch.location");
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

    }
}
