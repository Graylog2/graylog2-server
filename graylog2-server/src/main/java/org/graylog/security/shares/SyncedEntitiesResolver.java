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
package org.graylog.security.shares;

import jakarta.annotation.Nonnull;
import org.graylog.grn.GRN;

import java.util.Set;

public interface SyncedEntitiesResolver {
    /**
     * Return a set of entities that are to be kept in sync with the given entity.
     * The primary use case is to keep sharing of closely coupled entities in sync; specifically Sigma
     * rules and event definitions.
     *
     * @param primaryEntity The primary entity
     * @return A set of related entities; or empty set, if there are none.
     */
    @Nonnull
    Set<GRN> syncedEntities(GRN primaryEntity);
}
