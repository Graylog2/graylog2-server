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
package org.graylog.plugins.views.search.errors;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.graylog.plugins.views.search.Query;
import org.graylog.plugins.views.search.elasticsearch.QueryParam;

import java.util.Set;

public class UnboundParameterError extends QueryError {
    private final String parameterName;
    private final Set<QueryParam> allUnknownParameters;

    public UnboundParameterError(Query query, String name, Set<QueryParam> allUnknownParameters) {
        super(query, "Unbound required parameter used: " + name);
        this.parameterName = name;
        this.allUnknownParameters = allUnknownParameters;
    }

    @JsonProperty("parameter")
    public String parameterName() {
        return parameterName;
    }

    @JsonIgnore
    public Set<QueryParam> allUnknownParameters() {
        return allUnknownParameters;
    }
}
