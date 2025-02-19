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
import org.graylog.plugins.otel.input.OTelGrpcInput;
import org.graylog.plugins.otel.input.codec.OTelCodec;
import org.graylog.plugins.otel.input.codec.OTelLogsCodec;
import org.graylog.plugins.otel.input.grpc.OTelGrpcTransport;
import org.graylog.plugins.otel.input.grpc.OTelLogsService;
import org.graylog2.plugin.PluginModule;

public class OTelModule extends PluginModule {
    @Override
    protected void configure() {
        addMessageInput(OTelGrpcInput.class);
        addTransport(OTelGrpcTransport.NAME, OTelGrpcTransport.class);
        addCodec(OTelCodec.NAME, OTelCodec.class);

        install(new FactoryModuleBuilder().build(OTelLogsCodec.Factory.class));
        install(new FactoryModuleBuilder().build(OTelLogsService.Factory.class));
    }
}
