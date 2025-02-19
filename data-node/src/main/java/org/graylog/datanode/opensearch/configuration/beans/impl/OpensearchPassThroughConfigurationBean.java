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
package org.graylog.datanode.opensearch.configuration.beans.impl;

import jakarta.inject.Inject;
import org.graylog.datanode.Configuration;
import org.graylog.datanode.opensearch.configuration.OpensearchConfigurationParams;
import org.graylog.datanode.process.configuration.beans.DatanodeConfigurationBean;
import org.graylog.datanode.process.configuration.beans.DatanodeConfigurationPart;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.function.Supplier;

public class OpensearchPassThroughConfigurationBean implements DatanodeConfigurationBean<OpensearchConfigurationParams> {

    private static final Logger LOG = LoggerFactory.getLogger(OpensearchSecurityConfigurationBean.class);
    private final Configuration configuration;
    private final Supplier<Map<String, String>> systemEnvSupplier;

    @Inject
    public OpensearchPassThroughConfigurationBean(Configuration configuration) {
        this(configuration, System::getenv);
    }

    public OpensearchPassThroughConfigurationBean(Configuration configuration, Supplier<Map<String, String>> systemEnvSupplier) {
        this.configuration = configuration;
        this.systemEnvSupplier = systemEnvSupplier;
    }


    @Override
    public DatanodeConfigurationPart buildConfigurationPart(OpensearchConfigurationParams configurationParams) {

        final DatanodeConfigurationPart.Builder builder = DatanodeConfigurationPart.builder();

        Map<String, String> properties = new LinkedHashMap<>();

        // now copy all the environment values to the configuration arguments. Opensearch won't do it for us,
        // because we are using tar distriburion and opensearch does this only for docker dist. See opensearch-env script
        // additionally, the env variables have to be prefixed with opensearch. (e.g. "opensearch.cluster.routing.allocation.disk.threshold_enabled")
        systemEnvSupplier.get().entrySet().stream()
                .filter(entry -> entry.getKey().matches("^opensearch\\.[a-z0-9_]+(?:\\.[a-z0-9_]+)+"))
                .peek(entry -> LOG.info("Detected pass-through opensearch property {}:{}", entry.getKey().substring("opensearch.".length()), entry.getValue()))
                .forEach(entry -> properties.put(entry.getKey().substring("opensearch.".length()), entry.getValue()));

        configuration.getOpensearchProperties().forEach((key, value) -> properties.put(key.toLowerCase(Locale.ROOT).replaceAll("_", "."), value));

        return builder.properties(properties).build();
    }
}
