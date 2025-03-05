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
import org.checkerframework.checker.nullness.qual.NonNull;
import org.graylog2.shared.messageq.Acknowledgeable;
import org.joda.time.DateTime;

/**
 * A failure occurring at different stages of message processing
 * (e.g. pipeline processing, extraction, Elasticsearch indexing)
 */
public interface Failure extends Acknowledgeable {

    String FIELD_FAILURE_TYPE = "failure_type";
    String FIELD_FAILURE_CAUSE = "failure_cause";
    String FIELD_FAILURE_DETAILS = "failure_details";
    String FIELD_FAILED_MESSAGE = "failed_message";
    String FIELD_FAILED_MESSAGE_ID = "failed_message_id";
    String FIELD_FAILED_MESSAGE_STREAMS = "failed_message_streams";
    String FIELD_FAILED_MESSAGE_TIMESTAMP = "failed_message_timestamp";
    String FIELD_FAILED_MESSAGE_TARGET_INDEX = "failed_message_target_index";

    /**
     * Returns a type of this failure
     */
    FailureType failureType();

    /**
     * Returns a cause of this failure
     */
    FailureCause failureCause();

    /**
     * Returns a brief description of this failure, which
     * is supposed to answer the following 2 questions:
     * 1) WHAT has happened?
     * 2) WHICH component has caused it?
     */
    String message();

    /**
     * Returns further failure details, which are supposed
     * to answer the question "WHY this failure has happened?"
     */
    String failureDetails();

    /**
     * Returns a timestamp of this failure
     */
    DateTime failureTimestamp();

    /**
     * Returns an ElasticSearch index name targeted by
     * the failed message. For non-indexing failures
     * the value might be null.
     */
    @Nullable
    String targetIndex();

    /**
     * Returns true if the failed message must
     * be acknowledged upon failure handling
     */
    boolean requiresAcknowledgement();

    /**
     * Returns the message ID
     */
    @Nonnull
    String messageId();

    /**
     * Returns the message timestamp
     */
    @Nonnull
    DateTime messageTimestamp();

    @Nonnull
    FailureObjectBuilder failureObjectBuilder(ObjectMapper objectMapper,
                                              @NonNull Meter invalidTimestampMeter,
                                              boolean includeFailedMessage);

}
