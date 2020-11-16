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
package org.graylog.plugins.views.search;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.auto.value.AutoValue;
import com.google.common.collect.ImmutableMap;

import java.util.Map;

@AutoValue
@JsonAutoDetect
public abstract class SearchMetadata {

    @JsonProperty
    public abstract Map<String, QueryMetadata> queryMetadata();

    @JsonProperty("declared_parameters")
    public abstract ImmutableMap<String, Parameter> declaredParameters();

    public static SearchMetadata create(Map<String, QueryMetadata> queryMetadata, ImmutableMap<String, Parameter> declaredParameters) {
        return new AutoValue_SearchMetadata(queryMetadata, declaredParameters);
    }

}
