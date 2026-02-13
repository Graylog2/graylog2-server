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
package org.graylog.collectors;

import com.google.inject.assistedinject.FactoryModuleBuilder;
import org.graylog.collectors.input.CollectorIngestCodec;
import org.graylog.collectors.input.CollectorIngestGrpcInput;
import org.graylog.collectors.input.CollectorIngestHttpInput;
import org.graylog.collectors.input.transport.CollectorIngestGrpcTransport;
import org.graylog.collectors.input.transport.CollectorIngestHttpTransport;
import org.graylog.collectors.input.transport.CollectorIngestLogsService;
import org.graylog.collectors.rest.CollectorInstancesResource;
import org.graylog.collectors.rest.CollectorsConfigResource;
import com.google.inject.multibindings.Multibinder;
import org.graylog2.database.SequenceTopics;
import org.graylog2.plugin.PluginModule;

public class CollectorsModule extends PluginModule {
    @Override
    protected void configure() {
        Multibinder.newSetBinder(binder(), String.class, SequenceTopics.class)
                .addBinding().toInstance("fleet_txn_log");

        addMessageInput(CollectorIngestGrpcInput.class);
        addMessageInput(CollectorIngestHttpInput.class);
        addTransport(CollectorIngestGrpcTransport.NAME, CollectorIngestGrpcTransport.class);
        addTransport(CollectorIngestHttpTransport.NAME, CollectorIngestHttpTransport.class);
        addCodec(CollectorIngestCodec.NAME, CollectorIngestCodec.class);

        install(new FactoryModuleBuilder().build(CollectorIngestLogsService.Factory.class));

        addSystemRestResource(CollectorsConfigResource.class);
        addSystemRestResource(CollectorInstancesResource.class);
    }
}
