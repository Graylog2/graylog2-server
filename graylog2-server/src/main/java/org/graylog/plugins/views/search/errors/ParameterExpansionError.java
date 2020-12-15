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

import com.fasterxml.jackson.annotation.JsonProperty;

public class ParameterExpansionError implements SearchError {
    private final String parameterName;
    private final String description;

    public ParameterExpansionError(String name) {
        this.parameterName = name;
        this.description = "Error while expanding parameter <" + parameterName + ">";
    }

    public ParameterExpansionError(String name, String msg) {
        this.parameterName = name;
        this.description = "Error while expanding parameter <" + parameterName + ">: " + msg;
    }

    @JsonProperty("parameter")
    public String parameterName() {
        return parameterName;
    }

    @Override
    public String description() {
        return description;
    }
}
