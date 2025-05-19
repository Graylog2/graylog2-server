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

import java.util.Collection;

public interface DependentEntitiesResolver {
    /**
     * Return dependent entities based on the provided primary entity.
     *
     * @param primaryEntity The primary entity
     * @return A collection of related entities; or empty collection, if there are none.
     */
    @Nonnull
    Collection<GRN> dependentEntities(GRN primaryEntity);
}
