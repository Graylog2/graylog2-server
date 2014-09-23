/**
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

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.jaxrs.base.JsonMappingExceptionMapper;
import com.fasterxml.jackson.jaxrs.base.JsonParseExceptionMapper;
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
import org.graylog2.buffers.OutputBufferWatermark;
import org.graylog2.buffers.processors.OutputBufferProcessor;
import org.graylog2.buffers.processors.ServerProcessBufferProcessor;
import org.graylog2.database.MongoConnection;
import org.graylog2.filters.FilterService;
import org.graylog2.filters.FilterServiceImpl;
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
import org.graylog2.inputs.BasicCache;
import org.graylog2.inputs.InputCache;
import org.graylog2.inputs.OutputCache;
import org.graylog2.inputs.ServerInputRegistry;
import org.graylog2.jersey.container.netty.SecurityContextFactory;
import org.graylog2.plugin.BaseConfiguration;
import org.graylog2.plugin.PluginMetaData;
import org.graylog2.plugin.RulesEngine;
import org.graylog2.plugin.ServerStatus;
import org.graylog2.plugin.indexer.MessageGateway;
import org.graylog2.rest.NotFoundExceptionMapper;
import org.graylog2.rest.RestAccessLogFilter;
import org.graylog2.rest.ValidationExceptionMapper;
import org.graylog2.security.ShiroSecurityBinding;
import org.graylog2.security.ShiroSecurityContextFactory;
import org.graylog2.security.ldap.LdapConnector;
import org.graylog2.security.ldap.LdapSettingsImpl;
import org.graylog2.security.realm.LdapUserAuthenticator;
import org.graylog2.shared.inputs.InputRegistry;
import org.graylog2.shared.metrics.jersey2.MetricsDynamicBinding;
import org.graylog2.streams.StreamRouter;
import org.graylog2.system.jobs.SystemJobFactory;
import org.graylog2.system.jobs.SystemJobManager;
import org.graylog2.system.shutdown.GracefulShutdown;

import javax.ws.rs.container.ContainerResponseFilter;
import javax.ws.rs.container.DynamicFeature;
import javax.ws.rs.ext.ExceptionMapper;

/**
 * @author Dennis Oelkers <dennis@torch.sh>
 */
public class ServerBindings extends AbstractModule {
    private final Configuration configuration;

    public ServerBindings(Configuration configuration) {
        this.configuration = configuration;
    }

    @Override
    protected void configure() {
        bindInterfaces();
        bindSingletons();
        bindProviders();
        bindFactoryModules();
        bindDynamicFeatures();
        bindContainerResponseFilters();
        bindExceptionMappers();
        bindPluginMetaData();
    }

    private void bindProviders() {
        bind(ObjectMapper.class).toProvider(ServerObjectMapperProvider.class);
    }

    private void bindFactoryModules() {
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
        install(new FactoryModuleBuilder().build(LdapSettingsImpl.Factory.class));
    }

    private void bindSingletons() {
        bind(Configuration.class).toInstance(configuration);
        bind(BaseConfiguration.class).toInstance(configuration);

        bind(MongoConnection.class).toProvider(MongoConnectionProvider.class).asEagerSingleton();

        Multibinder<ServerStatus.Capability> capabilityBinder =
                Multibinder.newSetBinder(binder(), ServerStatus.Capability.class);
        capabilityBinder.addBinding().toInstance(ServerStatus.Capability.SERVER);
        if (configuration.isMaster())
            capabilityBinder.addBinding().toInstance(ServerStatus.Capability.MASTER);
        bind(ServerStatus.class).in(Scopes.SINGLETON);

        bind(OutputBufferWatermark.class).asEagerSingleton();
        bind(Indexer.class).asEagerSingleton();
        bind(SystemJobManager.class).asEagerSingleton();
        bind(InputRegistry.class).to(ServerInputRegistry.class).asEagerSingleton();
        bind(RulesEngine.class).toProvider(RulesEngineProvider.class);
        bind(LdapConnector.class).toProvider(LdapConnectorProvider.class).asEagerSingleton();
        bind(LdapUserAuthenticator.class).toProvider(LdapUserAuthenticatorProvider.class);
        bind(DefaultSecurityManager.class).toProvider(DefaultSecurityManagerProvider.class);
        bind(SystemJobFactory.class).asEagerSingleton();
        bind(AsyncHttpClient.class).asEagerSingleton();
        bind(GracefulShutdown.class).in(Scopes.SINGLETON);

        if (configuration.isMessageCacheOffHeap()) {
            bind(InputCache.class).toProvider(InputCacheProvider.class).asEagerSingleton();
            bind(OutputCache.class).toProvider(OutputCacheProvider.class).asEagerSingleton();
        } else {
            bind(InputCache.class).to(BasicCache.class).in(Scopes.SINGLETON);
            bind(OutputCache.class).to(BasicCache.class).in(Scopes.SINGLETON);
        }
    }

    private void bindInterfaces() {
        bind(MessageGateway.class).to(MessageGatewayImpl.class);
        bind(SecurityContextFactory.class).to(ShiroSecurityContextFactory.class);
        bind(AlertSender.class).to(FormattedEmailAlertSender.class);
        bind(StreamRouter.class);
        bind(FilterService.class).to(FilterServiceImpl.class).in(Scopes.SINGLETON);
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

    private void bindExceptionMappers() {
        TypeLiteral<Class<? extends ExceptionMapper>> type = new TypeLiteral<Class<? extends ExceptionMapper>>(){};
        Multibinder<Class<? extends ExceptionMapper>> setBinder = Multibinder.newSetBinder(binder(), type);
        setBinder.addBinding().toInstance(NotFoundExceptionMapper.class);
        setBinder.addBinding().toInstance(ValidationExceptionMapper.class);
        setBinder.addBinding().toInstance(JsonParseExceptionMapper.class);
        setBinder.addBinding().toInstance(JsonMappingExceptionMapper.class);
    }

    private void bindPluginMetaData() {
        Multibinder<PluginMetaData> setBinder = Multibinder.newSetBinder(binder(), PluginMetaData.class);
    }
}
