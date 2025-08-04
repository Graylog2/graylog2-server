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
