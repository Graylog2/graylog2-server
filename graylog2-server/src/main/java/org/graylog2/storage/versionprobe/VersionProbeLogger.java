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
package org.graylog2.storage.versionprobe;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.slf4j.Logger;

public class VersionProbeLogger implements VersionProbeListener {

    private final Logger logger;

    public VersionProbeLogger(Logger logger) {
        this.logger = logger;
    }

    @Override
    public void onRetry(long attemptNumber, long connectionAttempts, @Nullable Throwable cause) {
        if (connectionAttempts == 0) {
            logger.info("Indexer is not available. Retry #{}", attemptNumber);
        } else {
            logger.info("Indexer is not available. Retry #{}/{}", attemptNumber, connectionAttempts);
        }
    }

    @Override
    public void onError(@Nonnull String message, @Nullable Throwable cause) {
        if (cause != null) {
            logger.error(message, cause);
        } else {
            logger.error(message);
        }
    }
}
