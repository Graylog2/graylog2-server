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
package org.graylog2.rest.models.system.lookup;

import com.google.auto.value.AutoValue;
import com.google.common.collect.Maps;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

import org.graylog.autovalue.WithBeanGetter;

import java.util.Map;

@AutoValue
@WithBeanGetter
@JsonAutoDetect
@JsonDeserialize(builder = AutoValue_ErrorStates.Builder.class)
public abstract class ErrorStates {


    @JsonProperty("tables")
    public abstract Map<String, String> tables();

    @JsonProperty("data_adapters")
    public abstract Map<String, String> dataAdapters();

    @JsonProperty("caches")
    public abstract Map<String, String> caches();

    public static Builder builder() {
        return new AutoValue_ErrorStates.Builder()
                .caches(Maps.newHashMap())
                .tables(Maps.newHashMap())
                .dataAdapters(Maps.newHashMap());
    }


    @AutoValue.Builder
    public abstract static class Builder {

        public abstract Map<String, String> dataAdapters();
        public abstract Builder dataAdapters(Map<String, String> dataAdapters);

        public abstract Map<String, String> caches();
        public abstract Builder caches(Map<String, String> caches);

        public abstract Map<String, String> tables();
        public abstract Builder tables(Map<String, String> tables);

        public abstract ErrorStates build();
    }
}
