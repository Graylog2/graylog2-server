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
package org.graylog2.entitygroups.model;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public record BulkEntityGroupRequest(
        @JsonProperty(FIELD_ENTITY_TYPE)
        String type,
        @JsonProperty(FIELD_ENTITY_IDS)
        List<String> entityIds
) {
    public static final String FIELD_ENTITY_TYPE = "entity_type";
    public static final String FIELD_ENTITY_IDS = "entity_IDS";
}
