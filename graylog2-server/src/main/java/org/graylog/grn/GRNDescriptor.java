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
package org.graylog.grn;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.google.auto.value.AutoValue;

/**
 * A descriptor with metadata for a {@link GRN} instance.
 */
@AutoValue
@JsonDeserialize(builder = GRNDescriptor.Builder.class)
public abstract class GRNDescriptor {
    @JsonProperty("grn")
    public abstract GRN grn();

    @JsonProperty("title")
    public abstract String title();

    public static GRNDescriptor empty(GRN grn) {
        return builder().grn(grn).title(grn.toString()).build();
    }

    public static GRNDescriptor create(GRN grn, String title) {
        return builder().grn(grn).title(title).build();
    }

    public static Builder builder() {
        return Builder.create();
    }

    @AutoValue.Builder
    public abstract static class Builder {
        @JsonCreator
        public static Builder create() {
            return new AutoValue_GRNDescriptor.Builder();
        }

        @JsonProperty("grn")
        public abstract Builder grn(GRN grn);

        @JsonProperty("title")
        public abstract Builder title(String title);

        public abstract GRNDescriptor build();
    }
}
