package org.graylog.failure;

import com.codahale.metrics.Meter;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.graylog2.plugin.journal.RawMessage;
import org.joda.time.DateTime;

import java.util.Map;
import java.util.Optional;

import static org.graylog2.plugin.Message.FIELD_MESSAGE;
import static org.graylog2.plugin.Message.FIELD_SOURCE;
import static org.graylog2.plugin.Message.FIELD_STREAMS;
import static org.graylog2.plugin.Message.FIELD_TIMESTAMP;
import static org.graylog2.plugin.Tools.buildElasticSearchTimeFormat;
import static org.graylog2.plugin.streams.Stream.FAILURES_STREAM_ID;

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
    public Map<String, Object> toElasticSearchObject(ObjectMapper objectMapper, @NonNull Meter invalidTimestampMeter, boolean includeFailedMessage) {
        final ImmutableMap.Builder<String, Object> esObject = ImmutableMap.<String, Object>builder()
                .put(FIELD_MESSAGE, message())
                .put(FIELD_STREAMS, ImmutableList.of(FAILURES_STREAM_ID))
                .put(FIELD_TIMESTAMP, buildElasticSearchTimeFormat(failureTimestamp()))

                .put(FIELD_FAILURE_TYPE, failureType().toString())
                .put(FIELD_FAILURE_CAUSE, failureCause().label())
                .put(FIELD_FAILURE_DETAILS, failureDetails())

                .put(FIELD_FAILED_MESSAGE_ID, messageId())
                .put(FIELD_FAILED_MESSAGE_TIMESTAMP, buildElasticSearchTimeFormat(rawMessage.getTimestamp()));

        Optional.ofNullable(rawMessage.getRemoteAddress()).ifPresent(address ->
                esObject.put(FIELD_SOURCE, address.toString()));

        if (includeFailedMessage) {
            esObject.put(FIELD_FAILED_MESSAGE, originalMessage);
        }

        return esObject.build();
    }

    @Nullable
    @Override
    public Object getMessageQueueId() {
        return null;
    }
}
