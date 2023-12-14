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
package org.graylog.plugins.views.search.engine.monitoring.data.histogram;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeName;

import java.util.ArrayList;
import java.util.List;

import static org.graylog.plugins.views.search.engine.monitoring.data.histogram.Bin.SINGLE_BIN_TYPE;

@JsonTypeName(SINGLE_BIN_TYPE)
public record SingleValueBin<D extends BinDefinition>(@JsonProperty D binDefinition,
                                                      @JsonProperty Number value) implements Bin<D> {

    @JsonIgnore
    public List<String> toDataLine() {
        final List<String> definition = binDefinition.description();
        final List<String> result = new ArrayList<>(definition.size() + 1);
        result.addAll(definition);
        result.add(value.toString());
        return result;
    }
}
