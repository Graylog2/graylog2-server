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
package org.graylog2.decorators;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Map;
import java.util.Optional;

@JsonAutoDetect
public interface Decorator {
    String FIELD_ID = "id";
    String FIELD_TYPE = "type";
    String FIELD_CONFIG = "config";
    String FIELD_STREAM = "stream";
    String FIELD_ORDER = "order";

    @JsonProperty(FIELD_ID)
    String id();
    @JsonProperty(FIELD_TYPE)
    String type();
    @JsonProperty(FIELD_STREAM)
    Optional<String> stream();
    @JsonProperty(FIELD_CONFIG)
    Map<String, Object> config();
    @JsonProperty(FIELD_ORDER)
    int order();
}
