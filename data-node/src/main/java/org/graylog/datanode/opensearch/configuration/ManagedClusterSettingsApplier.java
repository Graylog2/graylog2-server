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
package org.graylog.datanode.opensearch.configuration;

import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;
import jakarta.annotation.Nullable;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import jakarta.json.JsonString;
import jakarta.json.JsonValue;
import org.graylog.datanode.opensearch.OpensearchProcess;
import org.graylog.datanode.opensearch.statemachine.OpensearchEvent;
import org.graylog.datanode.opensearch.statemachine.OpensearchState;
import org.graylog.datanode.process.statemachine.tracer.StateMachineTracer;
import org.graylog.storage.opensearch3.OfficialOpensearchClient;
import org.graylog2.configuration.managed.ManagedConfigurationApplier;
import org.graylog2.configuration.managed.ManagedConfigurationChangedEvent;
import org.graylog2.configuration.managed.NodeType;
import org.opensearch.client.json.JsonData;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeSet;

/**
 * Applies the stored values of the opensearch settings which can be changed without a restart, as persistent
 * cluster settings. They hold for the whole cluster, so each data node applying them only differs in who gets there
 * first: only settings whose persistent value differs from the stored one are written.
 * <p>
 * Values are applied whenever a {@link ManagedConfigurationChangedEvent} concerns them, and again whenever the
 * opensearch process becomes available: an event can be missed, or arrive while the process is down.
 * <p>
 * A setting without a stored value is reset, so that opensearch falls back to the value of its configuration file
 * or its own default. Persistent values written by other means are therefore overwritten as well: for the settings
 * managed here, the database is the only source.
 */
@Singleton
public class ManagedClusterSettingsApplier implements StateMachineTracer<OpensearchState, OpensearchEvent> {
    private static final Logger LOG = LoggerFactory.getLogger(ManagedClusterSettingsApplier.class);

    private final OpensearchProcess process;
    private final ManagedConfigurationApplier managedConfigurationApplier;

    @Inject
    public ManagedClusterSettingsApplier(OpensearchProcess process,
                                         ManagedConfigurationApplier managedConfigurationApplier,
                                         EventBus eventBus) {
        this.process = process;
        this.managedConfigurationApplier = managedConfigurationApplier;
        if (!managedConfigurationApplier.liveParameters().isEmpty()) {
            eventBus.register(this);
        }
    }

    @Subscribe
    public void handleConfigurationChanged(ManagedConfigurationChangedEvent event) {
        final boolean concernsLiveSettings = event.nodeType() == NodeType.DATANODE
                && event.nodeId() == null
                && !Collections.disjoint(event.names(), managedConfigurationApplier.liveParameters());
        if (concernsLiveSettings) {
            apply();
        }
    }

    @Override
    public void trigger(OpensearchEvent trigger) {
    }

    @Override
    public void transition(OpensearchEvent trigger, OpensearchState source, OpensearchState destination) {
        if (source != destination && destination == OpensearchState.AVAILABLE) {
            apply();
        }
    }

    /**
     * Brings the persistent cluster settings in line with the stored values. Failures are only logged: the next
     * change or start of the process tries again.
     */
    public synchronized void apply() {
        final Set<String> parameters = managedConfigurationApplier.liveParameters();
        if (parameters.isEmpty()) {
            return;
        }
        process.openSearchClient().ifPresentOrElse(client -> {
            try {
                apply(client, parameters);
            } catch (Exception e) {
                LOG.warn("Couldn't apply the cluster settings stored through the web interface, retrying on the " +
                        "next change or start of opensearch: {}", e.getMessage());
                LOG.debug("Applying the stored cluster settings failed.", e);
            }
        }, () -> LOG.debug("Opensearch isn't running, applying the stored cluster settings once it is available."));
    }

    private void apply(OfficialOpensearchClient client, Set<String> parameters) {
        final Map<String, String> current = persistentSettings(client);
        final Map<String, String> changes = changes(managedConfigurationApplier.liveValues(), parameters, current);
        if (changes.isEmpty()) {
            LOG.debug("The cluster settings stored through the web interface are applied already.");
            return;
        }

        final Map<String, JsonData> settings = new LinkedHashMap<>();
        changes.forEach((name, value) -> settings.put(name, JsonData.of(value == null ? JsonValue.NULL : value)));
        client.sync(c -> c.cluster().putSettings(request -> request.flatSettings(true).persistent(settings)),
                "Couldn't update the cluster settings");
        LOG.info("Applied the cluster settings stored through the web interface: {}", describe(changes));
    }

    private static Map<String, String> persistentSettings(OfficialOpensearchClient client) {
        final var response = client.sync(c -> c.cluster().getSettings(request -> request.flatSettings(true)),
                "Couldn't read the cluster settings");
        final Map<String, String> settings = new LinkedHashMap<>();
        response.persistent().forEach((name, value) -> settings.put(name, asString(value)));
        return settings;
    }

    @Nullable
    private static String asString(@Nullable JsonData data) {
        if (data == null) {
            return null;
        }
        final JsonValue value = data.toJson();
        if (value instanceof JsonString string) {
            return string.getString();
        }
        return value == JsonValue.NULL ? null : value.toString();
    }

    /**
     * The persistent settings to write so that they match the stored values.
     *
     * @param storedValues the stored values, keyed by setting
     * @param managed      all settings applied live: those without a stored value are reset
     * @param current      the persistent cluster settings as they are now, with flat keys
     * @return the settings to write, with {@code null} for the settings to reset
     */
    static Map<String, String> changes(Map<String, String> storedValues,
                                       Set<String> managed,
                                       Map<String, String> current) {
        final Map<String, String> changes = new LinkedHashMap<>();
        for (final String name : new TreeSet<>(managed)) {
            final String stored = storedValues.get(name);
            final String currentValue = current.get(name);
            if (stored == null ? currentValue != null : !Objects.equals(stored.trim(), currentValue)) {
                changes.put(name, stored == null ? null : stored.trim());
            }
        }
        return changes;
    }

    private static String describe(Map<String, String> changes) {
        final Map<String, String> described = new LinkedHashMap<>();
        changes.forEach((name, value) -> described.put(name, value == null ? "<reset>" : value));
        return described.toString();
    }
}
