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
package org.graylog2.configuration;

import com.github.joschi.jadconfig.Parameter;
import com.github.joschi.jadconfig.util.Duration;
import com.github.joschi.jadconfig.validators.PositiveDurationValidator;
import com.github.joschi.jadconfig.validators.PositiveIntegerValidator;

public class ClusterProcessingConfiguration {
    public static final String INSTALL_HTTP_CONNECTION_TIMEOUT = "install_http_connection_timeout";
    public static final String INSTALL_OUTPUT_BUFFER_DRAINING_INTERVAL = "install_output_buffer_drain_interval";
    public static final String INSTALL_OUTPUT_BUFFER_DRAINING_MAX_RETRIES = "install_output_buffer_max_retries";

    private static final int DEFAULT_INSTALL_RETRIES = 150;
    private static final Duration DEFAULT_INSTALL_SECONDS = Duration.seconds(2);

    @Parameter(value = INSTALL_HTTP_CONNECTION_TIMEOUT, validators = PositiveDurationValidator.class)
    private Duration installHttpConnectionTimeout = Duration.seconds(10L);

    @Parameter(value = INSTALL_OUTPUT_BUFFER_DRAINING_INTERVAL, validators = PositiveDurationValidator.class)
    private Duration installOutputBufferDrainingInterval = DEFAULT_INSTALL_SECONDS;

    // The maximum number of times to check if buffers have drained during Illuminate restarts on all
    // nodes before giving up
    @Parameter(value = INSTALL_OUTPUT_BUFFER_DRAINING_MAX_RETRIES, validators = PositiveIntegerValidator.class)
    private int installOutputBufferDrainingMaxRetries = DEFAULT_INSTALL_RETRIES;

}
