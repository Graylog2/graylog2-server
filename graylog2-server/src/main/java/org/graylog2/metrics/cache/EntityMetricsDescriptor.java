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
package org.graylog2.metrics.cache;

import org.graylog.plugins.views.search.permissions.SearchUser;

import java.util.Collection;
import java.util.Map;

/**
 * Pluggable descriptor for a single entity metric field. Defines the entity type,
 * field name, and how the value is computed.
 * <p>
 * Multiple descriptors can be registered for the same entity type via Guice multibinding.
 * Each descriptor handles exactly one field. Authorization is handled internally by each
 * descriptor — either by filtering results in {@link #computeField} or by checking
 * feature flags / licenses before returning data.
 * </p>
 *
 * @see EntityCachedMetricsDescriptor for fields that should be cached in MongoDB
 */
public interface EntityMetricsDescriptor {

    /**
     * The entity type identifier (e.g. "inputs", "streams").
     * Should match the MongoDB collection name from {@code @DbEntity}.
     */
    String entityType();

    /**
     * The field name this descriptor provides (e.g. "message_count", "pipeline_count").
     */
    String fieldName();

    /**
     * Computes fresh values for the given entity IDs.
     * Implementations should handle their own authorization (e.g. filtering results
     * by per-entity permissions, checking feature flags).
     *
     * @param entityIds  the entity IDs to compute values for
     * @param searchUser the current user for search and entity permissions
     * @return map of entity ID to computed value
     */
    Map<String, Object> computeField(Collection<String> entityIds, SearchUser searchUser);
}
