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
import com.google.inject.multibindings.MapBinder;
import com.google.inject.multibindings.Multibinder;
import org.graylog.collectors.config.FilelogReceiverConfig;
import org.graylog.collectors.config.JournaldReceiverConfig;
import org.graylog.collectors.config.MacOSUnifiedLoggingReceiverConfig;
import org.graylog.collectors.config.WindowsEventLogReceiverConfig;
import org.graylog.collectors.db.FileSourceConfig;
import org.graylog.collectors.db.JournaldSourceConfig;
import org.graylog.collectors.db.MacOSUnifiedLoggingSourceConfig;
import org.graylog.collectors.db.WindowsEventLogSourceConfig;
import org.graylog.collectors.input.CollectorIngestCodec;
import org.graylog.collectors.input.CollectorIngestGrpcInput;
import org.graylog.collectors.input.CollectorIngestHttpInput;
import org.graylog.collectors.input.debug.NoOpOtlpTrafficDump;
import org.graylog.collectors.input.debug.OtlpTrafficDump;
import org.graylog.collectors.input.debug.OtlpTrafficDumpService;
import org.graylog.collectors.input.processor.FilelogRecordProcessor;
import org.graylog.collectors.input.processor.JournaldRecordProcessor;
import org.graylog.collectors.input.processor.LogRecordProcessor;
import org.graylog.collectors.input.processor.MacOSUnifiedLoggingRecordProcessor;
import org.graylog.collectors.input.processor.WindowsEventLogRecordProcessor;
import org.graylog.collectors.input.transport.CollectorIngestGrpcTransport;
import org.graylog.collectors.input.transport.CollectorIngestHttpTransport;
import org.graylog.collectors.input.transport.CollectorIngestLogsService;
import org.graylog.collectors.rest.CollectorInstancesResource;
import org.graylog.collectors.rest.CollectorsConfigResource;
import org.graylog.collectors.rest.FleetPermissions;
import org.graylog.collectors.rest.FleetResource;
import org.graylog.collectors.rest.SourceResource;
import org.graylog2.database.SequenceTopics;
import org.graylog2.featureflag.FeatureFlags;
import org.graylog2.plugin.PluginModule;

public class CollectorsModule extends PluginModule {
    private static final String OTLP_DUMP_FLAG = "collector_otlp_traffic_dump";

    private final boolean otlpDumpEnabled;

    public CollectorsModule(FeatureFlags featureFlags) {
        this.otlpDumpEnabled = featureFlags.isOn(OTLP_DUMP_FLAG);
    }

    @Override
    protected void configure() {
        // Fleet transaction log
        Multibinder.newSetBinder(binder(), String.class, SequenceTopics.class)
                .addBinding().toInstance("fleet_txn_log");
        bind(FleetTransactionLogService.class);

        addMessageInput(CollectorIngestGrpcInput.class);
        addMessageInput(CollectorIngestHttpInput.class);
        addTransport(CollectorIngestGrpcTransport.NAME, CollectorIngestGrpcTransport.class);
        addTransport(CollectorIngestHttpTransport.NAME, CollectorIngestHttpTransport.class);
        addCodec(CollectorIngestCodec.NAME, CollectorIngestCodec.class);

        final var logRecordProcessorBinder = MapBinder.newMapBinder(binder(), String.class, LogRecordProcessor.class);

        logRecordProcessorBinder.addBinding(FilelogReceiverConfig.RECEIVER_TYPE).to(FilelogRecordProcessor.class);
        logRecordProcessorBinder.addBinding(JournaldReceiverConfig.RECEIVER_TYPE).to(JournaldRecordProcessor.class);
        logRecordProcessorBinder.addBinding(MacOSUnifiedLoggingReceiverConfig.RECEIVER_TYPE).to(MacOSUnifiedLoggingRecordProcessor.class);
        logRecordProcessorBinder.addBinding(WindowsEventLogReceiverConfig.RECEIVER_TYPE).to(WindowsEventLogRecordProcessor.class);

        if (otlpDumpEnabled) {
            bind(OtlpTrafficDump.class).to(OtlpTrafficDumpService.class).asEagerSingleton();
            addInitializer(OtlpTrafficDumpService.class);
        } else {
            bind(OtlpTrafficDump.class).to(NoOpOtlpTrafficDump.class);
        }

        install(new FactoryModuleBuilder().build(CollectorIngestLogsService.Factory.class));

        addSystemRestResource(CollectorsConfigResource.class);
        addSystemRestResource(CollectorInstancesResource.class);

        // Fleet management services
        bind(FleetService.class);
        bind(SourceService.class);

        // Fleet management REST resources
        addSystemRestResource(FleetResource.class);
        addSystemRestResource(SourceResource.class);

        // Fleet permissions
        addPermissions(FleetPermissions.class);

        // SourceConfig Jackson subtypes
        registerJacksonSubtype(FileSourceConfig.class);
        registerJacksonSubtype(JournaldSourceConfig.class);
        registerJacksonSubtype(MacOSUnifiedLoggingSourceConfig.class);
        registerJacksonSubtype(WindowsEventLogSourceConfig.class);
    }
}
