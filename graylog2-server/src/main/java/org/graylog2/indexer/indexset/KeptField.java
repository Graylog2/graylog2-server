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
package org.graylog2.indexer.indexset;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Map;

/**
 * A field that stays mapped after dynamic mapping is turned off, with its mapping in the search backend.
 * The name is the full dotted path of the field.
 */
public record KeptField(@JsonProperty(KeptField.FIELD_FIELD_NAME) String fieldName,
                        @JsonProperty(KeptField.FIELD_MAPPING) Map<String, Object> mapping) {

    public static final String FIELD_FIELD_NAME = "field_name";
    public static final String FIELD_MAPPING = "mapping";
}
