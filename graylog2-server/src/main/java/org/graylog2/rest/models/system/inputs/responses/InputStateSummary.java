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
package org.graylog2.rest.models.system.inputs.responses;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.auto.value.AutoValue;
import org.graylog.autovalue.WithBeanGetter;
import org.graylog2.rest.models.system.responses.IOStateSummary;
import org.joda.time.DateTime;

import javax.annotation.Nullable;

@JsonAutoDetect
@AutoValue
@WithBeanGetter
public abstract class InputStateSummary extends IOStateSummary {
    @JsonProperty
    public abstract InputSummary messageInput();

    @JsonCreator
    public static InputStateSummary create(@JsonProperty("id") String id,
                                           @JsonProperty("state") String state,
                                           @JsonProperty("started_at") DateTime startedAt,
                                           @JsonProperty("detailed_message") @Nullable String detailedMessage,
                                           @JsonProperty("message_input") InputSummary messageInput) {
        return new AutoValue_InputStateSummary(id, state, startedAt, detailedMessage, messageInput);
    }
}
