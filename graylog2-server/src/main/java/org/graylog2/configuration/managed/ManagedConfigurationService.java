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

import com.mongodb.MongoWriteException;
import com.mongodb.client.model.IndexOptions;
import com.mongodb.client.model.Indexes;
import com.mongodb.client.model.UpdateOneModel;
import com.mongodb.client.model.UpdateOptions;
import com.mongodb.client.model.Updates;
import com.mongodb.client.model.WriteModel;
import jakarta.annotation.Nullable;
import org.bson.conversions.Bson;
import org.graylog2.database.MongoCollection;
import org.graylog2.database.MongoCollections;
import org.graylog2.events.ClusterEventBus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Clock;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeMap;

import static com.mongodb.client.model.Filters.and;
import static com.mongodb.client.model.Filters.eq;
import static org.graylog2.configuration.managed.ManagedConfigurationEntry.FIELD_CHANGED_AT;
import static org.graylog2.configuration.managed.ManagedConfigurationEntry.FIELD_NAME;
import static org.graylog2.configuration.managed.ManagedConfigurationEntry.FIELD_NODE_ID;
import static org.graylog2.configuration.managed.ManagedConfigurationEntry.FIELD_NODE_SOURCE;
import static org.graylog2.configuration.managed.ManagedConfigurationEntry.FIELD_NODE_UPDATED_AT;
import static org.graylog2.configuration.managed.ManagedConfigurationEntry.FIELD_NODE_VALUE;
import static org.graylog2.configuration.managed.ManagedConfigurationEntry.FIELD_UPDATED_AT;
import static org.graylog2.configuration.managed.ManagedConfigurationEntry.FIELD_UPDATED_BY;
import static org.graylog2.configuration.managed.ManagedConfigurationEntry.FIELD_VALUE;
import static org.graylog2.shared.utilities.StringUtils.f;

/**
 * Stores the configuration values of one node type: one entry per parameter and node, or a single entry per parameter
 * for {@link ParameterScope#CLUSTER} parameters.
 * <p>
 * Values are stored as raw strings, exactly as they would be written in a configuration file. Whether a parameter may
 * be stored at all is decided by the {@link ConfigurationCatalog}, which this service doesn't consult: callers are
 * expected to have checked.
 */
public abstract class ManagedConfigurationService {
    private static final Logger LOG = LoggerFactory.getLogger(ManagedConfigurationService.class);

    private final MongoCollection<ManagedConfigurationEntry> collection;
    private final MongoCollection<NodeConfigurationState> stateCollection;
    private final NodeType nodeType;
    private final ClusterEventBus clusterEventBus;
    private final Clock clock;

    protected ManagedConfigurationService(MongoCollections mongoCollections,
                                          String collectionName,
                                          NodeType nodeType,
                                          ClusterEventBus clusterEventBus,
                                          Clock clock) {
        this.collection = mongoCollections.collection(collectionName, ManagedConfigurationEntry.class);
        this.nodeType = nodeType;
        this.clusterEventBus = clusterEventBus;
        this.clock = clock;

        collection.createIndex(Indexes.ascending(FIELD_NODE_ID, FIELD_NAME), new IndexOptions().unique(true));

        this.stateCollection = mongoCollections.collection(collectionName + "_state", NodeConfigurationState.class);
        stateCollection.createIndex(Indexes.ascending(NodeConfigurationState.FIELD_NODE_ID), new IndexOptions().unique(true));
    }

    public NodeType nodeType() {
        return nodeType;
    }

    /**
     * The stored entries of one node, or of the whole cluster if no node ID is given.
     *
     * @return the entries, keyed and sorted by parameter name
     */
    public Map<String, ManagedConfigurationEntry> get(@Nullable String nodeId) {
        final Map<String, ManagedConfigurationEntry> entries = new TreeMap<>();
        collection.find(eq(FIELD_NODE_ID, nodeId)).forEach(entry -> entries.put(entry.name(), entry));
        return Collections.unmodifiableMap(entries);
    }

    /**
     * Sets or removes values of one node, or of the whole cluster if no node ID is given. Parameters which aren't part
     * of the changes stay untouched.
     *
     * @param changes the new values keyed by parameter name, a {@code null} value removes the stored value
     * @return the entries of that scope after the change, keyed and sorted by parameter name
     */
    public Map<String, ManagedConfigurationEntry> update(@Nullable String nodeId,
                                                         Map<String, String> changes,
                                                         String updatedBy) {
        if (changes.isEmpty()) {
            return get(nodeId);
        }

        final Date now = Date.from(clock.instant());
        final List<WriteModel<ManagedConfigurationEntry>> writes = new ArrayList<>(changes.size());
        changes.forEach((name, value) -> {
            final Bson update = value == null
                    ? Updates.combine(
                            Updates.unset(FIELD_VALUE),
                            Updates.unset(FIELD_UPDATED_AT),
                            Updates.unset(FIELD_UPDATED_BY),
                            Updates.currentDate(FIELD_CHANGED_AT))
                    : Updates.combine(
                            Updates.set(FIELD_VALUE, value),
                            Updates.set(FIELD_UPDATED_AT, now),
                            Updates.set(FIELD_UPDATED_BY, updatedBy),
                            Updates.currentDate(FIELD_CHANGED_AT));
            writes.add(new UpdateOneModel<>(entryFilter(nodeId, name), update, new UpdateOptions().upsert(true)));
        });
        collection.bulkWrite(writes);

        clusterEventBus.post(new ManagedConfigurationChangedEvent(nodeType, nodeId, Set.copyOf(changes.keySet())));
        return get(nodeId);
    }

