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
package org.graylog2.configuration;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.auto.value.AutoValue;
import org.graylog.autovalue.WithBeanGetter;
import org.graylog2.plugin.PluginConfigBean;

import javax.annotation.Nullable;

@JsonAutoDetect
@AutoValue
@WithBeanGetter
public abstract class IndexDefaultsConfiguration implements PluginConfigBean {

    public static final String SHARDS = "shards";
    public static final String REPLICAS = "replicas";
    public static final int DEFAULT_SHARDS = 4;
    public static final int DEFAULT_REPLICAS = 0;

    @Nullable
    @JsonProperty(SHARDS)
    public abstract Integer shards();

    @Nullable
    @JsonProperty(REPLICAS)
    public abstract Integer replicas();

    @JsonCreator
    public static IndexDefaultsConfiguration create(@JsonProperty(SHARDS) Integer shards,
                                                    @JsonProperty(REPLICAS) Integer replicas) {
        return builder()
                .shards(shards)
                .replicas(replicas)
                .build();
    }

    public static Builder builder() {
        return new AutoValue_IndexDefaultsConfiguration.Builder()
                .shards(DEFAULT_SHARDS)
                .replicas(DEFAULT_REPLICAS);
    }

    public static IndexDefaultsConfiguration createNew() {
        return builder().build();
    }

    @AutoValue.Builder
    public abstract static class Builder {
        public abstract Builder shards(int shards);

        public abstract Builder replicas(int replicas);

        public abstract IndexDefaultsConfiguration build();
    }
}
