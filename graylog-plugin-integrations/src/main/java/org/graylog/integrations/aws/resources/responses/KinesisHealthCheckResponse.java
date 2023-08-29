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
package org.graylog.integrations.aws.resources.responses;


import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.auto.value.AutoValue;
import org.graylog.autovalue.WithBeanGetter;
import org.graylog.integrations.aws.AWSMessageType;

import java.util.Map;

@JsonAutoDetect
@AutoValue
@WithBeanGetter
public abstract class KinesisHealthCheckResponse {

    private static final String INPUT_TYPE = "input_type";
    private static final String EXPLANATION = "explanation";
    private static final String MESSAGE_FIELDS = "message_fields";

    @JsonProperty(INPUT_TYPE)
    public abstract AWSMessageType inputType();

    @JsonProperty(EXPLANATION)
    public abstract String explanation();

    @JsonProperty(MESSAGE_FIELDS)
    public abstract Map<String, Object> messageFields();

    public static KinesisHealthCheckResponse create(@JsonProperty(INPUT_TYPE) AWSMessageType inputType,
                                                    @JsonProperty(EXPLANATION) String explanation,
                                                    @JsonProperty(MESSAGE_FIELDS) Map<String, Object> messageFields) {
        return new AutoValue_KinesisHealthCheckResponse(inputType, explanation, messageFields);
    }
}