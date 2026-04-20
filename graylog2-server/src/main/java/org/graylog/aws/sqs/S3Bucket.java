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
package org.graylog.aws.sqs;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.google.auto.value.AutoValue;

@AutoValue
@JsonDeserialize(builder = S3Bucket.Builder.class)
public abstract class S3Bucket {
    private static final String BUCKET = "bucket";
    private static final String OBJECT = "object";

    @JsonProperty(BUCKET)
    public abstract JsonNode bucket();

    @JsonProperty(OBJECT)
    public abstract JsonNode object();

    public static Builder builder() {
        return Builder.create();
    }

    @AutoValue.Builder
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static abstract class Builder {

        @JsonProperty(BUCKET)

        public abstract Builder bucket(JsonNode bucket);

        @JsonProperty(OBJECT)
        public abstract Builder object(JsonNode object);

        @JsonCreator
        public static Builder create() {
            return new AutoValue_S3Bucket.Builder();
        }

        public abstract S3Bucket build();

    }
}
