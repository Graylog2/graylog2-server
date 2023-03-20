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
package org.graylog2.rest.resources.system.contentpacks.titles.model;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

public record EntitiesTitleResponse(@JsonProperty("entities") Set<EntityTitleResponse> entities,
                                    @JsonProperty("not_permitted_to_view") Collection<String> notPermitted) {

    public static final EntitiesTitleResponse EMPTY_RESPONSE = new EntitiesTitleResponse(Set.of(), Set.of());

    public EntitiesTitleResponse merge(final EntitiesTitleResponse other) {
        if (other == null || other.entities == null || other.entities.isEmpty()) {
            return this;
        }
        if (this.entities.isEmpty()) {
            return other;
        }
        final Set<EntityTitleResponse> mergedEntities = new HashSet<>(entities().size() + other.entities().size());
        mergedEntities.addAll(entities());
        mergedEntities.addAll(other.entities());
        final Set<String> mergedNotPermitted = new HashSet<>(notPermitted.size() + other.notPermitted.size());
        mergedNotPermitted.addAll(notPermitted());
        mergedNotPermitted.addAll(other.notPermitted());
        return new EntitiesTitleResponse(mergedEntities, mergedNotPermitted);
    }
}
