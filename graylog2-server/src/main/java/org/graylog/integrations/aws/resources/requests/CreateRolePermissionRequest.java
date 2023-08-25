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
package org.graylog.integrations.aws.resources.requests;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.google.auto.value.AutoValue;
import org.graylog.autovalue.WithBeanGetter;

@AutoValue
@WithBeanGetter
@JsonDeserialize(builder = CreateRolePermissionRequest.Builder.class)
public abstract class CreateRolePermissionRequest implements AWSRequest {

    private static final String STREAM_NAME = "stream_name";
    private static final String STREAM_ARN = "stream_arn";

    @JsonProperty(STREAM_NAME)
    public abstract String streamName();

    @JsonProperty(STREAM_ARN)
    public abstract String streamArn();

    public static Builder builder() {
        return Builder.create();
    }

    @AutoValue.Builder
    public static abstract class Builder implements AWSRequest.Builder<Builder> {
        @JsonCreator
        public static Builder create() {
            return new AutoValue_CreateRolePermissionRequest.Builder();
        }

        @JsonProperty(STREAM_NAME)
        public abstract Builder streamName(String streamName);

        @JsonProperty(STREAM_ARN)
        public abstract Builder streamArn(String streamArn);

        public abstract CreateRolePermissionRequest build();
    }
}
