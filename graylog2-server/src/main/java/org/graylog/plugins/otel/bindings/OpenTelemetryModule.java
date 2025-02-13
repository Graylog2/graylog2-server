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
package org.graylog.plugins.otel.bindings;

import com.google.inject.assistedinject.FactoryModuleBuilder;
import org.graylog.plugins.otel.input.OpenTelemetryGrpcInput;
import org.graylog.plugins.otel.input.codec.LogsCodec;
import org.graylog.plugins.otel.input.codec.OpenTelemetryCodec;
import org.graylog.plugins.otel.input.grpc.LogsService;
import org.graylog.plugins.otel.input.grpc.OpenTelemetryGrpcTransport;
import org.graylog2.plugin.PluginModule;

public class OpenTelemetryModule extends PluginModule {
    @Override
    protected void configure() {
        addMessageInput(OpenTelemetryGrpcInput.class);
        addTransport(OpenTelemetryGrpcTransport.NAME, OpenTelemetryGrpcTransport.class);
        addCodec(OpenTelemetryCodec.NAME, OpenTelemetryCodec.class);

        install(new FactoryModuleBuilder().build(LogsCodec.Factory.class));
        install(new FactoryModuleBuilder().build(LogsService.Factory.class));
    }
}
