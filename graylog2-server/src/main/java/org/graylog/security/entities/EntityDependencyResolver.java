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
package org.graylog.security.entities;

import com.google.common.collect.ImmutableSet;
import org.graylog.grn.GRN;

/**
 * Resolves dependencies for entities identified by GRNs (Global Resource Names).
 * <p>
 * Implementations of this interface provide methods to determine which other entities
 * a given entity depends on.
 */
public interface EntityDependencyResolver {
    /**
     * Resolves the dependencies for the given entity. Dependencies are other entities which require a capability grant
     * for the grantee to be present in order for the grantee to make effective use of the entity being shared with
     * them.
     *
     * @param entity the GRN of the entity whose dependencies should be resolved
     * @return an immutable set of {@link EntityDescriptor} representing the dependencies of the entity
     */
    ImmutableSet<EntityDescriptor> resolve(GRN entity);

    /**
     * Creates an {@link EntityDescriptor} from the given GRN.
     * <p>
     * TODO: this interface is not a good fit for this method, consider moving it.
     *
     * @param entity the GRN of the entity
     * @return an {@link EntityDescriptor} for the given entity
     */
    EntityDescriptor descriptorFromGRN(GRN entity);
}
