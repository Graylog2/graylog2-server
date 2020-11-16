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
package org.graylog2.rest.models.system.loggers.responses;


import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.auto.value.AutoValue;
import org.graylog.autovalue.WithBeanGetter;
import org.joda.time.DateTime;

import javax.annotation.Nullable;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import java.util.Map;

@AutoValue
@WithBeanGetter
@JsonAutoDetect
public abstract class InternalLogMessage {
    @JsonProperty
    @NotEmpty
    public abstract String message();

    @JsonProperty("class_name")
    @NotEmpty
    public abstract String className();

    @JsonProperty
    @NotEmpty
    public abstract String level();

    @JsonProperty
    @Nullable
    public abstract String marker();

    @JsonProperty
    @NotNull
    public abstract DateTime timestamp();

    @JsonProperty
    @Nullable
    public abstract String throwable();

    @JsonProperty("thread_name")
    @NotEmpty
    public abstract String threadName();

    @JsonProperty
    @NotNull
    public abstract Map<String, String> context();

    @JsonCreator
    public static InternalLogMessage create(@JsonProperty("message") @NotEmpty String message,
                                            @JsonProperty("class_name") @NotEmpty String className,
                                            @JsonProperty("level") @NotEmpty String level,
                                            @JsonProperty("marker") @Nullable String marker,
                                            @JsonProperty("timestamp") @NotNull DateTime timestamp,
                                            @JsonProperty("throwable") @Nullable String throwable,
                                            @JsonProperty("thread_name") @NotEmpty String threadName,
                                            @JsonProperty("context") @NotNull Map<String, String> context) {
        return new AutoValue_InternalLogMessage(message, className, level, marker, timestamp, throwable, threadName, context);
    }

}
