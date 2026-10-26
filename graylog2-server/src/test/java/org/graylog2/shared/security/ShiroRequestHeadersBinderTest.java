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
package org.graylog2.shared.security;

import jakarta.ws.rs.core.MultivaluedHashMap;
import jakarta.ws.rs.core.MultivaluedMap;
import org.apache.shiro.util.ThreadContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.graylog2.shared.rest.RequestIdFilter.X_REQUEST_ID;
import static org.graylog2.shared.security.ShiroRequestHeadersBinder.REQUEST_HEADERS;
import static org.graylog2.shared.security.ShiroRequestHeadersBinder.getHeaderFromThreadContext;
import static org.graylog2.shared.security.ShiroRequestHeadersBinder.withRequestId;

class ShiroRequestHeadersBinderTest {
    @AfterEach
    void tearDown() {
        ThreadContext.remove(REQUEST_HEADERS);
    }

    @Test
    void bindsRequestIdAndRemovesItAfterwards() {
        final Optional<String> requestId = withRequestId("request-id", () -> getHeaderFromThreadContext(X_REQUEST_ID));

        assertThat(requestId).hasValue("request-id");
        assertThat(ThreadContext.get(REQUEST_HEADERS)).isNull();
    }

    @Test
    void restoresPreviousHeaders() {
        final MultivaluedMap<String, String> headers = new MultivaluedHashMap<>();
        headers.putSingle(X_REQUEST_ID, "previous-request-id");
        ThreadContext.put(REQUEST_HEADERS, headers);

        final Optional<String> requestId = withRequestId("request-id", () -> getHeaderFromThreadContext(X_REQUEST_ID));

        assertThat(requestId).hasValue("request-id");
        assertThat(ThreadContext.get(REQUEST_HEADERS)).isSameAs(headers);
    }

    @Test
    void keepsOtherHeadersWhileRunning() {
        final MultivaluedMap<String, String> headers = new MultivaluedHashMap<>();
        headers.putSingle("X-Other", "value");
        ThreadContext.put(REQUEST_HEADERS, headers);

        final Optional<String> otherHeader = withRequestId("request-id", () -> getHeaderFromThreadContext("X-Other"));

        assertThat(otherHeader).hasValue("value");
        assertThat(headers).doesNotContainKey(X_REQUEST_ID);
        assertThat(ThreadContext.get(REQUEST_HEADERS)).isSameAs(headers);
    }

    @Test
    void restoresPreviousHeadersOnException() {
        assertThatThrownBy(() -> withRequestId("request-id", () -> {
            throw new IllegalStateException("boom");
        })).isInstanceOf(IllegalStateException.class);

        assertThat(ThreadContext.get(REQUEST_HEADERS)).isNull();
    }
}
