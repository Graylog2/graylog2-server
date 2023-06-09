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
package org.graylog.datanode.configuration.variants;

import com.google.common.collect.ImmutableMap;
import org.graylog.datanode.Configuration;
import org.graylog.security.certutil.ca.exceptions.KeyStoreStorageException;

import java.io.IOException;
import java.nio.file.Path;
import java.security.GeneralSecurityException;
import java.util.Map;
import java.util.Objects;

public interface SecurityConfigurationVariant {

    boolean checkPrerequisites(final Configuration localConfiguration);

    Map<String, String> configure(final Configuration localConfiguration) throws KeyStoreStorageException, IOException, GeneralSecurityException;

    default ImmutableMap<String, String> commonConfig(final Configuration localConfiguration) {
        final ImmutableMap.Builder<String, String> config = ImmutableMap.builder();
        Objects.requireNonNull(localConfiguration.getConfigLocation(), "config_location setting is required!");
        localConfiguration.getOpensearchNetworkHostHost().ifPresent(
                networkHost -> config.put("network.host", networkHost));
        config.put("path.data", Path.of(localConfiguration.getOpensearchDataLocation()).resolve(localConfiguration.getDatanodeNodeName()).toAbsolutePath().toString());
        config.put("path.logs", Path.of(localConfiguration.getOpensearchLogsLocation()).resolve(localConfiguration.getDatanodeNodeName()).toAbsolutePath().toString());
        if (localConfiguration.isSingleNodeOnly()) {
            config.put("discovery.type", "single-node");
        } else {
            config.put("cluster.initial_master_nodes", "node1");
        }

        // listen on all interfaces
        config.put("network.bind_host", "0.0.0.0");

        return config.build();
    }
}
