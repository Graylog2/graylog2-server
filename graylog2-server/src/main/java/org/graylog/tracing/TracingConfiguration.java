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
package org.graylog.tracing;

import com.github.joschi.jadconfig.Parameter;
import com.github.joschi.jadconfig.util.Duration;
import com.github.joschi.jadconfig.validators.StringNotBlankValidator;
import com.github.joschi.jadconfig.validators.URIAbsoluteValidator;

import java.net.URI;

@SuppressWarnings({"FieldCanBeLocal", "FieldMayBeFinal", "unused"})
public class TracingConfiguration {
    private static final String PREFIX = "tracing_";

    @Parameter(value = PREFIX + "service_name", validators = StringNotBlankValidator.class)
    private String serviceName = "Graylog";

    @Parameter(value = PREFIX + "instrumentation_scope_name", validators = StringNotBlankValidator.class)
    private String instrumentationScopeName = "org.graylog";

    @Parameter(value = PREFIX + "exporter_enabled")
    private boolean exporterEnabled = false;

    @Parameter(value = PREFIX + "exporter_http_endpoint", validators = URIAbsoluteValidator.class)
    private URI exporterHttpEndpoint = URI.create("http://127.0.0.1:14250");

    @Parameter(value = PREFIX + "exporter_publish_timeout")
    private Duration exporterPublishTimeout = Duration.seconds(1);

    public boolean isExporterEnabled() {
        return exporterEnabled;
    }

    public String getServiceName() {
        return serviceName;
    }

    public String getInstrumentationScopeName() {
        return instrumentationScopeName;
    }

    public URI getExporterHttpEndpoint() {
        return exporterHttpEndpoint;
    }

    public Duration getExporterPublishTimeout() {
        return exporterPublishTimeout;
    }
}
