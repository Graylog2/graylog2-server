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

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

public class CollectingVersionProbeListener implements VersionProbeListener {

    private final List<String> retries = new ArrayList<>();
    private final List<String> errors = new ArrayList<>();

    @Override
    public void onRetry(long attemptNumber, long connectionAttempts, @Nullable
    Throwable cause) {
        retries.add(String.format(Locale.ROOT, "Attempt %d/%d: %s", attemptNumber, connectionAttempts, getMessage(cause)));
    }

    @Override
    public void onError(@Nonnull String message, @Nullable Throwable cause) {
        errors.add(String.format(Locale.ROOT, "%s%s", message, getMessage(cause)));
    }

    @Nonnull
    private static String getMessage(@Nullable Throwable cause) {
        return Optional.ofNullable(cause).map(Throwable::getMessage).orElse("");
    }

    public List<String> getRetries() {
        return retries;
    }

    public List<String> getErrors() {
        return errors;
    }
}
