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
package org.graylog2.bootstrap.preflight.web.resources;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.graylog2.storage.versionprobe.VersionProbeListener;

import java.util.LinkedList;
import java.util.List;

public class VersionProbeMessageCollector implements VersionProbeListener {

    private final VersionProbeListener delegate;
    private final List<String> messages = new LinkedList<>();

    public VersionProbeMessageCollector(VersionProbeListener delegate) {
        this.delegate = delegate;
    }

    public void onRetry(long attemptNumber, long configuredAttempts, @Nullable Throwable cause) {
        delegate.onRetry(attemptNumber, configuredAttempts, cause);
        if (cause != null) {
            messages.add(cause.getMessage());
        }
    }

    @Override
    public void onError(@Nonnull String message, @Nullable Throwable cause) {
        delegate.onError(message, cause);
        if (cause != null) {
            messages.add(message + ": " + cause.getMessage());
        } else {
            messages.add(message);
        }
    }

    public List<String> getMessages() {
        return messages;
    }

    /**
     * @return messages joined and delimited by ";", null if there are no messages
     */
    @Nullable
    public String joinedMessages() {
        if (messages.isEmpty()) {
            return null;
        } else {
            return String.join(";", messages);
        }
    }
}
