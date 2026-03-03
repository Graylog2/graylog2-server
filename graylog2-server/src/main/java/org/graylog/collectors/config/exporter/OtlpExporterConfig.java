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
package org.graylog.collectors.config.exporter;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import org.graylog.collectors.config.GoDurationSerializer;
import org.graylog.collectors.config.TLSConfigurationSettings;

import java.time.Duration;

public interface OtlpExporterConfig {

    @JsonIgnore
    String getName();

    @JsonProperty("endpoint")
    String endpoint();

    @JsonProperty("tls")
    TLSConfigurationSettings tls();

    @JsonProperty("timeout")
    @JsonSerialize(using = GoDurationSerializer.class)
    Duration timeout();

    @JsonProperty("retry_on_failure")
    ExporterRetryOnFailure retryOnFailure();

    @JsonProperty("sending_queue")
    ExporterSendingQueue sendingQueue();

    interface Builder<T, B> {
        B endpoint(String endpoint);

        B tls(TLSConfigurationSettings tls);

        B timeout(Duration timeout);

        B retryOnFailure(ExporterRetryOnFailure retryOnFailure);

        B sendingQueue(ExporterSendingQueue sendingQueue);

        T build();
    }
}
