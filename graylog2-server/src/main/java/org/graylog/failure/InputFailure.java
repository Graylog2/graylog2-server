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
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.graylog2.plugin.journal.RawMessage;
import org.joda.time.DateTime;

import java.util.Optional;

import static org.graylog2.plugin.Message.FIELD_GL2_SOURCE_INPUT;
import static org.graylog2.plugin.Message.FIELD_GL2_SOURCE_NODE;
import static org.graylog2.plugin.Message.FIELD_SOURCE;

public class InputFailure implements Failure {

    private final FailureCause failureCause;
    private final String failureMessage;
    private final String failureDetails;
    private final DateTime failureTimestamp;
    private final RawMessage rawMessage;
    private final String originalMessage;

    public InputFailure(@Nonnull FailureCause failureCause,
                        @Nonnull String failureMessage,
                        @Nonnull String failureDetails,
                        @Nonnull DateTime failureTimestamp,
                        @Nonnull RawMessage rawMessage,
                        @Nonnull String originalMessage) {
        this.failureCause = failureCause;
        this.failureMessage = failureMessage;
        this.failureDetails = failureDetails;
        this.failureTimestamp = failureTimestamp;
        this.rawMessage = rawMessage;
        this.originalMessage = originalMessage;
    }

    @Override
    public FailureType failureType() {
        return FailureType.INPUT;
    }

    @Override
    public FailureCause failureCause() {
        return failureCause;
    }

    @Override
    public String message() {
        return failureMessage;
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
        return false;
    }

    @Nonnull
    @Override
    public String messageId() {
        return rawMessage.getId().toString();
    }

    @Nonnull
    @Override
    public DateTime messageTimestamp() {
        return rawMessage.getTimestamp();
    }

    @Nonnull
    @Override
    public FailureObjectBuilder failureObjectBuilder(ObjectMapper objectMapper, @Nonnull Meter invalidTimestampMeter, boolean includeFailedMessage) {
        FailureObjectBuilder builder = new FailureObjectBuilder(this);
        rawMessage.getLastSourceNode().ifPresent(sourceNode -> builder
                .put(FIELD_GL2_SOURCE_INPUT, sourceNode.inputId)
                .put(FIELD_GL2_SOURCE_NODE, sourceNode.nodeId)
        );
        Optional.ofNullable(rawMessage.getRemoteAddress()).ifPresent(address ->
                builder.put(FIELD_SOURCE, address.toString()));

        if (includeFailedMessage) {
            builder.put(FIELD_FAILED_MESSAGE, originalMessage);
        }

        return builder;
    }

    @Nullable
    @Override
    public Object getMessageQueueId() {
        return null;
    }
}
