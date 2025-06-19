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
package org.graylog.failure;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;

import java.util.Map;

import static org.graylog.failure.Failure.FIELD_FAILED_MESSAGE_ID;
import static org.graylog.failure.Failure.FIELD_FAILED_MESSAGE_TIMESTAMP;
import static org.graylog.failure.Failure.FIELD_FAILURE_CAUSE;
import static org.graylog.failure.Failure.FIELD_FAILURE_DETAILS;
import static org.graylog.failure.Failure.FIELD_FAILURE_TYPE;
import static org.graylog2.plugin.Message.FIELD_MESSAGE;
import static org.graylog2.plugin.Message.FIELD_STREAMS;
import static org.graylog2.plugin.Message.FIELD_TIMESTAMP;
import static org.graylog2.plugin.Tools.buildElasticSearchTimeFormat;
import static org.graylog2.plugin.streams.Stream.FAILURES_STREAM_ID;

public class FailureObjectBuilder {

    final ImmutableMap.Builder<String, Object> esObject;


    public FailureObjectBuilder(Failure inputFailure) {
        esObject = ImmutableMap.<String, Object>builder()
                .put(FIELD_MESSAGE, inputFailure.message())
                .put(FIELD_STREAMS, ImmutableList.of(FAILURES_STREAM_ID))
                .put(FIELD_TIMESTAMP, buildElasticSearchTimeFormat(inputFailure.failureTimestamp()))
                .put(FIELD_FAILURE_TYPE, inputFailure.failureType().toString())
                .put(FIELD_FAILURE_CAUSE, inputFailure.failureCause().label())
                .put(FIELD_FAILURE_DETAILS, inputFailure.failureDetails())

                .put(FIELD_FAILED_MESSAGE_ID, inputFailure.messageId())
                .put(FIELD_FAILED_MESSAGE_TIMESTAMP, buildElasticSearchTimeFormat(inputFailure.messageTimestamp()));
    }

    public FailureObjectBuilder put(String key, Object value) {
        if (value != null) {
            esObject.put(key, value);
        }
        return this;
    }

    public Map<String, Object> build() {
        return esObject.build();
    }

}
