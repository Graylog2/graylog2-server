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
package org.graylog2.bindings;

import com.floreysoft.jmte.Engine;
import com.google.inject.Scopes;
import com.google.inject.assistedinject.FactoryModuleBuilder;
import com.google.inject.multibindings.Multibinder;
import org.apache.shiro.mgt.DefaultSecurityManager;
import org.glassfish.grizzly.http.server.ErrorPageGenerator;
import org.graylog2.Configuration;
import org.graylog2.alerts.AlertSender;
import org.graylog2.alerts.EmailRecipients;
import org.graylog2.alerts.FormattedEmailAlertSender;
import org.graylog2.bindings.providers.ClusterEventBusProvider;
import org.graylog2.bindings.providers.DefaultSecurityManagerProvider;
import org.graylog2.bindings.providers.DefaultStreamProvider;
import org.graylog2.bindings.providers.MongoConnectionProvider;
import org.graylog2.bindings.providers.SystemJobFactoryProvider;
import org.graylog2.bindings.providers.SystemJobManagerProvider;
import org.graylog2.cluster.ClusterConfigServiceImpl;
import org.graylog2.database.MongoConnection;
import org.graylog2.events.ClusterEventBus;
import org.graylog2.grok.GrokModule;
import org.graylog2.grok.GrokPatternRegistry;
import org.graylog2.indexer.SetIndexReadOnlyJob;
import org.graylog2.indexer.fieldtypes.FieldTypesModule;
import org.graylog2.indexer.healing.FixDeflectorByDeleteJob;
import org.graylog2.indexer.healing.FixDeflectorByMoveJob;
import org.graylog2.indexer.indices.jobs.IndexSetCleanupJob;
import org.graylog2.indexer.indices.jobs.OptimizeIndexJob;
import org.graylog2.indexer.indices.jobs.SetIndexReadOnlyAndCalculateRangeJob;
import org.graylog2.indexer.ranges.CreateNewSingleIndexRangeJob;
import org.graylog2.indexer.ranges.RebuildIndexRangesJob;
import org.graylog2.inputs.InputEventListener;
import org.graylog2.inputs.InputStateListener;
import org.graylog2.inputs.PersistedInputsImpl;
import org.graylog2.lookup.LookupModule;
import org.graylog2.plugin.cluster.ClusterConfigService;
import org.graylog2.plugin.inject.Graylog2Module;
import org.graylog2.plugin.rest.ValidationFailureExceptionMapper;
import org.graylog2.plugin.streams.DefaultStream;
import org.graylog2.plugin.streams.Stream;
import org.graylog2.rest.ElasticsearchExceptionMapper;
import org.graylog2.rest.GenericErrorCsvWriter;
import org.graylog2.rest.GraylogErrorPageGenerator;
import org.graylog2.rest.NotFoundExceptionMapper;
import org.graylog2.rest.QueryParsingExceptionMapper;
import org.graylog2.rest.ScrollChunkWriter;
import org.graylog2.rest.ValidationExceptionMapper;
import org.graylog2.security.realm.AuthenticatingRealmModule;
import org.graylog2.security.realm.AuthorizationOnlyRealmModule;
import org.graylog2.shared.buffers.processors.ProcessBufferProcessor;
import org.graylog2.shared.inputs.PersistedInputs;
import org.graylog2.shared.messageq.MessageQueueModule;
import org.graylog2.shared.metrics.jersey2.MetricsDynamicBinding;
import org.graylog2.shared.security.RestrictToMasterFeature;
import org.graylog2.shared.system.activities.ActivityWriter;
import org.graylog2.streams.DefaultStreamChangeHandler;
import org.graylog2.streams.StreamRouter;
import org.graylog2.streams.StreamRouterEngine;
import org.graylog2.system.activities.SystemMessageActivityWriter;
import org.graylog2.system.debug.ClusterDebugEventListener;
import org.graylog2.system.debug.LocalDebugEventListener;
import org.graylog2.system.jobs.SystemJobFactory;
import org.graylog2.system.jobs.SystemJobManager;
import org.graylog2.system.shutdown.GracefulShutdown;
import org.graylog2.system.stats.ClusterStatsModule;
import org.graylog2.users.RoleService;
import org.graylog2.users.RoleServiceImpl;
import org.graylog2.users.StartPageCleanupListener;
import org.graylog2.users.UserImpl;

import javax.ws.rs.container.DynamicFeature;
import javax.ws.rs.ext.ExceptionMapper;

public class ServerBindings extends Graylog2Module {
    private final Configuration configuration;

    public ServerBindings(Configuration configuration) {
        this.configuration = configuration;
    }

    @Override
    protected void configure() {
        bindInterfaces();
        bindSingletons();
        install(new MessageQueueModule(configuration));
        bindProviders();
        bindFactoryModules();
        bindDynamicFeatures();
        bindExceptionMappers();
        bindAdditionalJerseyComponents();
        bindEventBusListeners();
        install(new AuthenticatingRealmModule(configuration));
        install(new AuthorizationOnlyRealmModule());
        bindSearchResponseDecorators();
        install(new GrokModule());
        install(new LookupModule(configuration));
        install(new FieldTypesModule());

        // Just to create the binders so they are present in the injector. Prevents a server startup error when no
        // outputs are bound that implement MessageOutput.Factory2.
        outputsMapBinder2();
    }

