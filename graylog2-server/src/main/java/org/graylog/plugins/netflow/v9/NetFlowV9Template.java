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
package org.graylog.plugins.netflow.v9;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.auto.value.AutoValue;
import com.google.common.collect.ImmutableList;

import java.util.List;

@JsonAutoDetect
@AutoValue
public abstract class NetFlowV9Template {
    @JsonProperty("template_id")
    public abstract int templateId();

    @JsonProperty("field_count")
    public abstract int fieldCount();

    @JsonProperty("definitions")
    public abstract ImmutableList<NetFlowV9FieldDef> definitions();

    @JsonCreator
    public static NetFlowV9Template create(@JsonProperty("template_id") int templateId,
                                           @JsonProperty("field_count") int fieldCount,
                                           @JsonProperty("definitions") List<NetFlowV9FieldDef> definitions) {
        return new AutoValue_NetFlowV9Template(templateId, fieldCount, ImmutableList.copyOf(definitions));
    }

}
