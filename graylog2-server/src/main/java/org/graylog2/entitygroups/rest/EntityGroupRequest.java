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
package org.graylog2.entitygroups.rest;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.graylog2.entitygroups.model.EntityGroup;

import javax.annotation.Nullable;

import java.util.Map;
import java.util.Set;

import static com.google.common.base.MoreObjects.firstNonNull;
import static org.graylog2.entitygroups.model.EntityGroup.FIELD_ENTITIES;
import static org.graylog2.entitygroups.model.EntityGroup.FIELD_NAME;

public record EntityGroupRequest(
        @JsonProperty(FIELD_NAME)
        String name,
        @Nullable
        @JsonProperty(FIELD_ENTITIES)
        Map<String, Set<String>> entities
) {
    public EntityGroup toEntityGroup() {
        return EntityGroup.builder()
                .name(name())
                .entities(firstNonNull(entities(), Map.of()))
                .build();
    }
}
