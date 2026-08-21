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
package org.graylog.collectors.opamp;

public final class OpAmpConstants {
    public static final String PATH = "/v1/opamp";

    /**
     * Identifies the enrollment auth-check message exchange. Collectors announce this custom
     * capability and send a matching custom message as a pre-flight check to detect invalid
     * tokens before starting the OpAMP client. Must match the collector-side constant.
     */
    public static final String AUTH_CHECK_CUSTOM_CAPABILITY = "org.graylog.collector.enrollment.auth-check";

    /**
     * The custom message type marking an auth-check request. Must match the collector-side
     * constant.
     */
    public static final String AUTH_CHECK_MESSAGE_TYPE = "request";

    private OpAmpConstants() {
    }
}
