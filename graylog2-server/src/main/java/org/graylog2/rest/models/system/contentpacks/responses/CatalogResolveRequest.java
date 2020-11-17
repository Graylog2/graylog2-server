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
package org.graylog2.rest.models.system.contentpacks.responses;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.auto.value.AutoValue;
import com.google.common.collect.ImmutableSet;
import org.graylog2.contentpacks.model.entities.EntityDescriptor;

import java.util.Set;

@JsonAutoDetect
@AutoValue
public abstract class CatalogResolveRequest {
    @JsonProperty("entities")
    public abstract ImmutableSet<EntityDescriptor> entities();

    @JsonCreator
    public static CatalogResolveRequest create(@JsonProperty("entities") Set<EntityDescriptor> entities) {
        return new AutoValue_CatalogResolveRequest(ImmutableSet.copyOf(entities));
    }
}
