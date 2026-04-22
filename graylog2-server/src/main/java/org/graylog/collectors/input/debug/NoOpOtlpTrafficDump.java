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
package org.graylog.collectors.input.debug;

import jakarta.inject.Singleton;
import org.graylog.collectors.CollectorJournal;

/**
 * No-op implementation of {@link OtlpTrafficDump} used when the
 * {@code collector_otlp_traffic_dump} feature flag is disabled.
 */
@Singleton
public class NoOpOtlpTrafficDump implements OtlpTrafficDump {
    @Override
    public void write(CollectorJournal.Record record) {
        // no-op
    }
}
