/*
 * Copyright 2012-2014 TORCH GmbH
 *
 * This file is part of Graylog2.
 *
 * Graylog2 is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Graylog2 is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Graylog2.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.graylog2.bindings;

import com.google.inject.AbstractModule;
import com.google.inject.Scopes;
import com.google.inject.TypeLiteral;
import com.google.inject.assistedinject.FactoryModuleBuilder;
import com.google.inject.multibindings.Multibinder;
import com.ning.http.client.AsyncHttpClient;
import org.apache.shiro.mgt.DefaultSecurityManager;
import org.graylog2.Configuration;
import org.graylog2.alerts.AlertSender;
import org.graylog2.alerts.FormattedEmailAlertSender;
import org.graylog2.bindings.providers.*;
import org.graylog2.buffers.OutputBuffer;
import org.graylog2.buffers.OutputBufferWatermark;
import org.graylog2.buffers.processors.OutputBufferProcessor;
import org.graylog2.buffers.processors.ServerProcessBufferProcessor;
import org.graylog2.dashboards.DashboardRegistry;
import org.graylog2.database.MongoConnection;
import org.graylog2.indexer.Indexer;
import org.graylog2.indexer.MessageGatewayImpl;
import org.graylog2.indexer.cluster.Cluster;
import org.graylog2.indexer.counts.Counts;
import org.graylog2.indexer.healing.FixDeflectorByDeleteJob;
import org.graylog2.indexer.healing.FixDeflectorByMoveJob;
import org.graylog2.indexer.indices.Indices;
import org.graylog2.indexer.indices.jobs.OptimizeIndexJob;
import org.graylog2.indexer.ranges.RebuildIndexRangesJob;
import org.graylog2.indexer.searches.Searches;
import org.graylog2.inputs.InputCache;
import org.graylog2.inputs.OutputCache;
import org.graylog2.jersey.container.netty.SecurityContextFactory;
import org.graylog2.outputs.OutputRegistry;
import org.graylog2.plugin.PluginMetaData;
import org.graylog2.plugin.RulesEngine;
import org.graylog2.plugin.indexer.MessageGateway;
import org.graylog2.rest.RestAccessLogFilter;
import org.graylog2.security.ShiroSecurityBinding;
import org.graylog2.security.ShiroSecurityContextFactory;
import org.graylog2.security.ldap.LdapConnector;
import org.graylog2.security.realm.LdapUserAuthenticator;
import org.graylog2.shared.BaseConfiguration;
import org.graylog2.shared.ServerStatus;
import org.graylog2.shared.bindings.providers.AsyncHttpClientProvider;
import org.graylog2.shared.inputs.InputRegistry;
import org.graylog2.shared.metrics.jersey2.MetricsDynamicBinding;
import org.graylog2.streams.StreamRouter;
import org.graylog2.system.jobs.SystemJobFactory;
import org.graylog2.system.jobs.SystemJobManager;
import org.graylog2.system.shutdown.GracefulShutdown;

import javax.ws.rs.container.ContainerResponseFilter;
import javax.ws.rs.container.DynamicFeature;

/**
 * @author Dennis Oelkers <dennis@torch.sh>
 */
public class ServerBindings extends AbstractModule {
    private final Configuration configuration;
    private final MongoConnection mongoConnection;

    public ServerBindings(Configuration configuration) {
        this.configuration = configuration;

        mongoConnection = new MongoConnection();
        mongoConnection.setUser(configuration.getMongoUser());
        mongoConnection.setPassword(configuration.getMongoPassword());
        mongoConnection.setHost(configuration.getMongoHost());
        mongoConnection.setPort(configuration.getMongoPort());
        mongoConnection.setDatabase(configuration.getMongoDatabase());
        mongoConnection.setUseAuth(configuration.isMongoUseAuth());
        mongoConnection.setMaxConnections(configuration.getMongoMaxConnections());
        mongoConnection.setThreadsAllowedToBlockMultiplier(configuration.getMongoThreadsAllowedToBlockMultiplier());
        mongoConnection.setReplicaSet(configuration.getMongoReplicaSet());
        mongoConnection.connect();
    }

    @Override
    protected void configure() {
        bindInterfaces();
        bindSingletons();
        bindFactoryModules();
        bindDynamicFeatures();
        bindContainerResponseFilters();
        bindPluginMetaData();
    }

