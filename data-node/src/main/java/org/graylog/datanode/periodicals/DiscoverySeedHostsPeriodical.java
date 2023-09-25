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
package org.graylog.datanode.periodicals;

import org.graylog.datanode.Configuration;
import org.graylog2.cluster.Node;
import org.graylog2.cluster.NodeService;
import org.graylog2.plugin.periodical.Periodical;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import javax.inject.Inject;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

public class DiscoverySeedHostsPeriodical extends Periodical {

    private static final Logger LOG = LoggerFactory.getLogger(DiscoverySeedHostsPeriodical.class);
    private final NodeService nodeService;
    private final Path hostsfile;

    private final Set<String> nodes = new HashSet<>();


    @Inject
    public DiscoverySeedHostsPeriodical(final NodeService nodeService, final Configuration configuration) {
        this.nodeService = nodeService;
        this.hostsfile = Path.of(configuration.getOpensearchConfigLocation()).resolve("opensearch").resolve("unicast_hosts.txt");
    }

    @Override
    public boolean runsForever() {
        return false;
    }

    @Override
    public boolean stopOnGracefulShutdown() {
        return false;
    }

    @Override
    public boolean startOnThisNode() {
        return true;
    }

    @Override
    public boolean isDaemon() {
        return true;
    }

    @Override
    public int getInitialDelaySeconds() {
        return 1;
    }

    @Override
    public int getPeriodSeconds() {
        return 2;
    }

    @Nonnull
    @Override
    protected Logger getLogger() {
        return LOG;
    }

    @Override
    public void initialize() {}

    @Override
    public synchronized void doRun() {
        final Set<String> current = nodeService.allActive(Node.Type.DATANODE).values().stream().map(node -> {
            try {
                return new URI(node.getTransportAddress()).getHost();
            } catch (URISyntaxException ex) {
                LOG.warn("Could not get host:port from {}", node);
                return null;
            }
        }).filter(Objects::nonNull).collect(Collectors.toSet());

        if (!nodes.equals(current)) {
            try {
                Files.write(hostsfile, current, Charset.defaultCharset(), StandardOpenOption.WRITE, StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.CREATE);
            } catch (IOException iox) {
                LOG.error("Could not write to file: {} - {}", hostsfile, iox.getMessage());
            }
            nodes.clear();
            nodes.addAll(current);
        }
    }
}
