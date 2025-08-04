package org.graylog.events.notifications.types;

import java.util.Set;

public class HTTPUtils {
    private HTTPUtils() {
    }

    private static Set<Integer> RETRYABLE_STATUS_CODES = Set.of(
            408,// Request Timeout
            429 // Too Many Requests
    );

    public static boolean isRetryableStatus(int statusCode) {
        if (RETRYABLE_STATUS_CODES.contains(statusCode)) {
            return true;
            // Any 5xx status code is considered retryable
        } else return statusCode >= 500 && statusCode <= 599;
    }
}
