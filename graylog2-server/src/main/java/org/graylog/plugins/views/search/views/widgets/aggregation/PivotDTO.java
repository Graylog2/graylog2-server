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
package org.graylog.plugins.views.search.views.widgets.aggregation;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.google.auto.value.AutoValue;
import org.graylog.autovalue.WithBeanGetter;

import java.util.Collections;
import java.util.List;

@AutoValue
@JsonDeserialize(builder = PivotDTO.Builder.class)
@WithBeanGetter
public abstract class PivotDTO {
    private static final String FIELD_FIELD_NAME = "field";
    private static final String FIELD_FIELDS_NAME = "fields";
    private static final String FIELD_TYPE = "type";
    private static final String FIELD_CONFIG = "config";

    @JsonProperty(FIELD_FIELDS_NAME)
    public abstract List<String> fields();

    @JsonProperty(FIELD_TYPE)
    public abstract String type();

    @JsonProperty(FIELD_CONFIG)
    public abstract PivotConfigDTO config();

    abstract Builder toBuilder();

    public PivotDTO withConfig(PivotConfigDTO config) {
        return toBuilder().config(config).build();
    }

    @AutoValue.Builder
    public static abstract class Builder {
        @JsonProperty(FIELD_FIELD_NAME)
        public Builder field(String field) {
            return fields(Collections.singletonList(field));
        }

        @JsonProperty(FIELD_FIELDS_NAME)
        public abstract Builder fields(List<String> field);

        @JsonProperty(FIELD_CONFIG)
        @JsonTypeInfo(
                use = JsonTypeInfo.Id.NAME,
                include = JsonTypeInfo.As.EXTERNAL_PROPERTY,
                property = PivotDTO.FIELD_TYPE,
                visible = true)
        public abstract Builder config(PivotConfigDTO config);

        @JsonProperty(FIELD_TYPE)
        public abstract Builder type(String type);

        public abstract PivotDTO build();

        @JsonCreator
        public static Builder builder() {
            return new AutoValue_PivotDTO.Builder();
        }
    }
}