    private void bindProviders() {
        bind(ClusterEventBus.class).toProvider(ClusterEventBusProvider.class).asEagerSingleton();
    }

    private void bindFactoryModules() {
        // System Jobs
        install(new FactoryModuleBuilder().build(RebuildIndexRangesJob.Factory.class));
        install(new FactoryModuleBuilder().build(OptimizeIndexJob.Factory.class));
        install(new FactoryModuleBuilder().build(SetIndexReadOnlyJob.Factory.class));
        install(new FactoryModuleBuilder().build(IndexSetCleanupJob.Factory.class));
        install(new FactoryModuleBuilder().build(CreateNewSingleIndexRangeJob.Factory.class));
        install(new FactoryModuleBuilder().build(FixDeflectorByDeleteJob.Factory.class));
        install(new FactoryModuleBuilder().build(FixDeflectorByMoveJob.Factory.class));
        install(new FactoryModuleBuilder().build(SetIndexReadOnlyAndCalculateRangeJob.Factory.class));

        install(new FactoryModuleBuilder().build(UserImpl.Factory.class));

        install(new FactoryModuleBuilder().build(EmailRecipients.Factory.class));

        install(new FactoryModuleBuilder().build(ProcessBufferProcessor.Factory.class));
        bind(Stream.class).annotatedWith(DefaultStream.class).toProvider(DefaultStreamProvider.class);
        bind(DefaultStreamChangeHandler.class).asEagerSingleton();
    }

    private void bindSingletons() {
        bind(MongoConnection.class).toProvider(MongoConnectionProvider.class);

        bind(SystemJobManager.class).toProvider(SystemJobManagerProvider.class);
        bind(DefaultSecurityManager.class).toProvider(DefaultSecurityManagerProvider.class).asEagerSingleton();
        bind(SystemJobFactory.class).toProvider(SystemJobFactoryProvider.class);
        bind(GracefulShutdown.class).in(Scopes.SINGLETON);
        bind(ClusterStatsModule.class).asEagerSingleton();
        bind(ClusterConfigService.class).to(ClusterConfigServiceImpl.class).asEagerSingleton();
        bind(GrokPatternRegistry.class).in(Scopes.SINGLETON);
        bind(Engine.class).toInstance(Engine.createEngine());
        bind(ErrorPageGenerator.class).to(GraylogErrorPageGenerator.class).asEagerSingleton();

        registerRestControllerPackage("org.graylog2.rest.resources");
        registerRestControllerPackage("org.graylog2.shared.rest.resources");
    }

    private void bindInterfaces() {
        bind(AlertSender.class).to(FormattedEmailAlertSender.class);
        bind(StreamRouter.class);
        install(new FactoryModuleBuilder().implement(StreamRouterEngine.class, StreamRouterEngine.class).build(
                StreamRouterEngine.Factory.class));
        bind(ActivityWriter.class).to(SystemMessageActivityWriter.class);
        bind(PersistedInputs.class).to(PersistedInputsImpl.class);

        bind(RoleService.class).to(RoleServiceImpl.class).in(Scopes.SINGLETON);
    }

    private void bindDynamicFeatures() {
        final Multibinder<Class<? extends DynamicFeature>> dynamicFeatures = jerseyDynamicFeatureBinder();
        dynamicFeatures.addBinding().toInstance(MetricsDynamicBinding.class);
        dynamicFeatures.addBinding().toInstance(RestrictToMasterFeature.class);
    }

    private void bindExceptionMappers() {
        final Multibinder<Class<? extends ExceptionMapper>> exceptionMappers = jerseyExceptionMapperBinder();
        exceptionMappers.addBinding().toInstance(NotFoundExceptionMapper.class);
        exceptionMappers.addBinding().toInstance(ValidationExceptionMapper.class);
        exceptionMappers.addBinding().toInstance(ValidationFailureExceptionMapper.class);
        exceptionMappers.addBinding().toInstance(ElasticsearchExceptionMapper.class);
        exceptionMappers.addBinding().toInstance(QueryParsingExceptionMapper.class);
    }

    private void bindAdditionalJerseyComponents() {
        jerseyAdditionalComponentsBinder().addBinding().toInstance(ScrollChunkWriter.class);
        jerseyAdditionalComponentsBinder().addBinding().toInstance(GenericErrorCsvWriter.class);
    }

    private void bindEventBusListeners() {
        bind(InputStateListener.class).asEagerSingleton();
        bind(InputEventListener.class).asEagerSingleton();
        bind(LocalDebugEventListener.class).asEagerSingleton();
        bind(ClusterDebugEventListener.class).asEagerSingleton();
        bind(StartPageCleanupListener.class).asEagerSingleton();
    }

    private void bindSearchResponseDecorators() {
        // only triggering an initialize to make sure that the binding exists
        searchResponseDecoratorBinder();
    }
}
