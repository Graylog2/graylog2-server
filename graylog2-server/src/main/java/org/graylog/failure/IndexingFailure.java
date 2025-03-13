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
import org.checkerframework.checker.nullness.qual.NonNull;
import org.graylog2.indexer.messages.Indexable;
import org.graylog2.plugin.Message;
import org.graylog2.shared.messageq.Acknowledgeable;
import org.joda.time.DateTime;

import java.util.Map;

import static org.graylog2.plugin.Message.FIELD_GL2_SOURCE_INPUT;
import static org.graylog2.plugin.Message.FIELD_GL2_SOURCE_NODE;
import static org.graylog2.plugin.Message.FIELD_SOURCE;
import static org.graylog2.plugin.Message.FIELD_STREAMS;

public class IndexingFailure implements Failure {

    private final FailureCause failureCause;
    private final String message;
    private final String failureDetails;
    private final DateTime failureTimestamp;
    private final Indexable failedMessage;
    private final String targetIndex;

    public IndexingFailure(
            FailureCause failureCause,
            String message,
            String failureDetails,
            DateTime failureTimestamp,
            Indexable failedMessage,
            String targetIndex) {
        this.failureCause = failureCause;
        this.message = message;
        this.failureDetails = failureDetails;
        this.failureTimestamp = failureTimestamp;
        this.failedMessage = failedMessage;
        this.targetIndex = targetIndex;
    }

    @Override
    public FailureType failureType() {
        return FailureType.INDEXING;
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
        return targetIndex;
    }

    @Override
    public boolean requiresAcknowledgement() {
        return false;
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
                                                     @NonNull Meter invalidTimestampMeter,
                                                     boolean includeFailedMessage) {
        Map<String, Object> fields = failedMessage.toElasticSearchObject(objectMapper, invalidTimestampMeter);
        fields.put(Message.FIELD_ID, failedMessage.getId());
        fields.remove(Message.FIELD_GL2_PROCESSING_ERROR);
        FailureObjectBuilder failureObjectBuilder = new FailureObjectBuilder(this)
                .put(FIELD_FAILED_MESSAGE_STREAMS, fields.get(FIELD_STREAMS))
                .put(FIELD_SOURCE, fields.get(FIELD_SOURCE))
                .put(FIELD_GL2_SOURCE_INPUT, fields.get(FIELD_GL2_SOURCE_INPUT))
                .put(FIELD_GL2_SOURCE_NODE, fields.get(FIELD_GL2_SOURCE_NODE));

        if (includeFailedMessage) {
            failureObjectBuilder.put(FIELD_FAILED_MESSAGE, fields);
        }

        final String index = targetIndex();
        failureObjectBuilder.put(FIELD_FAILED_MESSAGE_TARGET_INDEX, index != null ? index : "UNKNOWN");

        return failureObjectBuilder;
    }

    @Nullable
    @Override
    public Object getMessageQueueId() {
        if (failedMessage instanceof Acknowledgeable a) {
            return a.getMessageQueueId();
        }
        return null;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        final IndexingFailure that = (IndexingFailure) o;
        return Objects.equal(failureCause, that.failureCause)
                && Objects.equal(message, that.message)
                && Objects.equal(failureDetails, that.failureDetails)
                && Objects.equal(failureTimestamp, that.failureTimestamp)
                && Objects.equal(failedMessage, that.failedMessage)
                && Objects.equal(targetIndex, that.targetIndex);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(failureCause, message, failureDetails, failureTimestamp, failedMessage, targetIndex);
    }
}
