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
package org.graylog.collectors.db;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeName;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.google.auto.value.AutoValue;

@AutoValue
@JsonTypeName(TcpSourceConfig.TYPE_NAME)
@JsonDeserialize(builder = TcpSourceConfig.Builder.class)
public abstract class TcpSourceConfig implements SourceConfig {
    public static final String TYPE_NAME = "tcp";

    @Override
    @JsonProperty(TYPE_FIELD)
    public abstract String type();

    @JsonProperty("bind_address")
    public abstract String bindAddress();

    @JsonProperty("port")
    public abstract int port();

    @JsonProperty("framing")
    public abstract String framing();

    public static Builder builder() {
        return Builder.create();
    }

    @Override
    public void validate() {
        if (bindAddress() == null || bindAddress().isBlank()) {
            throw new IllegalArgumentException("TcpSourceConfig requires a non-blank bind_address");
        }
        if (port() < 1 || port() > 65535) {
            throw new IllegalArgumentException("TcpSourceConfig port must be between 1 and 65535");
        }
        if (framing() == null || framing().isBlank()) {
            throw new IllegalArgumentException("TcpSourceConfig requires a non-blank framing");
        }
    }

    @AutoValue.Builder
    public abstract static class Builder {
        @JsonCreator
        public static Builder create() {
            return new AutoValue_TcpSourceConfig.Builder()
                    .type(TYPE_NAME)
                    .framing("newline");
        }

        @JsonProperty(TYPE_FIELD)
        public abstract Builder type(String type);

        @JsonProperty("bind_address")
        public abstract Builder bindAddress(String bindAddress);

        @JsonProperty("port")
        public abstract Builder port(int port);

        @JsonProperty("framing")
        public abstract Builder framing(String framing);

        public abstract TcpSourceConfig build();
    }
}
