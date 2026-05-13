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
 * Descriptor for entity metric fields that are computed fresh on every request.
 * Use this for cheap queries (e.g. domain model lookups) where caching is unnecessary.
 * <p>
 * The {@link SearchUser} is passed to {@link #computeField} so that implementations
 * can filter results by the user's permissions (e.g. count only pipelines the user can read).
 * </p>
 */
public interface EntityUncachedMetricsDescriptor extends EntityMetricsDescriptor {

    /**
     * Computes fresh values for the given entity IDs, filtered by the user's permissions.
     *
     * @param entityIds  the entity IDs to compute values for
     * @param searchUser the current user for permission filtering
     * @return map of entity ID to computed value
     */
    Map<String, Object> computeField(Collection<String> entityIds, SearchUser searchUser);
}
