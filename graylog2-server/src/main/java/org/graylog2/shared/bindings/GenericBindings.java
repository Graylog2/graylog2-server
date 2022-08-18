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
package org.graylog2.shared.bindings;

import com.google.common.eventbus.EventBus;
import com.google.common.util.concurrent.ServiceManager;
import com.google.inject.Scopes;
import com.google.inject.TypeLiteral;
import com.google.inject.assistedinject.FactoryModuleBuilder;
import com.google.inject.multibindings.MapBinder;
import com.google.inject.multibindings.Multibinder;
import com.google.inject.multibindings.OptionalBinder;
import com.google.inject.name.Names;
import okhttp3.OkHttpClient;
import org.graylog.failure.DefaultFailureHandler;
import org.graylog.failure.DefaultFailureHandlingConfiguration;
import org.graylog.failure.FailureHandler;
import org.graylog.failure.FailureHandlingConfiguration;
import org.graylog.failure.FailureHandlingService;
import org.graylog2.indexer.EventIndexTemplateProvider;
import org.graylog2.indexer.IndexTemplateProvider;
import org.graylog2.indexer.MessageIndexTemplateProvider;
import org.graylog2.plugin.IOState;
import org.graylog2.plugin.LocalMetricRegistry;
import org.graylog2.plugin.buffers.InputBuffer;
import org.graylog2.plugin.inject.Graylog2Module;
import org.graylog2.plugin.inputs.MessageInput;
import org.graylog2.plugin.inputs.util.ThroughputCounter;
import org.graylog2.plugin.system.NodeId;
import org.graylog2.shared.bindings.providers.EventBusProvider;
import org.graylog2.shared.bindings.providers.NodeIdProvider;
import org.graylog2.shared.bindings.providers.OkHttpClientProvider;
import org.graylog2.shared.bindings.providers.ProxiedRequestsExecutorService;
import org.graylog2.shared.bindings.providers.ServiceManagerProvider;
import org.graylog2.shared.buffers.InputBufferImpl;
import org.graylog2.shared.buffers.NoopInputBuffer;
import org.graylog2.shared.buffers.ProcessBuffer;
import org.graylog2.shared.buffers.processors.DecodingProcessor;
import org.graylog2.shared.inputs.InputRegistry;

import javax.activation.MimetypesFileTypeMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Semaphore;

public class GenericBindings extends Graylog2Module {
    private final boolean isMigrationCommand;

    public GenericBindings(boolean isMigrationCommand) {
        this.isMigrationCommand = isMigrationCommand;
    }

    @Override
    protected void configure() {
        bind(LocalMetricRegistry.class).in(Scopes.NO_SCOPE); // must not be a singleton!

        install(new FactoryModuleBuilder().build(DecodingProcessor.Factory.class));

        bind(ProcessBuffer.class).asEagerSingleton();
        if (isMigrationCommand) {
            bind(InputBuffer.class).to(NoopInputBuffer.class);
        } else {
            bind(InputBuffer.class).to(InputBufferImpl.class);
        }
        bind(NodeId.class).toProvider(NodeIdProvider.class);

        if (!isMigrationCommand) {
            bind(ServiceManager.class).toProvider(ServiceManagerProvider.class).asEagerSingleton();
        }

        bind(ThroughputCounter.class);

        bind(EventBus.class).toProvider(EventBusProvider.class).in(Scopes.SINGLETON);

        bind(Semaphore.class).annotatedWith(Names.named("JournalSignal")).toInstance(new Semaphore(0));

        install(new FactoryModuleBuilder().build(new TypeLiteral<IOState.Factory<MessageInput>>() {}));

        bind(InputRegistry.class).asEagerSingleton();

        bind(OkHttpClient.class).toProvider(OkHttpClientProvider.class).asEagerSingleton();

        bind(MimetypesFileTypeMap.class).toInstance(new MimetypesFileTypeMap());

        bind(ExecutorService.class).annotatedWith(Names.named("proxiedRequestsExecutorService")).toProvider(ProxiedRequestsExecutorService.class).asEagerSingleton();

        bind(FailureHandler.class).annotatedWith(Names.named("fallbackFailureHandler"))
                .to(DefaultFailureHandler.class).asEagerSingleton();

        Multibinder.newSetBinder(binder(), FailureHandler.class);

        OptionalBinder.newOptionalBinder(binder(), FailureHandlingConfiguration.class)
                .setDefault()
                .to(DefaultFailureHandlingConfiguration.class);

        final MapBinder<String, IndexTemplateProvider> indexTemplateProviderBinder
                = MapBinder.newMapBinder(binder(), String.class, IndexTemplateProvider.class);

        indexTemplateProviderBinder.addBinding(MessageIndexTemplateProvider.MESSAGE_TEMPLATE_TYPE)
                .to(MessageIndexTemplateProvider.class);
        indexTemplateProviderBinder.addBinding(EventIndexTemplateProvider.EVENT_TEMPLATE_TYPE)
                .to(EventIndexTemplateProvider.class);

        serviceBinder().addBinding().to(FailureHandlingService.class).in(Scopes.SINGLETON);
    }

}
