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

import com.google.common.collect.ImmutableMap;
import org.graylog2.indexer.IndexFailureImpl;
import org.graylog2.indexer.IndexFailureService;

import jakarta.inject.Inject;

/**
 * A fallback failure handler, which persists submitted failures in Mongo via {@link IndexFailureService}.
 * Only indexing failures supported.
 */
public class DefaultFailureHandler implements FailureHandler {

    private final IndexFailureService indexFailureService;

    @Inject
    public DefaultFailureHandler(IndexFailureService indexFailureService) {
        this.indexFailureService = indexFailureService;
    }

    @Override
    public void handle(FailureBatch failureBatch) {
        failureBatch.getFailures().forEach(failure ->
                indexFailureService.saveWithoutValidation(new IndexFailureImpl(ImmutableMap.<String, Object>builder()
                        .put("letter_id", failure.failedMessage().getId())
                        .put("index", failure.targetIndex())
                        .put("type", failure.failureType().toString())
                        .put("message", failure.failureDetails())
                        .put("timestamp", failure.failedMessage().getTimestamp())
                        .build())));
    }

    @Override
    public boolean supports(FailureBatch failureBatch) {
        return failureBatch.containsIndexingFailures();
    }

    @Override
    public boolean isEnabled() {
        return true;
    }
}
