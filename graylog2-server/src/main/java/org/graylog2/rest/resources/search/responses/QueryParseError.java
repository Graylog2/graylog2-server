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
package org.graylog2.rest.resources.search.responses;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.auto.value.AutoValue;
import org.graylog2.plugin.rest.DetailedError;
import org.graylog2.plugin.rest.GenericError;

import javax.annotation.Nullable;
import java.util.Collection;

@JsonAutoDetect
@AutoValue
public abstract class QueryParseError implements DetailedError {
    @JsonProperty
    @Nullable
    public abstract Integer line();

    @JsonProperty
    @Nullable
    public abstract Integer column();


    public static QueryParseError create(String message,
                                         Collection<String> details,
                                         @Nullable Integer line,
                                         @Nullable Integer column) {
        return new AutoValue_QueryParseError(message, details, line, column);
    }
}
