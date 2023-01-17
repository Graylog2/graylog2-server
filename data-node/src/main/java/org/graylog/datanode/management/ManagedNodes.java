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
package org.graylog.datanode.management;

import org.graylog.datanode.DataNodeRunner;
import org.graylog.datanode.process.OpensearchProcess;
import org.graylog.datanode.process.ProcessConfiguration;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Set;

@Singleton
public class ManagedNodes {
    private final Set<OpensearchProcess> processes = new LinkedHashSet<>();

    final private String opensearchVersion;

    final private String opensearchLocation;

    final private DataNodeRunner dataNodeRunner;

    @Inject
    public ManagedNodes(@Named("opensearch_version") String opensearchVersion, @Named("opensearch_location") String opensearchLocation, DataNodeRunner dataNodeRunner) {
        this.opensearchVersion = opensearchVersion;
        this.opensearchLocation = opensearchLocation;
        this.dataNodeRunner = dataNodeRunner;
    }

    public void startOpensearchProcesses() {

        // TODO: obtain configuration from outside, one for each node

        final LinkedHashMap<String, String> config = new LinkedHashMap<>();
        config.put("discovery.type", "single-node");
        config.put("plugins.security.ssl.http.enabled", "false");
        config.put("plugins.security.disabled", "true");
        final ProcessConfiguration processConfiguration = new ProcessConfiguration(9300, 9400, config);

        final OpensearchProcess process = dataNodeRunner.start(Path.of(opensearchLocation), opensearchVersion, processConfiguration);
        this.processes.add(process);
    }

    public Set<OpensearchProcess> getProcesses() {
        return processes;
    }
}
