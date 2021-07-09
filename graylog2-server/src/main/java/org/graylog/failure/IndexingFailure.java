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

import org.joda.time.DateTime;

import javax.annotation.Nullable;

public class IndexingFailure implements Failure {

    private final String messageId;
    private final String targetIndex;
    private final String errorType;
    private final String errorMessage;
    private final DateTime timestamp;
    private final String messageJson;

    public IndexingFailure(String messageId,
                           String targetIndex,
                           String errorType,
                           String errorMessage,
                           DateTime timestamp,
                           String messageJson) {
        this.messageId = messageId;
        this.targetIndex = targetIndex;
        this.errorType = errorType;
        this.errorMessage = errorMessage;
        this.timestamp = timestamp;
        this.messageJson = messageJson;
    }

    @Override
    public String failureType() {
        return "indexing";
    }

    @Override
    public String messageId() {
        return messageId;
    }

    @Nullable
    @Override
    public String targetIndex() {
        return targetIndex;
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
    public String messageJson() {
        return messageJson;
    }
}
