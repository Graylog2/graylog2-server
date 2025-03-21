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

import com.codahale.metrics.Meter;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Objects;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.apache.commons.lang3.StringUtils;
import org.graylog2.plugin.Message;
import org.joda.time.DateTime;

import java.util.Map;

import static org.graylog2.plugin.Message.FIELD_GL2_SOURCE_INPUT;
import static org.graylog2.plugin.Message.FIELD_GL2_SOURCE_NODE;
import static org.graylog2.plugin.Message.FIELD_SOURCE;
import static org.graylog2.plugin.Message.FIELD_STREAMS;

public class ProcessingFailure implements Failure {

    private final FailureCause failureCause;
    private final String message;
    private final String failureDetails;
    private final DateTime failureTimestamp;
    private final Message failedMessage;
    private final boolean requiresAcknowledgement;

    public ProcessingFailure(
            FailureCause failureCause,
            String message,
            String failureDetails,
            DateTime failureTimestamp,
            Message failedMessage,
            boolean requiresAcknowledgement) {
        this.failureCause = failureCause;
        this.message = message;
        this.failureDetails = failureDetails;
        this.failureTimestamp = failureTimestamp;
        this.failedMessage = failedMessage;
        this.requiresAcknowledgement = requiresAcknowledgement;
    }

    @Override
    public FailureType failureType() {
        return FailureType.PROCESSING;
    }

    @Override
    public FailureCause failureCause() {
        return failureCause;
    }

    @Override
    public String message() {
        return message;
    }

    @Override
    public String failureDetails() {
        return failureDetails;
    }

    @Override
    public DateTime failureTimestamp() {
        return failureTimestamp;
    }

    @Nullable
    @Override
    public String targetIndex() {
        return null;
    }

    @Override
    public boolean requiresAcknowledgement() {
        return requiresAcknowledgement;
    }

    @Nonnull
    @Override
    public String messageId() {
        return StringUtils.isBlank(failedMessage.getMessageId()) ? failedMessage.getId() : failedMessage.getMessageId();
    }

    @Nonnull
    @Override
    public DateTime messageTimestamp() {
        return failedMessage.getTimestamp();
    }

    @Nonnull
    @Override
    public FailureObjectBuilder failureObjectBuilder(ObjectMapper objectMapper,
                                                     @Nonnull Meter invalidTimestampMeter,
                                                     boolean includeFailedMessage) {
        Map<String, Object> fields = failedMessage.toElasticSearchObject(objectMapper, invalidTimestampMeter);
        fields.put(Message.FIELD_ID, failedMessage.getId());
        fields.remove(Message.FIELD_GL2_PROCESSING_ERROR);

        FailureObjectBuilder builder = new FailureObjectBuilder(this)
                .put(FIELD_FAILED_MESSAGE_STREAMS, fields.get(FIELD_STREAMS))
                .put(FIELD_SOURCE, fields.get(FIELD_SOURCE))
                .put(FIELD_GL2_SOURCE_INPUT, fields.get(FIELD_GL2_SOURCE_INPUT))
                .put(FIELD_GL2_SOURCE_NODE, fields.get(FIELD_GL2_SOURCE_NODE));

        if (includeFailedMessage) {
            builder.put(FIELD_FAILED_MESSAGE, fields);
        }

        return builder;
    }

    @Nullable
    @Override
    public Object getMessageQueueId() {
        return failedMessage.getMessageQueueId();
    }


    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        final ProcessingFailure that = (ProcessingFailure) o;
        return requiresAcknowledgement == that.requiresAcknowledgement
                && Objects.equal(failureCause, that.failureCause)
                && Objects.equal(message, that.message)
                && Objects.equal(failureDetails, that.failureDetails)
                && Objects.equal(failureTimestamp, that.failureTimestamp)
                && Objects.equal(failedMessage, that.failedMessage);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(failureCause, message, failureDetails, failureTimestamp, failedMessage, requiresAcknowledgement);
    }
}
