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

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeSet;

import static org.graylog2.shared.utilities.StringUtils.f;

/**
 * Applies the stored values of the opensearch settings which can be changed without a restart, as persistent
 * cluster settings. They hold for the whole cluster, so each data node applying them only differs in who gets there
 * first: only settings whose persistent value differs from the stored one are written.
 * <p>
 * Values are applied whenever a {@link ManagedConfigurationChangedEvent} concerns them, and again whenever the
 * opensearch process becomes available: an event can be missed, or arrive while the process is down. After a failed
 * attempt, every health check of the available process tries again until an attempt succeeds.
 * <p>
 * Settings are written one by one, so that a value opensearch refuses doesn't keep the others from taking effect.
 * The outcome is recorded with {@link ManagedConfigurationApplier#recordLiveSettingsResult(String)}, which is how a
 * refused value becomes visible in the web interface.
 * <p>
 * A persistent value for a setting nothing has ever been stored for was set by other means, typically through the
 * cluster settings API before the setting was managed here. It is adopted as the stored value instead of being reset.
 * Once a value has been stored, the database is the only source: removing the stored value resets the setting, so
 * that opensearch falls back to the value of its configuration file or its own default.
 */
@Singleton
public class ManagedClusterSettingsApplier implements StateMachineTracer<OpensearchState, OpensearchEvent> {
    private static final Logger LOG = LoggerFactory.getLogger(ManagedClusterSettingsApplier.class);

    private final OpensearchProcess process;
    private final ManagedConfigurationApplier managedConfigurationApplier;
    private volatile boolean retryPending = false;
    @Nullable
    private String lastError;

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
        // Health checks keep the process available, which only has to apply again after a failed attempt
        if (destination == OpensearchState.AVAILABLE && (source != destination || retryPending)) {
            apply();
        }
    }

    /**
     * Brings the persistent cluster settings in line with the stored values. Failures are logged and recorded, and
     * the next health check tries again.
     */
    public synchronized void apply() {
        final Set<String> parameters = managedConfigurationApplier.liveParameters();
        if (parameters.isEmpty()) {
            return;
        }
        final var client = process.openSearchClient();
        if (client.isEmpty()) {
            // Becoming available applies them, so there's nothing to retry
            LOG.debug("Opensearch isn't running, applying the stored cluster settings once it is available.");
            return;
        }

        String error;
        try {
            error = apply(client.get(), parameters);
        } catch (Exception e) {
            error = "Couldn't apply the stored cluster settings: " + describe(e);
            LOG.debug("Applying the stored cluster settings failed.", e);
        }
        retryPending = error != null;
        recordResult(error);
    }

    /**
     * @return a description of the settings which couldn't be applied, {@code null} if all of them have been
     */
    @Nullable
    private String apply(OfficialOpensearchClient client, Set<String> parameters) {
        final Map<String, String> current = persistentSettings(client);
        final Map<String, String> storedValues = new LinkedHashMap<>(managedConfigurationApplier.liveValues());
        adoptPersistentValues(parameters, storedValues, current);

        final Map<String, String> changes = changes(storedValues, parameters, current);
        if (changes.isEmpty()) {
            LOG.debug("The cluster settings stored through the web interface are applied already.");
            return null;
        }

        final Map<String, String> applied = new LinkedHashMap<>();
        final List<String> failures = new ArrayList<>();
        changes.forEach((name, value) -> {
            try {
                final JsonData jsonValue = JsonData.of(value == null ? JsonValue.NULL : value);
                client.sync(c -> c.cluster().putSettings(request -> request.flatSettings(true)
                                .persistent(name, jsonValue)),
                        "Couldn't update cluster setting " + name);
                applied.put(name, value);
            } catch (Exception e) {
                failures.add(f("%s = %s: %s", name, value == null ? "<reset>" : value, describe(e)));
            }
        });
        if (!applied.isEmpty()) {
            LOG.info("Applied the cluster settings stored through the web interface: {}", describeChanges(applied));
        }
        return failures.isEmpty() ? null : "Opensearch refused stored cluster settings: " + String.join("; ", failures);
    }

    /**
     * Adopts the persistent values of settings nothing has ever been stored for, so that they are kept.
     */
    private void adoptPersistentValues(Set<String> parameters,
                                       Map<String, String> storedValues,
                                       Map<String, String> current) {
        for (final String name : new TreeSet<>(parameters)) {
            final String currentValue = current.get(name);
            if (currentValue == null || storedValues.containsKey(name)) {
                continue;
            }
            if (managedConfigurationApplier.adoptLiveValue(name, currentValue)) {
                LOG.info("Adopted the persistent value <{}> of cluster setting <{}>, which has been set by other " +
                        "means. It is managed through the web interface from now on.", currentValue, name);
                storedValues.put(name, currentValue);
            }
        }
    }

    private void recordResult(@Nullable String error) {
        if (error != null && !error.equals(lastError)) {
            LOG.warn("{}. Retrying on the next health check.", error);
        } else if (error == null && lastError != null) {
            LOG.info("Applied the stored cluster settings after an earlier failure.");
        }
        if (!Objects.equals(error, lastError)) {
            managedConfigurationApplier.recordLiveSettingsResult(error);
        }
        lastError = error;
    }

    /**
     * The messages of an exception and its causes. The client wraps the reason opensearch gives, e.g. that a low
     * disk watermark is higher than the high one, into an exception of its own.
     */
    static String describe(Throwable throwable) {
        final Set<String> messages = new LinkedHashSet<>();
        for (Throwable t = throwable; t != null && messages.size() < 5; t = t.getCause()) {
            if (t.getMessage() != null && !t.getMessage().isBlank()) {
                messages.add(t.getMessage().trim());
            }
        }
        return messages.isEmpty() ? throwable.getClass().getSimpleName() : String.join(": ", messages);
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

    private static String describeChanges(Map<String, String> changes) {
        final Map<String, String> described = new LinkedHashMap<>();
        changes.forEach((name, value) -> described.put(name, value == null ? "<reset>" : value));
        return described.toString();
    }
}
