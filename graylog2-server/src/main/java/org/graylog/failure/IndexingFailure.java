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

import com.google.common.base.Objects;
import org.graylog2.indexer.messages.Indexable;
import org.joda.time.DateTime;

import javax.annotation.Nullable;

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

    @Override
    public Indexable failedMessage() {
        return failedMessage;
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
