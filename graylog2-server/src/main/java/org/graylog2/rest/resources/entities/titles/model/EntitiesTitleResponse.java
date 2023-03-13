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
package org.graylog2.rest.resources.entities.titles.model;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public record EntitiesTitleResponse(@JsonProperty("entities") List<EntityTitleResponse> entities) {

    public EntitiesTitleResponse merge(final EntitiesTitleResponse other) {
        if (other == null || other.entities == null || other.entities.isEmpty()) {
            return this;
        }
        if (this.entities.isEmpty()) {
            return other;
        }
        final Set<EntityTitleResponse> merged = new HashSet<>(entities().size() + other.entities().size());
        merged.addAll(entities());
        merged.addAll(other.entities());
        return new EntitiesTitleResponse(new ArrayList<>(merged));
    }
}
