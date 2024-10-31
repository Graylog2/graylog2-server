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
package org.graylog.integrations.aws.cloudwatch;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.auto.value.AutoValue;
import org.graylog.autovalue.WithBeanGetter;
import org.joda.time.DateTime;

@JsonAutoDetect
@AutoValue
@WithBeanGetter
public abstract class KinesisLogEntry {

    private static final String KINESIS_STREAM = "kinesis_stream";
    private static final String LOG_GROUP = "log_group";
    private static final String LOG_STREAM = "log_stream";
    private static final String TIMESTAMP = "timestamp";
    private static final String MESSAGE = "message";

    @JsonProperty(KINESIS_STREAM)
    public abstract String kinesisStream();

    /**
     * CloudWatch Log Group and Log Stream are optional, since messages may have been written directly to Kinesis
     * without using CloudWatch. Only CloudWatch messages written VIA Kinesis CloudWatch subscriptions will
     * contain a log group and stream.
     */
    @JsonProperty(LOG_GROUP)
    public abstract String logGroup();

    @JsonProperty(LOG_STREAM)
    public abstract String logStream();

    @JsonProperty(TIMESTAMP)
    public abstract DateTime timestamp();

    @JsonProperty(MESSAGE)
    public abstract String message();

    @JsonCreator
    public static KinesisLogEntry create(@JsonProperty(KINESIS_STREAM) String kinesisStream,
                                         @JsonProperty(LOG_GROUP) String logGroup,
                                         @JsonProperty(LOG_STREAM) String logStream,
                                         @JsonProperty(TIMESTAMP) DateTime timestamp,
                                         @JsonProperty(MESSAGE) String message) {
        return new AutoValue_KinesisLogEntry(kinesisStream, logGroup, logStream, timestamp, message);
    }
}