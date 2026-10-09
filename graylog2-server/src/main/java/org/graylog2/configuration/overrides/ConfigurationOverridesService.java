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
package org.graylog2.configuration.overrides;

import com.mongodb.client.model.DeleteOneModel;
import com.mongodb.client.model.IndexOptions;
import com.mongodb.client.model.Indexes;
import com.mongodb.client.model.UpdateOneModel;
import com.mongodb.client.model.UpdateOptions;
import com.mongodb.client.model.Updates;
import com.mongodb.client.model.WriteModel;
import jakarta.annotation.Nullable;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import org.bson.conversions.Bson;
import org.graylog2.database.MongoCollection;
import org.graylog2.database.MongoCollections;
import org.graylog2.events.ClusterEventBus;
import org.graylog2.plugin.cluster.ClusterConfigService;

import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import static com.mongodb.client.model.Filters.and;
import static com.mongodb.client.model.Filters.eq;
import static org.graylog2.configuration.overrides.NodeConfigurationOverride.FIELD_NAME;
import static org.graylog2.configuration.overrides.NodeConfigurationOverride.FIELD_NODE_ID;
import static org.graylog2.configuration.overrides.NodeConfigurationOverride.FIELD_NODE_TYPE;
import static org.graylog2.configuration.overrides.NodeConfigurationOverride.FIELD_UPDATED_AT;
import static org.graylog2.configuration.overrides.NodeConfigurationOverride.FIELD_UPDATED_BY;
import static org.graylog2.configuration.overrides.NodeConfigurationOverride.FIELD_VALUE;

/**
 * Reads and writes configuration overrides, which take precedence over the values from the configuration file,
 * environment variables and system properties.
 * <p>
 * Overrides apply either to all nodes of a {@link NodeType} or to a single node. Overrides for all nodes of a type are
 * stored as {@link NodeTypeConfigurationOverrides} cluster config. Overrides for a single node are stored in their own
 * collection, one document per node and parameter.
 * <p>
 * Values are stored as raw strings and are neither validated nor checked against the parameters of a node.
 */
@Singleton
public class ConfigurationOverridesService {
    public static final String COLLECTION_NAME = "node_configuration_overrides";

    private final MongoCollection<NodeConfigurationOverride> collection;
    private final ClusterConfigService clusterConfigService;
    private final ClusterEventBus clusterEventBus;
    private final Clock clock;

    @Inject
    public ConfigurationOverridesService(MongoCollections mongoCollections,
                                         ClusterConfigService clusterConfigService,
                                         ClusterEventBus clusterEventBus,
                                         Clock clock) {
        this.collection = mongoCollections.collection(COLLECTION_NAME, NodeConfigurationOverride.class);
        this.clusterConfigService = clusterConfigService;
        this.clusterEventBus = clusterEventBus;
        this.clock = clock;

        collection.createIndex(Indexes.ascending(FIELD_NODE_ID, FIELD_NAME), new IndexOptions().unique(true));
    }

    /**
     * Returns the overrides for all nodes of the given type, or for a single node if a node ID is given.
     *
     * @return the overrides, keyed and sorted by parameter name
     */
    public Map<String, ConfigurationOverrideValue> getOverrides(NodeType nodeType, @Nullable String nodeId) {
        if (nodeId == null) {
            return sorted(getNodeTypeOverrides().forNodeType(nodeType));
        }

        final Map<String, ConfigurationOverrideValue> overrides = new TreeMap<>();
        collection.find(and(eq(FIELD_NODE_ID, nodeId), eq(FIELD_NODE_TYPE, nodeType.value())))
                .forEach(override -> overrides.put(override.name(), new ConfigurationOverrideValue(
                        override.value(), override.updatedAt(), override.updatedBy())));
        return Collections.unmodifiableMap(overrides);
    }

    /**
     * Changes the overrides for all nodes of the given type, or for a single node if a node ID is given. Overrides
     * which aren't part of the changes stay untouched.
     *
     * @param changes The new values keyed by parameter name. A {@code null} value removes the override.
     * @return the overrides after the change, keyed and sorted by parameter name
     */
    public Map<String, ConfigurationOverrideValue> updateOverrides(NodeType nodeType,
                                                                   @Nullable String nodeId,
                                                                   Map<String, String> changes,
                                                                   String updatedBy) {
        if (!changes.isEmpty()) {
            if (nodeId == null) {
                updateNodeTypeOverrides(nodeType, changes, updatedBy);
            } else {
                updateNodeOverrides(nodeType, nodeId, changes, updatedBy);
            }
        }
        return getOverrides(nodeType, nodeId);
    }

    private NodeTypeConfigurationOverrides getNodeTypeOverrides() {
        return clusterConfigService.getOrDefault(NodeTypeConfigurationOverrides.class, NodeTypeConfigurationOverrides.empty());
    }

    // Writing cluster config replaces the whole document, so concurrent updates on this node are serialized. Concurrent
    // updates on different nodes may still overwrite each other.
    private synchronized void updateNodeTypeOverrides(NodeType nodeType, Map<String, String> changes, String updatedBy) {
        final NodeTypeConfigurationOverrides overrides = getNodeTypeOverrides();
        final Map<String, ConfigurationOverrideValue> values = new HashMap<>(overrides.forNodeType(nodeType));
        final Instant now = Instant.now(clock);
        changes.forEach((name, value) -> {
            if (value == null) {
                values.remove(name);
            } else {
                values.put(name, new ConfigurationOverrideValue(value, now, updatedBy));
            }
        });
        clusterConfigService.write(overrides.withNodeType(nodeType, values));
    }

    private void updateNodeOverrides(NodeType nodeType, String nodeId, Map<String, String> changes, String updatedBy) {
        final Date now = Date.from(Instant.now(clock));
        final List<WriteModel<NodeConfigurationOverride>> writes = new ArrayList<>(changes.size());
        changes.forEach((name, value) -> {
            if (value == null) {
                writes.add(new DeleteOneModel<>(nodeOverrideFilter(nodeId, name)));
            } else {
                writes.add(new UpdateOneModel<>(
                        nodeOverrideFilter(nodeId, name),
                        Updates.combine(
                                Updates.set(FIELD_NODE_TYPE, nodeType.value()),
                                Updates.set(FIELD_VALUE, value),
                                Updates.set(FIELD_UPDATED_AT, now),
                                Updates.set(FIELD_UPDATED_BY, updatedBy)
                        ),
                        new UpdateOptions().upsert(true)
                ));
            }
        });
        collection.bulkWrite(writes);
        clusterEventBus.post(new NodeConfigurationOverridesChangedEvent(nodeId, Set.copyOf(changes.keySet())));
    }

    private static Bson nodeOverrideFilter(String nodeId, String name) {
        return and(eq(FIELD_NODE_ID, nodeId), eq(FIELD_NAME, name));
    }

    private static Map<String, ConfigurationOverrideValue> sorted(Map<String, ConfigurationOverrideValue> overrides) {
        return Collections.unmodifiableMap(new TreeMap<>(overrides));
    }
}