    private void bindFactoryModules() {
        install(new FactoryModuleBuilder().build(OutputBuffer.Factory.class));
        install(new FactoryModuleBuilder().build(OutputBufferProcessor.Factory.class));
        install(new FactoryModuleBuilder().build(ServerProcessBufferProcessor.Factory.class));
        install(new FactoryModuleBuilder().build(RebuildIndexRangesJob.Factory.class));
        install(new FactoryModuleBuilder().build(OptimizeIndexJob.Factory.class));
        install(new FactoryModuleBuilder().build(Searches.Factory.class));
        install(new FactoryModuleBuilder().build(Counts.Factory.class));
        install(new FactoryModuleBuilder().build(Cluster.Factory.class));
        install(new FactoryModuleBuilder().build(Indices.Factory.class));
        install(new FactoryModuleBuilder().build(FixDeflectorByDeleteJob.Factory.class));
        install(new FactoryModuleBuilder().build(FixDeflectorByMoveJob.Factory.class));
        install(new FactoryModuleBuilder().build(StreamRouter.Factory.class));
    }

    private void bindSingletons() {
        bind(Configuration.class).toInstance(configuration);
        bind(BaseConfiguration.class).toInstance(configuration);

        bind(MongoConnection.class).toInstance(mongoConnection);
        bind(OutputRegistry.class).toInstance(new OutputRegistry());

        ServerStatus serverStatus = new ServerStatus(configuration);
        serverStatus.addCapability(ServerStatus.Capability.SERVER);
        if (configuration.isMaster())
            serverStatus.addCapability(ServerStatus.Capability.MASTER);
        bind(ServerStatus.class).toInstance(serverStatus);

        bind(OutputBufferWatermark.class).toInstance(new OutputBufferWatermark());
        bind(OutputBuffer.class).toProvider(OutputBufferProvider.class);
        bind(Indexer.class).toProvider(IndexerProvider.class);
        bind(SystemJobManager.class).toProvider(SystemJobManagerProvider.class);
        bind(InputCache.class).toProvider(InputCacheProvider.class);
        bind(OutputCache.class).toProvider(OutputCacheProvider.class);
        bind(InputRegistry.class).toProvider(ServerInputRegistryProvider.class);
        bind(RulesEngine.class).toProvider(RulesEngineProvider.class);
        bind(LdapConnector.class).toProvider(LdapConnectorProvider.class);
        bind(LdapUserAuthenticator.class).toProvider(LdapUserAuthenticatorProvider.class);
        bind(DefaultSecurityManager.class).toProvider(DefaultSecurityManagerProvider.class);
        bind(SystemJobFactory.class).toProvider(SystemJobFactoryProvider.class);
        bind(DashboardRegistry.class).toProvider(DashboardRegistryProvider.class);
        bind(AsyncHttpClient.class).toProvider(AsyncHttpClientProvider.class);
        bind(StreamRouter.class).toProvider(StreamRouterProvider.class);
        bind(GracefulShutdown.class).in(Scopes.SINGLETON);
    }

    private void bindInterfaces() {
        bind(MessageGateway.class).to(MessageGatewayImpl.class);
        bind(SecurityContextFactory.class).to(ShiroSecurityContextFactory.class);
        bind(AlertSender.class).to(FormattedEmailAlertSender.class);
    }

    private MongoConnection getMongoConnection() {
        return this.mongoConnection;
    }

    private void bindDynamicFeatures() {
        TypeLiteral<Class<? extends DynamicFeature>> type = new TypeLiteral<Class<? extends DynamicFeature>>(){};
        Multibinder<Class<? extends DynamicFeature>> setBinder = Multibinder.newSetBinder(binder(), type);
        setBinder.addBinding().toInstance(ShiroSecurityBinding.class);
        setBinder.addBinding().toInstance(MetricsDynamicBinding.class);
    }

    private void bindContainerResponseFilters() {
        TypeLiteral<Class<? extends ContainerResponseFilter>> type = new TypeLiteral<Class<? extends ContainerResponseFilter>>(){};
        Multibinder<Class<? extends ContainerResponseFilter>> setBinder = Multibinder.newSetBinder(binder(), type);
        setBinder.addBinding().toInstance(RestAccessLogFilter.class);
    }

    private void bindPluginMetaData() {
        Multibinder<PluginMetaData> setBinder = Multibinder.newSetBinder(binder(), PluginMetaData.class);
    }
}
