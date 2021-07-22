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

import org.graylog2.indexer.messages.Indexable;
import org.joda.time.DateTime;

import javax.annotation.Nullable;

public class ProcessingFailure implements Failure {

    private final String failedMessageId;
    private final String errorType;
    private final String errorMessage;
    private final DateTime timestamp;
    private final Indexable failedMessage;

    public ProcessingFailure(String failedMessageId,
                             String errorType,
                             String errorMessage,
                             DateTime timestamp,
                             Indexable failedMessage) {
        this.failedMessageId = failedMessageId;
        this.errorType = errorType;
        this.errorMessage = errorMessage;
        this.timestamp = timestamp;
        this.failedMessage = failedMessage;
    }

    @Override
    public FailureType failureType() {
        return FailureType.PROCESSING;
    }

    @Override
    public String failedMessageId() {
        return failedMessageId;
    }

    @Nullable
    @Override
    public String targetIndex() {
        return null;
    }

    @Override
    public String errorType() {
        return errorType;
    }

    @Override
    public String errorMessage() {
        return errorMessage;
    }

    @Override
    public DateTime timestamp() {
        return timestamp;
    }

    @Override
    public Indexable failedMessage() {
        return failedMessage;
    }

    @Override
    public boolean requiresAcknowledgement() {
        return true;
    }
}
