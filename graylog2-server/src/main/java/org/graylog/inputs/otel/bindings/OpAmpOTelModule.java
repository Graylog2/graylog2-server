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
package org.graylog.inputs.otel.bindings;

import com.google.inject.assistedinject.FactoryModuleBuilder;
import org.graylog.inputs.otel.OpAmpOTelGrpcInput;
import org.graylog.inputs.otel.OpAmpOTelHttpInput;
import org.graylog.inputs.otel.codec.OpAmpOTelCodec;
import org.graylog.inputs.otel.transport.OpAmpOTelGrpcTransport;
import org.graylog.inputs.otel.transport.OpAmpOTelHttpTransport;
import org.graylog.inputs.otel.transport.OpAmpOTelLogsService;
import org.graylog2.plugin.PluginModule;

public class OpAmpOTelModule extends PluginModule {
    @Override
    protected void configure() {
        addMessageInput(OpAmpOTelGrpcInput.class);
        addMessageInput(OpAmpOTelHttpInput.class);
        addTransport(OpAmpOTelGrpcTransport.NAME, OpAmpOTelGrpcTransport.class);
        addTransport(OpAmpOTelHttpTransport.NAME, OpAmpOTelHttpTransport.class);
        addCodec(OpAmpOTelCodec.NAME, OpAmpOTelCodec.class);

        install(new FactoryModuleBuilder().build(OpAmpOTelLogsService.Factory.class));
    }
}
