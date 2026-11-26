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

import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;
import jakarta.annotation.Nullable;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Singleton;
import org.graylog2.plugin.system.NodeId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;

import static org.graylog2.shared.utilities.StringUtils.f;

/**
 * Makes this node pick up the values stored through the API.
 * <p>
 * Picking up means loading the stored values, processing the configuration beans again with them in front of all
 * other sources, and reporting the outcome. It happens whenever a {@link ManagedConfigurationChangedEvent} concerns
 * this node, and again right before the node starts the process it manages: the collection behind the cluster event
 * bus is capped, so an event can be missed.
 * <p>
 * Picking a value up doesn't put it to use. Parameters which need a restart are only read when the process is
 * started, which is why the times of loading and of the process becoming available are recorded: see
 * {@link PendingRestarts}.
 */
@Singleton
public class ManagedConfigurationApplier {
    /**
     * Name of the configuration parameter with the names of the parameters whose stored value is ignored, as the way
     * out when a stored value keeps a node from working. A node which picks up stored values has to declare it in
     * its own configuration bean: it must never come from the database.
     */
    public static final String IGNORED_PARAMETERS = "ignore_stored_configuration";
    /**
     * Ignores the stored values of all parameters.
     */
    public static final String IGNORE_ALL = "*";

    private static final Logger LOG = LoggerFactory.getLogger(ManagedConfigurationApplier.class);

    private final NodeType nodeType;
    private final String nodeId;
    private final ManagedConfigurationService configurationService;
    private final ConfigurationReprocessor reprocessor;
    private final NodeConfigurationReporter reporter;
    private final ManagedConfigurationRepository repository;
    private final Set<String> processStartParameters;
    private final Set<String> liveParameters;
    private final boolean enabled;

    @Inject
    public ManagedConfigurationApplier(NodeType nodeType,
                                       NodeId nodeId,
                                       ConfigurationCatalog catalog,
                                       ManagedConfigurationServices configurationServices,
                                       ConfigurationReprocessor reprocessor,
                                       NodeConfigurationReporter reporter,
                                       EventBus eventBus,
                                       @Named(IGNORED_PARAMETERS) Set<String> ignoredParameters) {
        this.nodeType = nodeType;
        this.nodeId = nodeId.getNodeId();
        this.configurationService = configurationServices.get(nodeType);
        this.reprocessor = reprocessor;
        this.reporter = reporter;
        final Set<String> ignored = ignoredParameters.stream()
                .map(String::trim)
                .filter(name -> !name.isEmpty())
                .collect(Collectors.toUnmodifiableSet());
        this.repository = new ManagedConfigurationRepository(configurationService, catalog, this.nodeId, ignored);
        this.enabled = !ignored.contains(IGNORE_ALL);
        final Set<ConfigurationParameter> runtimeParameters = catalog.get(nodeType).stream()
                .filter(parameter -> parameter.backing() == ParameterBacking.RUNTIME)
                .collect(Collectors.toUnmodifiableSet());
        this.processStartParameters = names(runtimeParameters, true).stream()
                .filter(name -> enabled && !ignored.contains(name))
                .collect(Collectors.toUnmodifiableSet());
        // Settings applied live hold for the whole cluster and every node writes them, so ignoring them on a single
        // node can't work. They never keep a node from starting either, so a bad value can be removed through the
        // API instead.
        this.liveParameters = names(runtimeParameters, false);
        final Set<String> ignoredLiveParameters = ignored.stream()
                .filter(liveParameters::contains)
                .collect(Collectors.toCollection(TreeSet::new));
        if (!ignoredLiveParameters.isEmpty()) {
            LOG.warn("Ignoring stored values has no effect on settings which apply to the whole cluster without a " +
                    "restart: {}. Remove or correct the stored value instead.", ignoredLiveParameters);
        }

        if (!enabled) {
            LOG.warn("All configuration values stored through the API are ignored on this node.");
        } else {
            if (!ignored.isEmpty()) {
                LOG.warn("The configuration values stored through the API are ignored on this node for: {}", ignored);
            }
            // Replicated cluster events arrive on the regular event bus
            eventBus.register(this);
        }
    }

    private static Set<String> names(Set<ConfigurationParameter> parameters, boolean requiresRestart) {
        return parameters.stream()
                .filter(parameter -> parameter.requiresRestart() == requiresRestart)
                .map(ConfigurationParameter::name)
                .collect(Collectors.toUnmodifiableSet());
    }

