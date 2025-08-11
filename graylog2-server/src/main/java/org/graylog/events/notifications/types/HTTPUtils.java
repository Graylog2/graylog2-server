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
