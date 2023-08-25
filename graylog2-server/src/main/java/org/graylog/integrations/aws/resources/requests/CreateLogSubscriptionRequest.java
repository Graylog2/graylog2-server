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
@JsonDeserialize(builder = CreateLogSubscriptionRequest.Builder.class)
public abstract class CreateLogSubscriptionRequest implements AWSRequest {

    private static final String LOG_GROUP_NAME = "log_group_name";
    private static final String FILTER_NAME = "filter_name";
    private static final String FILTER_PATTERN = "filter_pattern";
    private static final String DESTINATION_STREAM_ARN = "destination_stream_arn";
    private static final String ROLE_ARN = "role_arn";

    /**
     * {@see <a href="https://docs.aws.amazon.com/AmazonCloudWatch/latest/logs/SubscriptionFilters.html">CloudWatch Subscription Filter</a>},
     */
    @JsonProperty(LOG_GROUP_NAME)
    public abstract String logGroupName();

    @JsonProperty(FILTER_NAME)
    public abstract String filterName();

    @JsonProperty(FILTER_PATTERN)
    public abstract String filterPattern();

    @JsonProperty(DESTINATION_STREAM_ARN)
    public abstract String destinationStreamArn();

    @JsonProperty(ROLE_ARN)
    public abstract String roleArn();

    public static Builder builder() {
        return Builder.create();
    }

    @AutoValue.Builder
    public static abstract class Builder implements AWSRequest.Builder<Builder> {
        @JsonCreator
        public static Builder create() {
            return new AutoValue_CreateLogSubscriptionRequest.Builder();
        }

        @JsonProperty(LOG_GROUP_NAME)
        public abstract Builder logGroupName(String logGroupName);

        @JsonProperty(FILTER_NAME)
        public abstract Builder filterName(String filterName);

        @JsonProperty(FILTER_PATTERN)
        public abstract Builder filterPattern(String filterPattern);

        @JsonProperty(DESTINATION_STREAM_ARN)
        public abstract Builder destinationStreamArn(String destinationStreamArn);

        @JsonProperty(ROLE_ARN)
        public abstract Builder roleArn(String roleArn);

        public abstract CreateLogSubscriptionRequest build();
    }
}