    @Subscribe
    public void handleConfigurationChanged(ManagedConfigurationChangedEvent event) {
        final boolean concernsThisNode = event.nodeType() == nodeType
                && (event.nodeId() == null || event.nodeId().equals(nodeId));
        if (concernsThisNode) {
            LOG.debug("Stored configuration values changed: {}", event.names());
            refresh();
        }
    }

    /**
     * Loads the stored values, processes the configuration beans with them and reports the outcome.
     */
    public synchronized void refresh() {
        if (!enabled) {
            return;
        }
        repository.resume();
        reprocessor.prependRepository(repository);
        try {
            reprocessor.reprocess(repository.servedParameters());
        } catch (Exception e) {
            LOG.error("Couldn't process the configuration with the values stored through the API, falling back to " +
                    "the configuration of this node. Correct or remove the stored value to resolve this.", e);
            fallBackToNodeConfiguration();
        }
        reporter.report();
    }

    private void fallBackToNodeConfiguration() {
        repository.suspend();
        try {
            reprocessor.reprocess(repository.servedParameters());
        } catch (Exception e) {
            LOG.error("Couldn't process the configuration of this node either.", e);
        }
    }

    /**
     * To be called right before the process this node manages is started, so that it starts with the stored values
     * and the time they were loaded at is known.
     * <p>
     * The times are recorded even while stored values are ignored. A restart is then still visible to whoever
     * waits for it, at the price of stored values no longer being shown as waiting for one.
     */
    public synchronized void prepareProcessStart() {
        try {
            refresh();
            configurationService.markConfigurationLoaded(nodeId);
        } catch (Exception e) {
            // Starting the process matters more than picking up stored values, which is retried on the next start
            LOG.error("Couldn't pick up the stored configuration values before starting the process.", e);
        }
    }

    /**
     * To be called once the process started after {@link #prepareProcessStart()} is available.
     */
    public void processAvailable() {
        try {
            configurationService.markConfigurationApplied(nodeId);
        } catch (Exception e) {
            LOG.error("Couldn't record that the process runs with the stored configuration values.", e);
        }
    }

    /**
     * The stored values of runtime parameters which only take effect when the process starts. They aren't declared in
     * a configuration bean, so processing the beans doesn't apply them: whoever starts the process has to hand them
     * over.
     *
     * @return the stored values keyed by parameter name
     */
    public Map<String, String> processStartValues() {
        return storedValues(processStartParameters);
    }

    /**
     * The stored values of the parameters which are applied to the running process.
     *
     * @return the stored values keyed by parameter name, a subset of {@link #liveParameters()}
     */
    public Map<String, String> liveValues() {
        return storedValues(liveParameters);
    }

    /**
     * The parameters this node applies to the running process. A parameter in here without a stored value has to be
     * reset, so that the process falls back to its own configuration. The parameters named by
     * {@link #IGNORED_PARAMETERS} are included as well: these settings hold for the whole cluster, so ignoring them
     * on a single node would have no effect.
     */
    public Set<String> liveParameters() {
        return liveParameters;
    }

    /**
     * Stores a value found in the running process for a parameter which is applied live, unless a value has ever
     * been stored for it. Values which were set by other means before the parameter was managed are kept that way,
     * while a value removed through the API stays removed.
     *
     * @return {@code true} if the value has been stored by this call
     */
    public boolean adoptLiveValue(String name, String value) {
        if (!liveParameters.contains(name)) {
            return false;
        }
        return configurationService.adoptClusterValue(name, value,
                f("adopted from the OpenSearch cluster settings by node %s", nodeId));
    }

    /**
     * Records whether this node managed to apply the stored values of the parameters which are applied live, so that
     * a failure can be shown.
     *
     * @param error why applying failed, {@code null} if it succeeded
     */
    public void recordLiveSettingsResult(@Nullable String error) {
        try {
            configurationService.recordLiveSettingsResult(nodeId, error);
        } catch (Exception e) {
            LOG.warn("Couldn't record the outcome of applying the stored cluster settings.", e);
        }
    }

    private Map<String, String> storedValues(Set<String> parameters) {
        if (parameters.isEmpty()) {
            return Map.of();
        }
        return configurationService.storedValues(nodeId).entrySet().stream()
                .filter(entry -> parameters.contains(entry.getKey()))
                .collect(Collectors.toUnmodifiableMap(Map.Entry::getKey, Map.Entry::getValue));
    }
}