    /**
     * Stores the values a node resolved for itself, leaving everything set through the API untouched. Called on
     * startup and whenever a node applies a change, so that the user interface can tell a stored value which is
     * already in use from one which still needs a restart.
     */
    public void reportNodeValues(String nodeId, Map<String, ReportedValue> reportedValues) {
        if (reportedValues.isEmpty()) {
            return;
        }

        final Date now = Date.from(clock.instant());
        final List<WriteModel<ManagedConfigurationEntry>> writes = new ArrayList<>(reportedValues.size());
        reportedValues.forEach((name, reported) -> writes.add(new UpdateOneModel<>(
                entryFilter(nodeId, name),
                Updates.combine(
                        Updates.set(FIELD_NODE_VALUE, reported.value()),
                        Updates.set(FIELD_NODE_SOURCE, reported.source().value()),
                        Updates.set(FIELD_NODE_UPDATED_AT, now)),
                new UpdateOptions().upsert(true))));
        collection.bulkWrite(writes);
    }

    /**
     * Stores a value for all nodes of the type, but only if none has been stored yet.
     * <p>
     * This is how a {@link ParameterScope#CLUSTER} parameter which is backed by a configuration file gets its value:
     * the first node to start adopts what it resolved locally, fixing it for every other node. Once a value exists it
     * is never replaced here, and a value explicitly removed through the API isn't adopted again.
     *
     * @return {@code true} if the value has been stored by this call
     */
    public boolean adoptClusterValue(String name, String value, String adoptedFrom) {
        final Date now = Date.from(clock.instant());
        try {
            final var result = collection.updateOne(
                    entryFilter(null, name),
                    Updates.combine(
                            Updates.setOnInsert(FIELD_VALUE, value),
                            Updates.setOnInsert(FIELD_UPDATED_AT, now),
                            Updates.setOnInsert(FIELD_UPDATED_BY, adoptedFrom)),
                    new UpdateOptions().upsert(true));
            return result.getUpsertedId() != null;
        } catch (MongoWriteException e) {
            // Another node adopted the value at the same time and won the unique index on (node_id, name)
            LOG.debug("Value of cluster parameter <{}> has been adopted by another node.", name, e);
            return false;
        }
    }

    /**
     * The values stored through the API which apply to the given node: the ones stored for all nodes of the type,
     * replaced by the ones stored for the node itself.
     *
     * @return the stored values keyed by parameter name
     */
    public Map<String, String> storedValues(String nodeId) {
        final Map<String, String> values = new TreeMap<>();
        for (final String scope : new String[]{null, nodeId}) {
            get(scope).values().stream()
                    .filter(entry -> entry.value() != null)
                    .forEach(entry -> values.put(entry.name(), entry.value()));
        }
        return Collections.unmodifiableMap(values);
    }

    /**
     * Records that a node loaded its stored values to start its process with them. The time only becomes the
     * {@link NodeConfigurationState#appliedAt()} of the node once the process runs, see
     * {@link #markConfigurationApplied(String)}.
     */
    public void markConfigurationLoaded(String nodeId) {
        stateCollection.updateOne(
                eq(NodeConfigurationState.FIELD_NODE_ID, nodeId),
                Updates.currentDate(NodeConfigurationState.FIELD_LOADED_AT),
                new UpdateOptions().upsert(true));
    }

    /**
     * Records that the process of a node runs with the values it loaded last. Stored values which have been changed
     * between loading them and the process becoming available stay marked as not applied, because the time they were
     * loaded at counts, not the time of this call.
     */
    public void markConfigurationApplied(String nodeId) {
        configurationState(nodeId)
                .map(NodeConfigurationState::loadedAt)
                .ifPresent(loadedAt -> stateCollection.updateOne(
                        eq(NodeConfigurationState.FIELD_NODE_ID, nodeId),
                        Updates.set(NodeConfigurationState.FIELD_APPLIED_AT, Date.from(loadedAt))));
    }

    /**
     * Records whether a node managed to apply the stored settings which take effect without a restart.
     *
     * @param error why applying failed, {@code null} if it succeeded
     */
    public void recordLiveSettingsResult(String nodeId, @Nullable String error) {
        stateCollection.updateOne(
                eq(NodeConfigurationState.FIELD_NODE_ID, nodeId),
                error == null
                        ? Updates.unset(NodeConfigurationState.FIELD_LIVE_SETTINGS_ERROR)
                        : Updates.set(NodeConfigurationState.FIELD_LIVE_SETTINGS_ERROR, error),
                new UpdateOptions().upsert(true));
    }

    public Optional<NodeConfigurationState> configurationState(String nodeId) {
        return Optional.ofNullable(stateCollection.find(eq(NodeConfigurationState.FIELD_NODE_ID, nodeId)).first());
    }

    /**
     * The state of every node which reported one, keyed by node ID.
     */
    public Map<String, NodeConfigurationState> configurationStates() {
        final Map<String, NodeConfigurationState> states = new TreeMap<>();
        stateCollection.find().forEach(state -> states.put(state.nodeId(), state));
        return Collections.unmodifiableMap(states);
    }

    private Bson entryFilter(@Nullable String nodeId, String name) {
        return and(eq(FIELD_NODE_ID, nodeId), eq(FIELD_NAME, name));
    }

    @Override
    public String toString() {
        return f("%s(%s)", getClass().getSimpleName(), nodeType.value());
    }
}
