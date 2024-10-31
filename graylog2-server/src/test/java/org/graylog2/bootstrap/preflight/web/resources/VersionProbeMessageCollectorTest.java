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
import org.assertj.core.api.Assertions;
import org.graylog2.storage.versionprobe.VersionProbeListener;
import org.junit.jupiter.api.Test;

import java.net.ConnectException;

class VersionProbeMessageCollectorTest {

    @Test
    void testMessageCollectionError() {
        final VersionProbeMessageCollector messageCollector = new VersionProbeMessageCollector(dummyDelegate());
        messageCollector.onError("Failed to connect", new IllegalArgumentException("Invalid URL"));
        Assertions.assertThat(messageCollector.joinedMessages()).isEqualTo("Failed to connect: Invalid URL");
    }


    @Test
    void testMessageCollectionAttempt() {
        final VersionProbeMessageCollector messageCollector = new VersionProbeMessageCollector(dummyDelegate());
        messageCollector.onRetry(1, 5, new ConnectException("Host not reachable"));
        Assertions.assertThat(messageCollector.joinedMessages()).isEqualTo("Host not reachable");
    }

    @Nonnull
    private static VersionProbeListener dummyDelegate() {
        return new VersionProbeListener() {
            @Override
            public void onRetry(long attemptNumber, long connectionAttempts, @Nullable Throwable cause) {
            }

            @Override
            public void onError(@Nonnull String message, @Nullable Throwable cause) {
            }
        };
    }
}
