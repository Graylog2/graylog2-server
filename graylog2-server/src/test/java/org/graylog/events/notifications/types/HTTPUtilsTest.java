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
package org.graylog.events.notifications.types;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class HTTPUtilsTest {
    @Test
    void testIsRetryableStatus_withRetryableCodes() {
        assertTrue(HTTPUtils.isRetryableStatus(408));
        assertTrue(HTTPUtils.isRetryableStatus(429));
        assertTrue(HTTPUtils.isRetryableStatus(500));
        assertTrue(HTTPUtils.isRetryableStatus(599));
        assertTrue(HTTPUtils.isRetryableStatus(503));
    }

    @Test
    void testIsRetryableStatus_withNonRetryableCodes() {
        assertFalse(HTTPUtils.isRetryableStatus(200));
        assertFalse(HTTPUtils.isRetryableStatus(404));
        assertFalse(HTTPUtils.isRetryableStatus(400));
        assertFalse(HTTPUtils.isRetryableStatus(301));
    }
}
