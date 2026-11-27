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
package org.graylog2.configuration.managed;

import com.github.joschi.jadconfig.Repository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Feeds the values stored through the API into JadConfig. Placed in front of all other repositories, so a stored
 * value wins over environment variables, system properties and the configuration file.
 * <p>
 * The values are loaded again whenever JadConfig processes the configuration beans, which it does by opening its
 * repositories. Only parameters of the {@link ConfigurationCatalog} which are backed by a configuration bean are
 * served: everything else in the collection is ignored, so nothing but registered parameters can be changed this way.
 */
public class ManagedConfigurationRepository implements Repository {
    private static final Logger LOG = LoggerFactory.getLogger(ManagedConfigurationRepository.class);

    private final ManagedConfigurationService configurationService;
    private final String nodeId;
    private final Set<String> servedParameters;

    private volatile Map<String, String> values = Map.of();
    private volatile boolean suspended = false;

    /**
     * @param ignoredParameters names of parameters whose stored value isn't served
     */
    public ManagedConfigurationRepository(ManagedConfigurationService configurationService,
                                          ConfigurationCatalog catalog,
                                          String nodeId,
                                          Set<String> ignoredParameters) {
        this.configurationService = configurationService;
        this.nodeId = nodeId;
        this.servedParameters = catalog.get(configurationService.nodeType()).stream()
                .filter(parameter -> parameter.backing() == ParameterBacking.CONFIG_FILE)
                .map(ConfigurationParameter::name)
                .filter(name -> !ignoredParameters.contains(name))
                .collect(Collectors.toUnmodifiableSet());
    }

    /**
     * The parameters this repository can serve a value for, which are therefore the ones whose value can go away
     * at runtime.
     */
    public Set<String> servedParameters() {
        return servedParameters;
    }

    @Override
    public void open() {
        try {
            values = configurationService.storedValues(nodeId).entrySet().stream()
                    .filter(entry -> servedParameters.contains(entry.getKey()))
                    .collect(Collectors.toUnmodifiableMap(Map.Entry::getKey, Map.Entry::getValue));
        } catch (Exception e) {
            // Keep what has been loaded before. Failing here would fail the whole processing run, leaving the node
            // without any configuration because the database isn't reachable for a moment.
            LOG.warn("Couldn't load the stored configuration values, keeping the ones loaded before.", e);
        }
    }

    @Override
    public String read(String name) {
        return suspended ? null : values.get(name);
    }

    @Override
    public String describeSource(String name) {
        return "configuration stored in the database";
    }

    @Override
    public void close() {
    }

    /**
     * Stops serving values, for when a stored value keeps the configuration beans from being processed. The node
     * then falls back to its own configuration instead of failing.
     */
    public void suspend() {
        suspended = true;
    }

    public void resume() {
        suspended = false;
    }
}
