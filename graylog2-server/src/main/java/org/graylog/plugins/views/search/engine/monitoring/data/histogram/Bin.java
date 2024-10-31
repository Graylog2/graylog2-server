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

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

import java.util.List;

@JsonTypeInfo(
        use = JsonTypeInfo.Id.NAME,
        property = Bin.TYPE)
@JsonSubTypes({
        @JsonSubTypes.Type(value = SingleValueBin.class, name = Bin.SINGLE_BIN_TYPE),
        @JsonSubTypes.Type(value = MultiValueBin.class, name = Bin.MULTI_BIN_TYPE),
})
public interface Bin<D extends BinDefinition> {

    String TYPE = "type";
    String SINGLE_BIN_TYPE = "SingleValueBin";
    String MULTI_BIN_TYPE = "MultiValueBin";

    D binDefinition();

    List<String> toDataLine();
}
